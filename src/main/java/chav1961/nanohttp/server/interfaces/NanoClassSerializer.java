package chav1961.nanohttp.server.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import chav1961.purelib.basic.MimeType;

public interface NanoClassSerializer {
	MimeType[] getMimeTypes();
	<T> boolean canServe(MimeType mime, Class<T> awaited);
	<T> T deserialize(InputStream is, Class<T> awaited) throws IOException;
	<T> void serialize(T content, OutputStream os) throws IOException;
}
