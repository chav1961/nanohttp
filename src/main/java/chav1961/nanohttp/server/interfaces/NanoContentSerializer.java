package chav1961.nanohttp.server.interfaces;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import chav1961.nanohttp.server.NanoServiceBuilder;
import chav1961.purelib.basic.MimeType;

public interface NanoContentSerializer {
	MimeType[] getMimeTypes();
	boolean canServe(MimeType mimeFrom, MimeType mimeTo);
	boolean isWriterSupported();
	boolean isOutputStreamSupported();
	OutputStream serialize(OutputStream os, MimeType mimeFrom, MimeType mimeTo, NanoServiceBuilder props) throws IOException;
	Writer serialize(Writer os, MimeType mimeFrom, MimeType mimeTo, NanoServiceBuilder props) throws IOException;
}
