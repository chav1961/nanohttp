package chav1961.nanohttp.server.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import chav1961.purelib.basic.MimeType;

// MimeBodyPart
public interface NanoContentSerializer {
	MimeType[] getMimeTypes();
	boolean canServe(MimeType mimeFrom, MimeType mimeTo);
	InputStream deserialize(InputStream is, MimeType mimeFrom, MimeType mimeTo) throws IOException;
	OutputStream serialize(OutputStream os, MimeType mimeFrom, MimeType mimeTo) throws IOException;
}
