package chav1961.nanohttp.internal;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class DeploymentRepo implements Iterable<URLClassLoader>, Closeable {
	private final URLClassLoader	root;
	private final Map<String, URLClassLoader>	children = new HashMap<>();
	
	public DeploymentRepo(final File appDir) throws IOException {
		if (appDir == null || !appDir.exists() || !appDir.isDirectory() || !appDir.canRead()) {
			throw new IllegalArgumentException("Applicaiton dir is null, not exists, not a directory or can't be read for you"); 
		}
		else {
			this.root = new URLClassLoader(new URL[]{appDir.toURI().toURL()});
		}
	}
	
	public URLClassLoader addClassLoader(final File jar) throws IOException {
		if (jar == null || !jar.exists() || !jar.isFile() || !jar.canRead()) {
			throw new IllegalArgumentException("Jar file to add is null, not exists, not a file or can't be read for you"); 
		}
		else if (children.containsKey(jar.getName())) {
			throw new IllegalArgumentException("Duplicate jar name ["+jar.getAbsolutePath()+"] to add. Remove it first."); 
		}
		else {
			final URLClassLoader	result = new URLClassLoader(new URL[]{jar.toURI().toURL()}, root);  
			
			children.put(jar.getName(), result);
			return result;
		}
	}

	public void removeClassLoader(final File jar) throws IOException {
		if (jar == null) {
			throw new NullPointerException("Jar file to remove is null"); 
		}
		else if (!children.containsKey(jar.getName())) {
			throw new IllegalArgumentException("Jar name to remove ["+jar.getAbsolutePath()+"] not found."); 
		}
		else {
			children.remove(jar.getName()).close();
		}
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
}
