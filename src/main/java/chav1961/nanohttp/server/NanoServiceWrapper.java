package chav1961.nanohttp.server;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.URLConnection;
import java.util.ArrayList;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import chav1961.nanohttp.internal.InternalUtils;
import chav1961.nanohttp.server.exceptions.RestServiceException;
import chav1961.nanohttp.server.interfaces.NanoContentEncoder;
import chav1961.nanohttp.server.interfaces.NanoContentSerializer;
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
import chav1961.purelib.basic.interfaces.LoggerFacadeOwner;
import chav1961.purelib.concurrent.LightWeightRWLockerWrapper;
import chav1961.purelib.concurrent.LightWeightRWLockerWrapper.Locker;
import chav1961.purelib.fsys.interfaces.DataWrapperInterface;
import chav1961.purelib.fsys.interfaces.FileSystemInterface;

public class NanoServiceWrapper implements NanoService, Closeable, LoggerFacadeOwner {
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
//	private static final String 		HEAD_ACCEPT_CHARSET = "Accept-charset"; 	
//	private static final MimeType[]		EMPTY_MIMES = new MimeType[0];

	private static final String 		HEAD_CONTENT_ENCODING_IDENTITY = "identity";
//	private static final String 		HEAD_CONTENT_ENCODING_GZIP = "gzip";
//	private static final String 		HEAD_CONTENT_ENCODING_COMPRESS = "compress";
	
	private final NanoServiceBuilder 	bldr;
	private final boolean				useHttps;
	private final boolean				turnOnTrace;
	private final FileSystemInterface	fsi;
	private final LightWeightRWLockerWrapper		lock = new LightWeightRWLockerWrapper(); 
	private final List<DeploymentKeeper<?>>			deployed = new ArrayList<>();
	private final Map<String, NanoContentEncoder>	encoders = new HashMap<>();
	private final boolean				lockExternalQueries;
	private final LoggerFacade			logger; 
	private volatile HttpServer			server = null;
	private volatile ExecutorService	executors = null;
	private volatile Object[]			passedParameters = new Object[0];
	private volatile boolean			isStarted = false;	
	private volatile boolean			isSuspended = false;	
	
	NanoServiceWrapper(final NanoServiceBuilder bldr, final LoggerFacade logger) throws IOException {
		this.bldr = bldr;
		this.logger = logger;
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
	public LoggerFacade getLogger() {
		return logger;
	}

	@Override
	public synchronized void close() throws IOException {
		if (isStarted) {
			stop();
		}
		fsi.close();
		if (isTraceTurnedOn()) {
			getLogger().message(Severity.debug, "Server closed");
		}
	}
	
	@Override
	public synchronized void start() throws IOException {
		if (isStarted) {
			throw new IllegalStateException("Server is already started");
		}
		else {
			this.executors = prepareExecutors(bldr);
			this.server = prepareServer(bldr, executors);
			isStarted = true;
			this.server.start();
			if (isTraceTurnedOn()) {
				getLogger().message(Severity.debug, "Server started, port="+bldr.getSocketAddress().getPort());
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
			this.server.stop(5);
			this.executors.shutdownNow();
			try {
				if (isTraceTurnedOn()) {
					logger.message(Severity.debug, "Stopping executors");
				}
				this.executors.awaitTermination(10, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
			}
			this.server = null;
			this.executors = null;
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
			try (final Locker l = lock.lock(false)) {
				deployed.add(new DeploymentKeeper<FileSystemInterface>(InternalUtils.splitRequestPath(path), (FileSystemInterface)instance2deploy));
				deployed.sort(DEPLOY_SORT);
			}
			if (isTraceTurnedOn()) {
				logger.message(Severity.debug, "File system ["+((FileSystemInterface)instance2deploy).getAbsoluteURI()+"] deployed at ["+path+"]");
			}
		}
		else {
			try (final Locker l = lock.lock(false)) {
				final AnnotationParser<?>	ap = new AnnotationParser<>(instance2deploy, path);
				final String				totalPath = ap.getRootPath();				
				
				deployed.add(new DeploymentKeeper<AnnotationParser<?>>(InternalUtils.splitRequestPath(totalPath), ap));
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
			final String[]	content = InternalUtils.splitRequestPath(path);
			
			try (final Locker l = lock.lock(false)) {
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

	@Override
	public void forEachDeployed(final BiConsumer<String, Object> callback) {
		if (callback == null) {
			throw new NullPointerException("Callback can't be null"); 
		}
		else {
			final DeploymentKeeper<AnnotationParser<?>>[]	content;
			
			try (final Locker l = lock.lock()) {
				content = deployed.toArray(new DeploymentKeeper[deployed.size()]);
			}
			for(DeploymentKeeper<AnnotationParser<?>> item : content) {
				callback.accept(String.join("/", item.path), item.content.getInstance());
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

	public boolean isTraceTurnedOn() {
		return turnOnTrace;
	}

	protected boolean isHttpsUsed() {
		return useHttps;
	}
	
	private void processRequest(final HttpExchange e) throws IOException {
		if (isSuspended || lockExternalQueries && !e.getRemoteAddress().getHostName().equals(e.getLocalAddress().getHostName())) {
			sendResponse(e, 503);
		}
		else {
			final String	path = e.getRequestURI().getPath();
			final String[]	content = InternalUtils.splitRequestPath(path);
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
		e.close();
	}

	private boolean isDeployed(final String path) {
		final String[]	content = InternalUtils.splitRequestPath(path);
		
		try (final Locker l = lock.lock()) {
			for(DeploymentKeeper<?> item : deployed) {
				if (item.equalsWith(content)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private DeploymentKeeper<?> getDeployed(final String[] content) {
		final DeploymentKeeper<?>[]	result = new DeploymentKeeper[1];

		try (final Locker l = lock.lock()) {
			deployed.forEach((val)->{
				if (val.startsWith(content)) {
					result[0] = val;
				}
			});
		}
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
		final MimeType[]	possibleType = defineMimeByExtension(fsi.getName());
		final MimeType		sourceMime = possibleType == null || possibleType.length == 0 ? MimeType.MIME_PLAIN_TEXT : possibleType[0]; 
		final List<String>	availableType = e.getRequestHeaders().get(HEAD_ACCEPT);
		final MimeType		targetMime = availableType == null || availableType.isEmpty() ? MimeType.MIME_PLAIN_TEXT : toMimeType(availableType.get(0)); 
		final List<String>	availableEncodings = e.getRequestHeaders().get(HEAD_ACCEPT_ENCODING);
		final Set<String> 	encodings;
		
		if (availableEncodings == null || (encodings = extractEncoding(availableEncodings)).isEmpty()) {
			sendRawContent(e, fsi, sourceMime, os, targetMime, bldr);
		}
		else {
			for(Entry<String, NanoContentEncoder> item : encoders.entrySet()) {
				if (encodings.contains(item.getKey())) {
					printDebug(e, 200);
					e.getResponseHeaders().add(HEAD_CONTENT_TYPE, targetMime.toString());
					e.getResponseHeaders().add(HEAD_CONTENT_ENCODING, item.getKey());
					e.sendResponseHeaders(200, 0);
					
					try(final InputStream	is = fsi.read();
						final OutputStream	decode = item.getValue().encode(os, item.getKey())) {
						serializeRawContent(is, sourceMime, decode, targetMime, bldr);
					}
					return;
				}
			}
			sendRawContent(e, fsi, sourceMime, os, targetMime, bldr);
		}
	}

	private void sendRawContent(final HttpExchange e, final FileSystemInterface fsi, final MimeType sourceMime, final OutputStream os, final MimeType targetMime, final NanoServiceBuilder props) throws IOException {
		final long	length = (long) fsi.getAttributes().get(DataWrapperInterface.ATTR_SIZE);
		
		printDebug(e, 200);
		e.getResponseHeaders().add(HEAD_CONTENT_TYPE, targetMime.toString());
		e.getResponseHeaders().add(HEAD_CONTENT_LENGTH, String.valueOf(length));
		e.getResponseHeaders().add(HEAD_CONTENT_ENCODING, HEAD_CONTENT_ENCODING_IDENTITY);
		e.sendResponseHeaders(200, length);
		try(final InputStream	is = fsi.read()) {
			serializeRawContent(is, sourceMime, os, targetMime, props);
		}
	}	

	private void serializeRawContent(final InputStream is, final MimeType sourceMime, final OutputStream os, final MimeType targetMime, final NanoServiceBuilder props) throws IOException {
		if (targetMime.containerOf(sourceMime)) {
			Utils.copyStream(is, os);				
		}
		else {
			for (NanoContentSerializer ser : ServiceLoader.load(NanoContentSerializer.class)) {
				if (ser.canServe(sourceMime, targetMime)) {
					if (ser.isOutputStreamSupported()) {
						Utils.copyStream(is, ser.serialize(os, sourceMime, targetMime, props));
						return;
					}
					else if (ser.isWriterSupported()) {
						final Reader	rdr = new InputStreamReader(is);
						final Writer	wr = new OutputStreamWriter(os);
						
						Utils.copyStream(rdr, ser.serialize(wr, sourceMime, targetMime, props));
						return;
					}
				}
			}
			Utils.copyStream(is, os);				
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
			return new MimeType[] {toMimeType(URLConnection.getFileNameMap().getContentTypeFor(fileName))};
		}
	}
	
	private static MimeType toMimeType(final String mime) {
		try {
			return MimeType.valueOf(mime);
		} catch (MimeParseException | IllegalArgumentException e) {
			return MimeType.MIME_PLAIN_TEXT;
		}		
	}

	private ExecutorService prepareExecutors(final NanoServiceBuilder bldr) throws IOException {
		if (bldr.getExecutorPoolSize() == 0) {
			return Executors.newCachedThreadPool((r)->createThread(r));
		}
		else {
			return Executors.newFixedThreadPool(bldr.getExecutorPoolSize(), (r)->createThread(r));
		}
	}	
	
	private static Thread createThread(final Runnable r) {
		final Thread	t = new Thread(r);
		
		t.setName("HTTP request processor");
		t.setDaemon(true);
		return t;
	}

	private HttpServer prepareServer(final NanoServiceBuilder bldr, final ExecutorService service) throws IOException {
		final HttpServer	result;
		
		if (bldr.needUseSSL()) {
			final HttpsServer	temp = HttpsServer.create(bldr.getSocketAddress(), 0);
			
			temp.setHttpsConfigurator(new HttpsConfigurator(bldr.buildSSLContext()));
			result = temp;
		}
		else {
			result = HttpServer.create(bldr.getSocketAddress(), 0);
		}
		result.setExecutor(service);
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

		@Override
		public String toString() {
			return "DeploymentKeeper [path=" + String.join("/", path) + ", class=" + ((content instanceof AnnotationParser) ? ((AnnotationParser)content).getInstance().getClass().getName() : content.getClass().getName()) + "]";
		}
	}	
}
