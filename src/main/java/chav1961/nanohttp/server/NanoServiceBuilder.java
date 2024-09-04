package chav1961.nanohttp.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyStore;
import java.util.Map.Entry;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.Utils;

public class NanoServiceBuilder {
	private int		port = 8080;
	private URI		root = URI.create("fsys:file:./root");
	private boolean	localhostOnly = Boolean.valueOf(Constants.DEFAULT_NANOSERVICE_LOCALHOST_ONLY);
	private int		executorPoolSize = Integer.valueOf(Constants.DEFAULT_NANOSERVICE_EXECUTOR_POOL_SIZE);
	private boolean	disableLoopback = Boolean.valueOf(Constants.DEFAULT_NANOSERVICE_DISABLE_LOOPBACK);
	private int		temporaryCacheSize = 0;
	private URI		creolePrologueURI = null;
	private URI		creoleEpilogueURI = null;
	private boolean	useSSL = false;
	private boolean	useSSLKeystore = System.getProperties().containsKey(Constants.SYSTEM_SSL_KEYSTORE) ? true : false;
	private URI		sslKeyStore = System.getProperties().containsKey(Constants.SYSTEM_SSL_KEYSTORE) ? URI.create(System.getProperty(Constants.SYSTEM_SSL_KEYSTORE)) : null;
	private String	sslKeyStoreType = System.getProperties().containsKey(Constants.SYSTEM_SSL_KEYSTORE_TYPE) ? System.getProperty(Constants.SYSTEM_SSL_KEYSTORE_TYPE) : Constants.DEFAULT_SYSTEM_SSL_KEYSTORE_TYPE;
	private char[]	sslKeyStorePassword = System.getProperties().containsKey(Constants.SYSTEM_SSL_KEYSTORE_PASSWD) ? System.getProperty(Constants.SYSTEM_SSL_KEYSTORE_PASSWD).toCharArray() : null;
	private boolean	useTrustKeystore = System.getProperties().containsKey(Constants.SYSTEM_SSL_TRUSTSTORE) ? true : false;
	private URI		sslTrustStore = System.getProperties().containsKey(Constants.SYSTEM_SSL_TRUSTSTORE) ? URI.create(System.getProperty(Constants.SYSTEM_SSL_TRUSTSTORE)) : null;
	private String	sslTrustStoreType = System.getProperties().containsKey(Constants.SYSTEM_SSL_TRUSTSTORE_TYPE) ? System.getProperty(Constants.SYSTEM_SSL_TRUSTSTORE_TYPE) : Constants.DEFAULT_SYSTEM_SSL_TRUSTSTORE_TYPE;
	private char[]	sslTrustStorePassword = System.getProperties().containsKey(Constants.SYSTEM_SSL_TRUSTSTORE_PASSWD) ? System.getProperty(Constants.SYSTEM_SSL_TRUSTSTORE_PASSWD).toCharArray() : null;
	private boolean	turnOnTrace = false;
	
	private NanoServiceBuilder() {
		
	}

	private NanoServiceBuilder(final SubstitutableProperties props, final boolean checkOptionsAreValid) {
		if (props == null) {
			throw new NullPointerException("Properties can't be null");
		}
		else {
			for(Entry<Object, Object> item : props.entrySet()) {
				if (!Constants.NANOSERVICE_AVAILABLE_OPTIONS.contains(item.getKey())) {
					if (checkOptionsAreValid) {
						throw new IllegalArgumentException("Unsupported option ["+item.getKey()+"] in the props");
					}
				}
				else {
					switch ((String)item.getKey()) {
						case Constants.NANOSERVICE_PORT :
							setPort(Integer.valueOf((String)item.getValue()));
							break;
						case Constants.NANOSERVICE_ROOT :
							setRoot(URI.create((String)item.getValue()));
							break;
						case Constants.NANOSERVICE_LOCALHOST_ONLY :
							setLocalhostOnly(Boolean.valueOf((String)item.getValue()));
							break;
						case Constants.NANOSERVICE_EXECUTOR_POOL_SIZE :
							setExecutorPoolSize(Integer.valueOf((String)item.getValue()));
							break;
						case Constants.NANOSERVICE_DISABLE_LOOPBACK :
							setDisableLoopback(Boolean.valueOf((String)item.getValue()));
							break;
						case Constants.NANOSERVICE_TEMPORARY_CACHE_SIZE :
							setTemporaryCacheSize(Integer.valueOf((String)item.getValue()));
							break;
						case Constants.NANOSERVICE_CREOLE_PROLOGUE_URI :
							setCreolePrologueURI(URI.create((String)item.getValue()));
							break;
						case Constants.NANOSERVICE_CREOLE_EPILOGUE_URI :
							setCreoleEpilogueURI(URI.create((String)item.getValue()));
							break;
						case Constants.NANOSERVICE_USE_SSL :
							setUseSSL(Boolean.valueOf((String)item.getValue()));
							break;
						case Constants.NANOSERVICE_USE_KEYSTORE :
							setUseSSLKeystore(Boolean.valueOf((String)item.getValue()));
							break;
						case Constants.NANOSERVICE_SSL_KEYSTORE :
							setSslKeyStore(URI.create((String)item.getValue()));
							break;							
						case Constants.NANOSERVICE_SSL_KEYSTORE_TYPE :
							setSslKeyStoreType((String)item.getValue());
							break;							
						case Constants.NANOSERVICE_SSL_KEYSTORE_PASSWD :
							setSslKeyStorePassword(((String)item.getValue()).toCharArray());
							break;							
						case Constants.NANOSERVICE_USE_TRUSTSTORE :
							setUseTrustKeystore(Boolean.valueOf((String)item.getValue()));
							break;
						case Constants.NANOSERVICE_SSL_TRUSTSTORE :
							setSslTrustStore(URI.create((String)item.getValue()));
							break;							
						case Constants.NANOSERVICE_SSL_TRUSTSTORE_TYPE :
							setSslTrustStoreType((String)item.getValue());
							break;							
						case Constants.NANOSERVICE_SSL_TRUSTSTORE_PASSWD :
							setSslTrustStorePassword(((String)item.getValue()).toCharArray());
							break;							
					}
				}
			}
		}
	}

	public static NanoServiceBuilder newInstance() {
		return new NanoServiceBuilder();
	}
	
	public static NanoServiceBuilder of(final SubstitutableProperties props) {
		return new NanoServiceBuilder(props, true);
	}
	
	public NanoServiceBuilder setPort(final int port) {
		if (port <= 0 || port > 65535) {
			throw new IllegalArgumentException("Port number ["+port+"] out of range 1..65535");
		}
		else {
			this.port = port;
			return this;
		}
	}

	public NanoServiceBuilder setRoot(final URI root) {
		if (root == null) {
			throw new NullPointerException("Root can't be null");
		}
		else {
			this.root = root;
			return this;
		}
	}

	public NanoServiceBuilder setLocalhostOnly(final boolean localhostOnly) {
		this.localhostOnly = localhostOnly;
		return this;
	}

	public NanoServiceBuilder setExecutorPoolSize(final int executorPoolSize) {
		if (executorPoolSize <= 0) {
			throw new IllegalArgumentException("Executor pool size ["+executorPoolSize+"] must be greater than 0");
		}
		else {
			this.executorPoolSize = executorPoolSize;
			return this;
		}
	}

	public NanoServiceBuilder setDisableLoopback(final boolean disableLoopback) {
		this.disableLoopback = disableLoopback;
		return this;
	}

	public NanoServiceBuilder setTemporaryCacheSize(int temporaryCacheSize) {
		if (temporaryCacheSize < 0) {
			throw new IllegalArgumentException("Temporary cache size ["+temporaryCacheSize+"] can't be negative number");
		}
		else {
			this.temporaryCacheSize = temporaryCacheSize;
			return this;
		}
	}

	public NanoServiceBuilder setCreolePrologueURI(final URI creolePrologueURI) {
		this.creolePrologueURI = creolePrologueURI;
		return this;
	}

	public NanoServiceBuilder setCreoleEpilogueURI(final URI creoleEpilogueURI) {
		this.creoleEpilogueURI = creoleEpilogueURI;
		return this;
	}

	public NanoServiceBuilder setUseSSL(final boolean useSSL) {
		this.useSSL = useSSL;
		return this;
	}

	public NanoServiceBuilder setUseSSLKeystore(final boolean useSSLKeystore) {
		this.useSSLKeystore = useSSLKeystore;
		return this;
	}

	public NanoServiceBuilder setSslKeyStore(final URI sslKeyStore) {
		if (sslKeyStore == null) {
			throw new NullPointerException("SSL keystore to set can't be null");
		}
		else {
			this.sslKeyStore = sslKeyStore;
			return this;
		}
	}

	public NanoServiceBuilder setSslKeyStoreType(final String sslKeyStoreType) {
		if (Utils.checkEmptyOrNullString(sslKeyStoreType)) {
			throw new IllegalArgumentException("SSL keystore type to set can't be null or empty");
		}
		else {
			this.sslKeyStoreType = sslKeyStoreType;
			return this;
		}
	}

	public NanoServiceBuilder setSslKeyStorePassword(final char[] sslKeyStorePassword) {
		if (sslKeyStorePassword == null || sslKeyStorePassword.length == 0) {
			throw new IllegalArgumentException("SSL keystore password to set can't be null or empty");
		}
		else {
			this.sslKeyStorePassword = sslKeyStorePassword;
			return this;
		}
	}

	public NanoServiceBuilder setUseTrustKeystore(final boolean useTrustKeystore) {
		this.useTrustKeystore = useTrustKeystore;
		return this;
	}

	public NanoServiceBuilder setSslTrustStore(final URI sslTrustStore) {
		if (sslTrustStore == null) {
			throw new NullPointerException("SSL trust store to set can't be null");
		}
		else {
			this.sslTrustStore = sslTrustStore;
			return this;
		}
	}

	public NanoServiceBuilder setSslTrustStoreType(final String sslTrustStoreType) {
		if (Utils.checkEmptyOrNullString(sslTrustStoreType)) {
			throw new IllegalArgumentException("SSL trust store type to set can't be null or empty");
		}
		else {
			this.sslTrustStoreType = sslTrustStoreType;
			return this;
		}
	}

	public NanoServiceBuilder setSslTrustStorePassword(final char[] sslTrustStorePassword) {
		if (sslTrustStorePassword == null || sslTrustStorePassword.length == 0) {
			throw new IllegalArgumentException("SSL trust store password to set can't be null or empty");
		}
		else {
			this.sslTrustStorePassword = sslTrustStorePassword;
			return this;
		}
	}
	
	public NanoServiceBuilder setTraceOn(final boolean traceOn) {
		this.turnOnTrace = traceOn;
		return this;
	}
	
	public NanoServiceWrapper build() throws IOException {
		if (useSSL) {
			if (useSSLKeystore) {
				if (sslKeyStore == null) {
					throw new IllegalStateException("Mandatory parameter ["+Constants.SYSTEM_SSL_KEYSTORE+"] was not defined");
				}
				if (sslKeyStorePassword == null) {
					throw new IllegalStateException("Mandatory parameter ["+Constants.SYSTEM_SSL_KEYSTORE_PASSWD+"] was not defined");
				}
			}
			if (useTrustKeystore) {
				if (sslTrustStore == null) {
					throw new IllegalStateException("Mandatory parameter ["+Constants.SYSTEM_SSL_TRUSTSTORE+"] was not defined");
				}
				if (sslTrustStorePassword == null) {
					throw new IllegalStateException("Mandatory parameter ["+Constants.SYSTEM_SSL_TRUSTSTORE_PASSWD+"] was not defined");
				}
			}
		}
		return new NanoServiceWrapper(this);
	}
	
	InetSocketAddress getSocketAddress() {
		return new InetSocketAddress(port);
	}

	URI getRoot() {
		return root;
	}

	boolean isLocalhostOnly() {
		return localhostOnly;
	}

	boolean isTraceTurned() {
		return turnOnTrace;
	}
	
	int getExecutorPoolSize() {
		return executorPoolSize;
	}

	boolean isDisableLoopback() {
		return disableLoopback;
	}

	int getTemporaryCacheSize() {
		return temporaryCacheSize;
	}

	URI getCreolePrologueURI() {
		return creolePrologueURI;
	}

	URI getCreoleEpilogueURI() {
		return creoleEpilogueURI;
	}

	boolean needUseSSL() {
		return useSSL;
	}
	
	SSLContext buildSSLContext() throws IOException {
	    try{final SSLContext			sslContext = SSLContext.getInstance(Constants.NANOSERVICE_SSL_TYPE);
			final KeyStore 				ks = KeyStore.getInstance(sslKeyStoreType);
			final KeyManagerFactory		kmf = KeyManagerFactory.getInstance(Constants.NANOSERVICE_KEY_TYPE);
		    final KeyStore 				ts = KeyStore.getInstance(sslTrustStoreType);
		    final TrustManagerFactory	tmf = TrustManagerFactory.getInstance(Constants.NANOSERVICE_KEY_TYPE);
	
		    if (useSSLKeystore) {
		        try(final InputStream 	is = sslKeyStore.toURL().openStream()) {
		        	ks.load(is, sslKeyStorePassword);
		        }
		        kmf.init(ks, sslKeyStorePassword);
		    }
		    if (useTrustKeystore) {
		        try(final InputStream 	is = sslTrustStore.toURL().openStream()) {
		        	ts.load(is, sslTrustStorePassword);
		        }
		        tmf.init(ts);
		    }
		    sslContext.init(useSSLKeystore ? kmf.getKeyManagers() : null
		    			  , useTrustKeystore ? tmf.getTrustManagers() : null
		    			  , null);
		    return sslContext;
		} catch (Exception e) {
			throw new IOException(e);
		}       
	}
}
