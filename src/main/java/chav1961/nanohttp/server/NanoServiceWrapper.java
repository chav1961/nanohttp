package chav1961.nanohttp.server;




import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLConnection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import chav1961.nanohttp.server.interfaces.NanoContentEncoder;
import chav1961.nanohttp.server.interfaces.NanoService;
import chav1961.nanohttp.server.parser.AnnotationParser;
import chav1961.purelib.basic.MimeType;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.MimeParseException;
import chav1961.purelib.basic.exceptions.SyntaxException;
import chav1961.purelib.fsys.interfaces.FileSystemInterface;

public class NanoServiceWrapper implements NanoService, Closeable {
	private static final Comparator<DeploymentKeeper<?>>	DEPLOY_SORT = (o1,o2)->{
																int	delta = o1.path.length - o2.path.length;
																
																if (delta != 0) {
																	return delta;
																}
																else {
																	for(int index = 0; index < o1.path.length; index++) {
																		delta = o1.path[index].compareTo(o2.path[index]);
																		if (delta != 0) {
																			return delta;
																		}
																	}
																	return 0;
																}
															};
	private static final String			ACCEPT_ENCODING = "Accept-Encoding";
	private static final String			CONTENT_ENCODING = "Content-Encoding";
	private static final MimeType[]		EMPTY_MIMES = new MimeType[0];
	
	private final HttpServer			server;
	private final boolean				useHttps;
	private final FileSystemInterface	fsi;
	private final List<DeploymentKeeper<?>>			deployed = new CopyOnWriteArrayList<>();
	private final Map<String, NanoContentEncoder>	encoders = new HashMap<>();
	private final boolean				lockExternalQueries;
	private volatile boolean			isStarted = false;	
	private volatile boolean			isSuspended = false;	
	
	NanoServiceWrapper(final NanoServiceBuilder bldr) throws IOException {
		if (bldr.needUseSSL()) {
			final HttpsServer	temp = HttpsServer.create(bldr.getSocketAddress(), 0);
			
			temp.setHttpsConfigurator(new HttpsConfigurator(bldr.buildSSLContext()));
			this.server = temp;
			this.useHttps = true;			
		}
		else {
			this.server = HttpServer.create(bldr.getSocketAddress(), 0);
			this.useHttps = false;
		}
		if (bldr.getExecutorPoolSize() == 0) {
			this.server.setExecutor(Executors.newCachedThreadPool());
		}
		else {
			this.server.setExecutor(Executors.newFixedThreadPool(bldr.getExecutorPoolSize()));
		}
		this.server.createContext("/", (e)->processRequest(e));
		this.fsi = FileSystemInterface.Factory.newInstance(bldr.getRoot());
		this.lockExternalQueries = !bldr.isLocalhostOnly();
		
		for(NanoContentEncoder item : ServiceLoader.<NanoContentEncoder>load(NanoContentEncoder.class)) {
			for(String encoding : item.getEncodings()) {
				encoders.put(encoding, item);
			}
		}
	}

	@Override
	public synchronized void close() throws IOException {
		if (isStarted) {
			stop();
		}
		fsi.close();
	}
	
	@Override
	public synchronized void start() throws IOException {
		if (isStarted) {
			throw new IllegalStateException("Server is already started");
		}
		else {
			isStarted = true;
			server.start();
		}
	}

	@Override
	public synchronized void suspend() throws IOException {
		if (!isStarted) {
			throw new IllegalStateException("Server is not started or was stopped earlier");
		}
		else if (isSuspended) {
			throw new IllegalStateException("Server is already suspended");
		}
		else {
			isSuspended = true;
		}
	}

	@Override
	public void resume() throws IOException {
		if (!isStarted) {
			throw new IllegalStateException("Server is not started or was stopped earlier");
		}
		else if (!isSuspended) {
			throw new IllegalStateException("Server is not suspended yet");
		}
		else {
			isSuspended = false;
		}
	}

	@Override
	public void stop() throws IOException {
		if (!isStarted) {
			throw new IllegalStateException("Server is not started or was stopped earlier");
		}
		else {
			server.stop(0);
			isSuspended = false;
			isStarted = false;
		}
	}

	@Override
	public boolean isStarted() {
		return isStarted;
	}

	@Override
	public boolean isSuspended() {
		return isSuspended;
	}

	@Override
	public void deploy(final String path, final Object instance2deploy) throws IOException, ContentException, SyntaxException {
		if (Utils.checkEmptyOrNullString(path)) {
			throw new IllegalArgumentException("Path to deploy can't be null or empty");
		}
		else if (instance2deploy == null) {
			throw new NullPointerException("Instance to deploy can't be null");
		}
		else if (isDeployed(path)) {
			throw new IllegalStateException("Path to deploy ["+path+"] already has deployed instance. Call undeploy(...) before");
		}
		else if (instance2deploy instanceof FileSystemInterface) {
			getServiceRoot().open(path).mount((FileSystemInterface)instance2deploy);
			synchronized (deployed) {
				deployed.add(new DeploymentKeeper<FileSystemInterface>(path.split("/"), (FileSystemInterface)instance2deploy));
				deployed.sort(DEPLOY_SORT);
			}
		}
		else {
			synchronized (deployed) {
				deployed.add(new DeploymentKeeper<AnnotationParser<?>>(path.split("/"), new AnnotationParser<>(instance2deploy)));
				deployed.sort(DEPLOY_SORT);
			}
		}
	}

	@Override
	public Object undeploy(final String path) throws IOException {
		if (Utils.checkEmptyOrNullString(path)) {
			throw new IllegalArgumentException("Path to deploy can't be null or empty");
		}
		else if (!isDeployed(path)) {
			throw new IllegalStateException("Path to undeploy ["+path+"] had not been deployed yet. Call deploy(...) before");
		}
		else {
			final String[]	content = path.split("/");
			
			synchronized (deployed) {
				for(int index  = deployed.size()-1; index >= 0; index--) {
					if (deployed.get(index).equalsWith(content)) {
						final DeploymentKeeper<?> 	keeper = deployed.remove(index);
						
						if (keeper.content instanceof FileSystemInterface) {
							return getServiceRoot().open(path).unmount();
						}
						else {
							return keeper.content;
						}
					}
				}
			}
			return null;
		}
	}

	@Override
	public FileSystemInterface getServiceRoot() {
		return fsi;
	}

	@Override
	public InetSocketAddress getServerAddress() {
		return server.getAddress();
	}
	
	protected boolean isHttpsUsed() {
		return useHttps;
	}

	private void processRequest(final HttpExchange e) throws IOException {
		if (isSuspended || lockExternalQueries && !e.getRemoteAddress().getHostName().equals(e.getLocalAddress().getHostName())) {
			e.sendResponseHeaders(503, 0);
		}
		else {
			final String	path = e.getRequestURI().getPath();
			final String[]	content = path.split("/");
			final DeploymentKeeper<?>	deployed = getDeployed(content);
			
			if (deployed == null || (deployed.content instanceof FileSystemInterface)) {
				if ("GET".equals(e.getRequestMethod())) {
					try(final FileSystemInterface	fsi = getServiceRoot().clone().open(path)) {
						if (!fsi.exists()) {
							e.sendResponseHeaders(404, 0);
						}
						else {
							final String	name = fsi.getName(); 
							
							try(final InputStream	is = fsi.read();
								final OutputStream	os = e.getResponseBody()) {
								final MimeType[]	inputType = defineMimeByExtension(name);
								
								sendContent(e, name, is, os);
							} catch (Exception exc) {
								e.sendResponseHeaders(500, 0);
							}
						}
					}
				}
				else {
					e.sendResponseHeaders(404, 0);
				}
			}
			else {
				e.sendResponseHeaders(500, 0);
			}
		}
	}

	private boolean isDeployed(final String path) {
		final String[]	content = path.split("/");
		
		for(DeploymentKeeper<?> item : deployed) {
			if (item.equalsWith(content)) {
				return true;
			}
		}
		return false;
	}
	
	private DeploymentKeeper<?> getDeployed(final String[] content) {
		final DeploymentKeeper<?>[]	result = new DeploymentKeeper[1];

		deployed.forEach((val)->{
			if (val.startsWith(content)) {
				result[0] = val;
			}
		});
		return result[0];
	}
	
	private void sendContent(final HttpExchange e, final String name, final InputStream is, final OutputStream os) throws IOException {
		final List<String>	availableEncodings = e.getRequestHeaders().get(ACCEPT_ENCODING);
		final Set<String> 	encodings;
		
		if (availableEncodings == null || (encodings = extractEncoding(availableEncodings)).isEmpty()) {
			e.sendResponseHeaders(200, 0);
			Utils.copyStream(is, os);				
		}
		else {
			for(Entry<String, NanoContentEncoder> item : encoders.entrySet()) {
				if (encodings.contains(item.getKey())) {
					e.getResponseHeaders().add(CONTENT_ENCODING, item.getKey());
					e.sendResponseHeaders(200, 0);
					Utils.copyStream(is, item.getValue().encode(os, item.getKey()));
					return;
				}
			}
			e.sendResponseHeaders(200, 0);
			Utils.copyStream(is, os);				
		}
	}

	private Set<String> extractEncoding(final List<String> readedEncodings) {
		final Set<String>	result = new HashSet<>();
		
		if (readedEncodings != null) {
			for(String item : readedEncodings) {
				for(String piece : item.split(",")) {
					result.add(piece.trim());
				}
			}
		}
		return result;
	}

	private static MimeType[] defineMimeByExtension(final String fileName) {
		try {
			return new MimeType[] {MimeType.valueOf(URLConnection.getFileNameMap().getContentTypeFor(fileName))};
		} catch (MimeParseException | IllegalArgumentException e) {
			return EMPTY_MIMES;
		}		
	}

	
	static class DeploymentKeeper<T> {
		final String[]	path;
		final T			content;
		
		DeploymentKeeper(String[] path, T content) {
			super();
			this.path = path;
			this.content = content;
		}

		boolean startsWith(final String... path) {
			if (path == null) {
				throw new NullPointerException("Path can't be null");
			}
			else if (path.length < this.path.length) {
				return false;
			}
			else {
				final String[]	temp = this.path;
				
				for(int index = 0, maxIndex = temp.length; index < maxIndex; index++) {
					if (!temp[index].equals(path[index])) {
						return false;
					}
				}
				return true;
			}
		}

		boolean equalsWith(final String... path) {
			if (path == null) {
				throw new NullPointerException("Path can't be null");
			}
			else if (path.length != this.path.length) {
				return false;
			}
			else {
				final String[]	temp = this.path;
				
				for(int index = 0, maxIndex = temp.length; index < maxIndex; index++) {
					if (!temp[index].equals(path[index])) {
						return false;
					}
				}
				return true;
			}
		}
	}	
}
