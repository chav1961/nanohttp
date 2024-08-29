package chav1961.nanohttp.server.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface NanoClassSerializer {
	String[] getMimeTypes();
	<T> boolean canServe(String mime, Class<T> awaited);
	<T> T deserialize(InputStream is, Class<T> awaited) throws IOException;
	<T> void serialize(T content, OutputStream os) throws IOException;
}
