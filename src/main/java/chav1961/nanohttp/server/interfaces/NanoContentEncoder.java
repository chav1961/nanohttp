package chav1961.nanohttp.server.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface NanoContentEncoder {
	String[] getEncodings();
	boolean canServe(String encoding);
	InputStream decode(InputStream is, String encoding) throws IOException;
	OutputStream encode(OutputStream os, String encoding) throws IOException;
}
