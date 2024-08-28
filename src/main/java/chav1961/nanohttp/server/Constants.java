package chav1961.nanohttp.server;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Constants {
	public static final String 			NANOSERVICE_PORT = "nanoservicePort";
	public static final String 			NANOSERVICE_ROOT = "nanoserviceRoot";
	public static final String 			NANOSERVICE_LOCALHOST_ONLY = "nanoserviceLocalhostOnly";
	public static final String 			DEFAULT_NANOSERVICE_LOCALHOST_ONLY = "true";
	public static final String 			NANOSERVICE_EXECUTOR_POOL_SIZE = "nanoserviceExecutorPoolSize";
	public static final String 			DEFAULT_NANOSERVICE_EXECUTOR_POOL_SIZE = "10";
	public static final String 			NANOSERVICE_DISABLE_LOOPBACK = "nanoserviceDisableLoopback";
	public static final String 			DEFAULT_NANOSERVICE_DISABLE_LOOPBACK = "true";
	public static final String 			NANOSERVICE_TEMPORARY_CACHE_SIZE = "nanoserviceTemporaryCacheSize";
	public static final String 			NANOSERVICE_CREOLE_PROLOGUE_URI = "nanoserviceCreolePrologueURI";
	public static final String 			NANOSERVICE_CREOLE_EPILOGUE_URI = "nanoserviceCreoleEpilogueURI";
	public static final String 			NANOSERVICE_USE_SSL = "nanoserviceUseSSL";
	public static final String 			NANOSERVICE_USE_KEYSTORE = "nanoserviceUseKeyStore";
	public static final String 			NANOSERVICE_SSL_KEYSTORE = "nanoserviceSSLKeyStore";
	public static final String 			NANOSERVICE_SSL_KEYSTORE_TYPE = "nanoserviceSSLKeyStoreType";
	public static final String 			NANOSERVICE_SSL_KEYSTORE_PASSWD = "nanoserviceSSLKeyStorePasswd";
	public static final String 			NANOSERVICE_USE_TRUSTSTORE = "nanoserviceUseTrustStore";
	public static final String 			NANOSERVICE_SSL_TRUSTSTORE = "nanoserviceSSLTrustStore";
	public static final String 			NANOSERVICE_SSL_TRUSTSTORE_TYPE = "nanoserviceSSLTrustStoreType";
	public static final String 			NANOSERVICE_SSL_TRUSTSTORE_PASSWD = "nanoserviceSSLTrustStorePasswd";
	public static final String 			NANOSERVICE_SSL_TYPE = "TLS";
	public static final String 			NANOSERVICE_KEY_TYPE = "SunX509";
	
	public static final Set<String>		NANOSERVICE_AVAILABLE_OPTIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
											NANOSERVICE_PORT,
											NANOSERVICE_ROOT,
											NANOSERVICE_LOCALHOST_ONLY,
											NANOSERVICE_EXECUTOR_POOL_SIZE,
											NANOSERVICE_DISABLE_LOOPBACK,
											NANOSERVICE_TEMPORARY_CACHE_SIZE,
											NANOSERVICE_CREOLE_PROLOGUE_URI,
											NANOSERVICE_CREOLE_EPILOGUE_URI,
											NANOSERVICE_USE_SSL,
											NANOSERVICE_USE_KEYSTORE,
											NANOSERVICE_SSL_KEYSTORE,
											NANOSERVICE_SSL_KEYSTORE_TYPE,
											NANOSERVICE_SSL_KEYSTORE_PASSWD,
											NANOSERVICE_USE_TRUSTSTORE,
											NANOSERVICE_SSL_TRUSTSTORE,
											NANOSERVICE_SSL_TRUSTSTORE_TYPE,
											NANOSERVICE_SSL_TRUSTSTORE_PASSWD
											)
										));
	
	public static final String 			SYSTEM_SSL_KEYSTORE = "javax.net.ssl.keyStore";
	public static final String 			SYSTEM_SSL_KEYSTORE_TYPE = "javax.net.ssl.keyStoreType";
	public static final String 			DEFAULT_SYSTEM_SSL_KEYSTORE_TYPE = "JKS";
	public static final String 			SYSTEM_SSL_KEYSTORE_PASSWD = "javax.net.ssl.keyStorePassword";
	public static final String 			SYSTEM_SSL_TRUSTSTORE = "javax.net.ssl.trustStore";
	public static final String 			SYSTEM_SSL_TRUSTSTORE_TYPE = "javax.net.ssl.trustStoreType";
	public static final String 			DEFAULT_SYSTEM_SSL_TRUSTSTORE_TYPE = "JKS";
	public static final String 			SYSTEM_SSL_TRUSTSTORE_PASSWD = "javax.net.ssl.trustStorePassword";
}
