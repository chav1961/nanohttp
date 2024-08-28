package chav1961.nanohttp.server.https;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import chav1961.nanohttp.server.Constants;
import chav1961.purelib.basic.SubstitutableProperties;

public class SSLUtils {
	public static SSLContext createSSLContext(final SubstitutableProperties props) throws IOException, NullPointerException  {
		if (props == null) {
			throw new NullPointerException("Properties can't be null");
		}
		else {
		    try{final SSLContext			sslContext = SSLContext.getInstance(Constants.NANOSERVICE_SSL_TYPE);
	        	final KeyManagerFactory		kmf = KeyManagerFactory.getInstance(Constants.NANOSERVICE_KEY_TYPE);
		        final TrustManagerFactory	tmf = TrustManagerFactory.getInstance(Constants.NANOSERVICE_KEY_TYPE);
		    	final KeyStore 				ks = KeyStore.getInstance(props.getProperty(Constants.NANOSERVICE_SSL_KEYSTORE_TYPE, String.class
		    									, System.getProperty(Constants.SYSTEM_SSL_KEYSTORE_TYPE, Constants.DEFAULT_SYSTEM_SSL_KEYSTORE_TYPE)));
			    final KeyStore 				ts = KeyStore.getInstance(props.getProperty(Constants.NANOSERVICE_SSL_TRUSTSTORE_TYPE, String.class
												, System.getProperty(Constants.SYSTEM_SSL_TRUSTSTORE_TYPE, Constants.DEFAULT_SYSTEM_SSL_TRUSTSTORE_TYPE)));
	
			    if (props.getProperty(Constants.NANOSERVICE_USE_KEYSTORE,boolean.class,"false")) {
			        try(final InputStream 	is = props.getProperty(Constants.NANOSERVICE_SSL_KEYSTORE, InputStream.class, System.getProperty(Constants.SYSTEM_SSL_KEYSTORE))) {
			        	ks.load(is,props.getProperty(Constants.NANOSERVICE_SSL_KEYSTORE_PASSWD, char[].class, System.getProperty(Constants.SYSTEM_SSL_KEYSTORE_PASSWD)));
			        }
			        kmf.init(ks,props.getProperty(Constants.NANOSERVICE_SSL_KEYSTORE_PASSWD, char[].class, System.getProperty(Constants.SYSTEM_SSL_KEYSTORE_PASSWD)));
			    }
			    if (props.getProperty(Constants.NANOSERVICE_USE_TRUSTSTORE,boolean.class,"false")) {
			        try(final InputStream 	is = props.getProperty(Constants.NANOSERVICE_SSL_TRUSTSTORE, InputStream.class, System.getProperty(Constants.SYSTEM_SSL_TRUSTSTORE))) {
			        	ts.load(is,props.getProperty(Constants.NANOSERVICE_SSL_TRUSTSTORE_PASSWD, char[].class, System.getProperty(Constants.SYSTEM_SSL_TRUSTSTORE_PASSWD)));
			        }
			        tmf.init(ts);
			    }
		        sslContext.init(props.getProperty(Constants.NANOSERVICE_USE_KEYSTORE,boolean.class,"false") ? kmf.getKeyManagers() : null
		        			  , props.getProperty(Constants.NANOSERVICE_USE_TRUSTSTORE,boolean.class,"false") ? tmf.getTrustManagers() : null
		        			  , null);
		        return sslContext;
		    } catch (Exception e) {
		    	throw new IOException(e);
		    }       
		}
	}
}
