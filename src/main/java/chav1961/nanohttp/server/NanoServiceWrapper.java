package chav1961.nanohttp.server;



import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import chav1961.nanohttp.server.interfaces.NanoService;
import chav1961.nanohttp.server.parser.AnnotationParser;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.SyntaxException;
import chav1961.purelib.fsys.interfaces.FileSystemInterface;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

public class NanoServiceWrapper implements NanoService, Closeable {
	private final HttpServer			server;
	private final boolean				useHttps;
	private final FileSystemInterface	fsi;
	private final Map<String,Object>	deployed = new HashMap<>(); 
	private volatile boolean			isStarted = false;	
	private volatile boolean			isSuspended = false;	
	
	NanoServiceWrapper(final NanoServiceBuilder bldr) throws IOException {
		if (bldr.needUseSSL()) {
			final HttpsServer	temp = HttpsServer.create(bldr.buildSocketAddress(), 0);
			
			temp.setHttpsConfigurator(new HttpsConfigurator(bldr.buildSSLContext()));
			this.server = temp;
			this.useHttps = true;			
		}
		else {
			this.server = HttpServer.create(bldr.buildSocketAddress(), 0);
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
	public void deploy(String path, Object instance2deploy) throws IOException, ContentException, SyntaxException {
		// TODO Auto-generated method stub
		if (Utils.checkEmptyOrNullString(path)) {
			throw new IllegalArgumentException("Path to deploy can't be null or empty");
		}
		else if (instance2deploy == null) {
			throw new NullPointerException("Instance to deploy can't be null");
		}
		else if (deployed.containsKey(path)) {
			throw new IllegalStateException("Path to deploy [] already has deployed instance. Call undeploy() before");
		}
		else if (instance2deploy instanceof FileSystemInterface) {
			getServiceRoot().open(path).mount((FileSystemInterface)instance2deploy);
			deployed.put(path, instance2deploy);
		}
		else {
			deployed.put(path, new AnnotationParser(instance2deploy));
		}
	}

	@Override
	public Object undeploy(String path) throws IOException {
		// TODO Auto-generated method stub
		if (Utils.checkEmptyOrNullString(path)) {
			throw new IllegalArgumentException("Path to deploy can't be null or empty");
		}
		else {
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

	private void processRequest(final HttpExchange e) {
		// TODO Auto-generated method stub
		
	}
}
