module chav1961.nanohttp {
	requires transitive chav1961.purelib;
	requires java.base;
	requires jdk.httpserver;
	requires javax.ws.rs.api;
	requires java.management;
	requires com.google.gson;
	requires java.mail;
	requires jdk.attach;
	requires org.commonmark;
	
	exports chav1961.nanohttp;
	exports chav1961.nanohttp.server;
	exports chav1961.nanohttp.server.exceptions;
	exports chav1961.nanohttp.server.interfaces;

	uses chav1961.nanohttp.server.interfaces.NanoClassSerializer;
	provides chav1961.nanohttp.server.interfaces.NanoClassSerializer with 
			chav1961.nanohttp.server.serializers.StringClassSerializer,
			chav1961.nanohttp.server.serializers.MultipartClassSerializer,
			chav1961.nanohttp.server.serializers.GsonClassSerializer;

	uses chav1961.nanohttp.server.interfaces.NanoContentEncoder;
	provides chav1961.nanohttp.server.interfaces.NanoContentEncoder with 
			chav1961.nanohttp.server.serializers.GZipContentEncoder;

	uses chav1961.nanohttp.server.interfaces.NanoContentSerializer;
	provides chav1961.nanohttp.server.interfaces.NanoContentSerializer with 
			chav1961.nanohttp.server.serializers.TextContentSerializer;
}
