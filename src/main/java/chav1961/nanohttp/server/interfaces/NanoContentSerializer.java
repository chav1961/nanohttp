package chav1961.nanohttp.server.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// MimeBodyPart
public interface NanoContentSerializer {
	String[] getMimeTypes();
	boolean canServe(String mimeFrom, String mimeTo);
	InputStream deserialize(InputStream is, String mime) throws IOException;
	OutputStream serialize(OutputStream os, String mime) throws IOException;
}
