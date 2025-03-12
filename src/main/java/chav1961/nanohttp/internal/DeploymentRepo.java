package chav1961.nanohttp.internal;


import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.LoggerFacade.Severity;
import chav1961.purelib.basic.interfaces.LoggerFacadeOwner;

public class DeploymentRepo implements Iterable<URLClassLoader>, Closeable, LoggerFacadeOwner {
	private final URLClassLoader	root;
	private final File				tempDir;
	private final LoggerFacade		logger;
	private final boolean			isDebugTraceRequired;
	private final Map<String, URLClassLoader>	children = new HashMap<>();
	
	public DeploymentRepo(final File appDir, final File tempDir, final LoggerFacade logger, final boolean isDebugTraceRequired) throws IOException {
		if (appDir == null || !appDir.exists() || !appDir.isDirectory() || !appDir.canRead()) {
			throw new IllegalArgumentException("Application dir is null, not exists, not a directory or can't be read for you"); 
		}
		else if (tempDir == null || !tempDir.exists() || !tempDir.isDirectory() || !tempDir.canRead() || !tempDir.canWrite()) {
			throw new IllegalArgumentException("Temp dir is null, not exists, not a directory or can't be read/write for you"); 
		}
		else if (logger == null) {
			throw new NullPointerException("Logger can't be null");
		}
		else {
			this.root = new URLClassLoader(new URL[]{appDir.toURI().toURL()});
			this.tempDir = tempDir;
			this.logger = logger;
			this.isDebugTraceRequired = isDebugTraceRequired;					
		}
	}
	
	@Override
	public LoggerFacade getLogger() {
		return logger;
	}
	
	public URLClassLoader addClassLoader(final File jar) throws IOException {
		if (jar == null || !jar.exists() || !jar.isFile() || !jar.canRead()) {
			throw new IllegalArgumentException("Jar file to add is null, not exists, not a file or can't be read for you"); 
		}
		else if (children.containsKey(jar.getName())) {
			throw new IllegalArgumentException("Duplicate jar name ["+jar.getAbsolutePath()+"] to add. Remove it first."); 
		}
		else {
			final File	copy = copyFile(jar);
			final URLClassLoader	result = new URLClassLoader(new URL[]{copy.toURI().toURL()}, root);  
			
			children.put(jar.getName(), result);
			if (isDebugTraceRequired()) {
				logger.message(Severity.debug, "Class loader for ["+jar.getName()+"] created and added to repository");
			}
			return result;
		}
	}

	private File copyFile(final File jar) throws IOException {
		final File	target = new File(tempDir, jar.getName());
		
		try(final InputStream	from = new FileInputStream(jar);
			final OutputStream	to = new FileOutputStream(target)) {
			Utils.copyStream(from, to);
		}
		return target;
	}

	public void removeClassLoader(final File jar) throws IOException {
		if (jar == null) {
			throw new NullPointerException("Jar file to remove is null"); 
		}
		else if (!children.containsKey(jar.getName())) {
			throw new IllegalArgumentException("Jar name to remove ["+jar.getAbsolutePath()+"] not found."); 
		}
		else {
			final URLClassLoader temp = children.remove(jar.getName()); 
			
			if (isDebugTraceRequired()) {
				logger.message(Severity.debug, "Class loader for ["+jar.getName()+"] removed from repository");
			}
			System.gc();
			final	Thread t = new Thread(()->{
						try {
							temp.close();
						} catch (IOException e) {
						} finally {
							removeFile(jar);
							if (isDebugTraceRequired()) {
								logger.message(Severity.debug, "Class loader for ["+jar.getName()+"] closed");
							}
						}
					});
			
			t.setName("Remove class loader");
			t.setDaemon(true);
			t.start();
		}
	}
	
	private void removeFile(File jar) {
		new File(tempDir, jar.getName()).delete();
	}

	public URLClassLoader getClassLoader(final File jar) {
		if (jar == null) {
			throw new NullPointerException("Jar file to remove is null"); 
		}
		else {
			return children.get(jar.getName());
		}
	}

	@Override
	public Iterator<URLClassLoader> iterator() {
		final List<URLClassLoader>	result = new ArrayList<>();
		
		for(Entry<String, URLClassLoader> item : children.entrySet()) {
			result.add(item.getValue());
		}
		return result.iterator();
	}

	@Override
	public void close() throws IOException {
		for(Entry<String, URLClassLoader> item : children.entrySet()) {
			item.getValue().close();
		}
		root.close();
	}
	
	protected boolean isDebugTraceRequired() {
		return isDebugTraceRequired; 
	}
}
