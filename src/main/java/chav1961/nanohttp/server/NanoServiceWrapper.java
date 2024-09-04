package chav1961.nanohttp.server;




import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLConnection;
import java.util.Arrays;
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
import java.util.function.BiConsumer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import chav1961.nanohttp.server.exceptions.RestServiceException;
import chav1961.nanohttp.server.interfaces.NanoContentEncoder;
import chav1961.nanohttp.server.interfaces.NanoService;
import chav1961.nanohttp.server.parser.AnnotationParser;
import chav1961.purelib.basic.MimeType;
import chav1961.purelib.basic.SystemErrLoggerFacade;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.MimeParseException;
import chav1961.purelib.basic.exceptions.SyntaxException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.LoggerFacade.Severity;
import chav1961.purelib.fsys.interfaces.DataWrapperInterface;
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
	private static final String			HEAD_ACCEPT_ENCODING = "Accept-Encoding";
	private static final String			HEAD_CONTENT_ENCODING = "Content-Encoding";
	private static final String 		HEAD_CONTENT_LENGTH = "Content-Length";
	private static final String 		HEAD_CONTENT_TYPE = "Content-type";
	private static final String 		HEAD_ACCEPT = "Accept"; 	
	private static final String 		HEAD_ACCEPT_CHARSET = "Accept-charset"; 	
	private static final MimeType[]		EMPTY_MIMES = new MimeType[0];

	private static final String 		HEAD_CONTENT_ENCODING_IDENTITY = "identity";
	private static final String 		HEAD_CONTENT_ENCODING_GZIP = "gzip";
	private static final String 		HEAD_CONTENT_ENCODING_COMPRESS = "compress";
	
	private final NanoServiceBuilder 	bldr;
	private final boolean				useHttps;
	private final boolean				turnOnTrace;
	private final FileSystemInterface	fsi;
	private final List<DeploymentKeeper<?>>			deployed = new CopyOnWriteArrayList<>();
	private final Map<String, NanoContentEncoder>	encoders = new HashMap<>();
	private final boolean				lockExternalQueries;
	private volatile HttpServer			server = null;
	private final LoggerFacade			logger = new SystemErrLoggerFacade(); 
	private volatile Object[]			passedParameters = new Object[0];
	private volatile boolean			isStarted = false;	
	private volatile boolean			isSuspended = false;	
	
	NanoServiceWrapper(final NanoServiceBuilder bldr) throws IOException {
		this.bldr = bldr;
		this.useHttps = bldr.needUseSSL();
		this.fsi = FileSystemInterface.Factory.newInstance(bldr.getRoot());
		this.lockExternalQueries = !bldr.isLocalhostOnly();
		this.turnOnTrace = bldr.isTraceTurned();
		
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
		if (isTraceTurnedOn()) {
			logger.message(Severity.debug, "Server closed");
		}
	}
	
	@Override
	public synchronized void start() throws IOException {
		if (isStarted) {
			throw new IllegalStateException("Server is already started");
		}
		else {
			this.server = prepareServer(bldr);
			
			isStarted = true;
			this.server.start();
			if (isTraceTurnedOn()) {
				logger.message(Severity.debug, "Server started, port="+bldr.getSocketAddress().getPort());
			}
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
			if (isTraceTurnedOn()) {
				logger.message(Severity.debug, "Server suspended");
			}
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
			if (isTraceTurnedOn()) {
				logger.message(Severity.debug, "Server resumed");
			}
		}
	}

	@Override
	public void stop() throws IOException {
		if (!isStarted) {
			throw new IllegalStateException("Server is not started or was stopped earlier");
		}
		else {
			this.server.stop(0);
			this.server = null;
			isSuspended = false;
			isStarted = false;
			if (isTraceTurnedOn()) {
				logger.message(Severity.debug, "Server stopped");
			}
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
			if (isTraceTurnedOn()) {
				logger.message(Severity.debug, "File system ["+((FileSystemInterface)instance2deploy).getAbsoluteURI()+"] deployed at ["+path+"]");
			}
		}
		else {
			synchronized (deployed) {
				final AnnotationParser<?>	ap = new AnnotationParser<>(instance2deploy, path);
				final String				totalPath = ap.getRootPath();				
				
				deployed.add(new DeploymentKeeper<AnnotationParser<?>>(totalPath.split("/"), ap));
				deployed.sort(DEPLOY_SORT);
			}
			if (isTraceTurnedOn()) {
				logger.message(Severity.debug, "Servlet class ["+instance2deploy.getClass().getCanonicalName()+"] deployed at ["+path+"]");
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
							if (isTraceTurnedOn()) {
								logger.message(Severity.debug, "File system undeployed from ["+path+"]");
							}
							return getServiceRoot().open(path).unmount();
						}
						else {
							if (isTraceTurnedOn()) {
								logger.message(Severity.debug, "Servlet class undeployed from ["+path+"]");
							}
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

	public void forEachDeployed(final BiConsumer<String, Object> callback) {
		if (callback == null) {
			throw new NullPointerException("Callback can't be null"); 
		}
		else {
			final DeploymentKeeper<?>[]	content;
			
			synchronized (deployed) {
				content = deployed.toArray(new DeploymentKeeper<?>[deployed.size()]);
			}
			for(DeploymentKeeper<?> item : content) {
				callback.accept(String.join("/", item.path), item.content);
			}
		}
	}
	
	public Object[] getPassedParameters() {
		return passedParameters;
	}
	
	public void setPassedParameters(final Object... parameters) {
		if (parameters == null) {
			throw new NullPointerException("Parameters to set can't be null");
		}
		else {
			this.passedParameters = parameters;
		}
	}
	
	protected boolean isHttpsUsed() {
		return useHttps;
	}
	
	protected boolean isTraceTurnedOn() {
		return turnOnTrace;
	}

	private void processRequest(final HttpExchange e) throws IOException {
		if (isSuspended || lockExternalQueries && !e.getRemoteAddress().getHostName().equals(e.getLocalAddress().getHostName())) {
			sendResponse(e, 503);
		}
		else {
			final String	path = e.getRequestURI().getPath();
			final String[]	content = path.split("/");
			final DeploymentKeeper<?>	deployed = getDeployed(content);
			
			if (deployed == null || (deployed.content instanceof FileSystemInterface)) {
				if ("GET".equals(e.getRequestMethod())) {
					try(final FileSystemInterface	fsi = getServiceRoot().clone().open(path)) {
						if (!fsi.exists()) {
							sendResponse(e, 404);
						}
						else {
							try(final OutputStream	os = e.getResponseBody()) {
								sendContent(e, fsi, os);
							} catch (Exception exc) {
								sendResponse(e, 500);
							}
						}
					}
				}
				else {
					sendResponse(e, 404);
				}
			}
 			else if (deployed.content instanceof AnnotationParser<?>) {
				try {
	 				((AnnotationParser<?>)deployed.content).processRequest(e, passedParameters);
				} catch (RestServiceException exc) {
					sendResponse(e, exc.getResponseCode());
				} catch (Exception exc) {
					sendResponse(e, 500);
				}
 			}
 			else {	
				sendResponse(e, 500);
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
	
	private void sendResponse(final HttpExchange e, final int rc) throws IOException {
		printDebug(e, rc);
		e.sendResponseHeaders(rc, 0);
		try(final OutputStream	os = e.getResponseBody()) {
			os.flush();
		}
	}

	private void sendContent(final HttpExchange e, final FileSystemInterface fsi, final OutputStream os) throws IOException {
		final List<String>	availableEncodings = e.getRequestHeaders().get(HEAD_ACCEPT_ENCODING);
		final MimeType[]	possibleType = defineMimeByExtension(fsi.getName());
		final MimeType		selectedMime = possibleType == null || possibleType.length == 0 ? MimeType.MIME_PLAIN_TEXT : possibleType[0]; 
		final Set<String> 	encodings;
		
		if (availableEncodings == null || (encodings = extractEncoding(availableEncodings)).isEmpty()) {
			sendRawContent(e, fsi, selectedMime, os);
		}
		else {
			for(Entry<String, NanoContentEncoder> item : encoders.entrySet()) {
				if (encodings.contains(item.getKey())) {
					printDebug(e, 200);
					e.getResponseHeaders().add(HEAD_CONTENT_TYPE, selectedMime.toString());
					e.getResponseHeaders().add(HEAD_CONTENT_ENCODING, item.getKey());
					e.sendResponseHeaders(200, 0);
					
					try(final InputStream	is = fsi.read();
						final OutputStream	decode = item.getValue().encode(os, item.getKey())) {
						
						Utils.copyStream(is, decode);				
					}
					return;
				}
			}
			sendRawContent(e, fsi, selectedMime, os);
		}
	}

	private void sendRawContent(final HttpExchange e, final FileSystemInterface fsi, final MimeType contentType, final OutputStream os) throws IOException {
		final long	length = (long) fsi.getAttributes().get(DataWrapperInterface.ATTR_SIZE);
		
		printDebug(e, 200);
		e.getResponseHeaders().add(HEAD_CONTENT_TYPE, contentType.toString());
		e.getResponseHeaders().add(HEAD_CONTENT_LENGTH, String.valueOf(length));
		e.getResponseHeaders().add(HEAD_CONTENT_ENCODING, HEAD_CONTENT_ENCODING_IDENTITY);
		e.sendResponseHeaders(200, length);
		try(final InputStream	is = fsi.read()) {
			Utils.copyStream(is, os);				
		}
	}	
	
	private void sendContent(final HttpExchange e, final String name, final InputStream is, final OutputStream os) throws IOException {
		final List<String>	availableEncodings = e.getRequestHeaders().get(HEAD_ACCEPT_ENCODING);
		final Set<String> 	encodings;
		
//		handler.getResponseHeaders().add(HEAD_CONTENT_TYPE, MimeType.MIME_PLAIN_TEXT.toString());
//		handler.getResponseHeaders().add(HEAD_CONTENT_LENGTH, String.valueOf(answer.length));
//		handler.getResponseHeaders().add(HEAD_CONTENT_ENCODING, HEAD_CONTENT_ENCODING_IDENTITY);
//		
		
		if (availableEncodings == null || (encodings = extractEncoding(availableEncodings)).isEmpty()) {
			printDebug(e, 200);
			e.sendResponseHeaders(200, 0);
			System.err.println("len1="+Utils.copyStream(is, os));				
		}
		else {
			for(Entry<String, NanoContentEncoder> item : encoders.entrySet()) {
				if (encodings.contains(item.getKey())) {
					printDebug(e, 200);
					e.getResponseHeaders().add(HEAD_CONTENT_ENCODING, item.getKey());
					e.sendResponseHeaders(200, 0);
					System.err.println("len2="+Utils.copyStream(is, item.getValue().encode(os, item.getKey())));
					return;
				}
			}
			printDebug(e, 200);
			e.sendResponseHeaders(200, 0);
			System.err.println("len3="+Utils.copyStream(is, os));				
		}
	}

	private void printDebug(final HttpExchange e, final int rc) throws IOException {
		if (isTraceTurnedOn()) {
			logger.message(Severity.debug, "Method: "+e.getRequestMethod()+", path="+e.getRequestURI()+" returned "+rc);
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
		if (fileName.endsWith(".cre")) {
			return new MimeType[] {MimeType.MIME_CREOLE_TEXT};
		}
		else {
			try {
				return new MimeType[] {MimeType.valueOf(URLConnection.getFileNameMap().getContentTypeFor(fileName))};
			} catch (MimeParseException | IllegalArgumentException e) {
				return EMPTY_MIMES;
			}		
		}
	}

	private HttpServer prepareServer(final NanoServiceBuilder bldr) throws IOException {
		final HttpServer	result;
		
		if (bldr.needUseSSL()) {
			final HttpsServer	temp = HttpsServer.create(bldr.getSocketAddress(), 0);
			
			temp.setHttpsConfigurator(new HttpsConfigurator(bldr.buildSSLContext()));
			result = temp;
		}
		else {
			result = HttpServer.create(bldr.getSocketAddress(), 0);
		}
		if (bldr.getExecutorPoolSize() == 0) {
			result.setExecutor(Executors.newCachedThreadPool());
		}
		else {
			result.setExecutor(Executors.newFixedThreadPool(bldr.getExecutorPoolSize()));
		}
		result.createContext("/", (e)->processRequest(e));
		return result;
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
