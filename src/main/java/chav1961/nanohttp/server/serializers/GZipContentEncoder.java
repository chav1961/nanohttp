package chav1961.nanohttp.server.serializers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import chav1961.nanohttp.server.interfaces.NanoContentEncoder;

public class GZipContentEncoder implements NanoContentEncoder {
	private static final String[]	ENCODINGS = {"gzip"};

	@Override
	public String[] getEncodings() {
		return ENCODINGS;
	}

	@Override
	public boolean canServe(final String encoding) {
		return ENCODINGS[0].equals(encoding);
	}

	@Override
	public InputStream decode(final InputStream is, final String encoding) throws IOException {
		if (!canServe(encoding)) {
			throw new IllegalArgumentException("Unsupported encoding ["+encoding+"] was passed");
		}
		else {
			return new GZIPInputStream(is);
		}
	}

	@Override
	public OutputStream encode(final OutputStream os, final String encoding) throws IOException {
		if (!canServe(encoding)) {
			throw new IllegalArgumentException("Unsupported encoding ["+encoding+"] was passed");
		}
		else {
			return new GZIPOutputStream(os);
		}
	}
}
