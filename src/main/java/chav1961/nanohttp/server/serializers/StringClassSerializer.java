package chav1961.nanohttp.server.serializers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import chav1961.nanohttp.server.interfaces.NanoClassSerializer;
import chav1961.purelib.basic.URIUtils;

public class StringClassSerializer implements NanoClassSerializer {
	private static final String[]	MIMES = {"text/plain", "text/html"};

	@Override
	public String[] getMimeTypes() {
		return MIMES;
	}

	@Override
	public <T> boolean canServe(final String mime, final Class<T> awaited) {
		if (awaited == String.class && mime != null) {
			for(String item : getMimeTypes()) {
				if (mime.startsWith(item)) {
					return true;
				}
			}
			return false;
		}
		else {
			return false;
		}
	}

	@Override
	public <T> T deserialize(final InputStream is, final Class<T> awaited) throws IOException {
		final StringBuilder	sb = new StringBuilder();
		final char[]		buffer = new char[8192];
		final Reader 		rdr = new InputStreamReader(is);
		int	len;
			
		while ((len = rdr.read(buffer)) > 0) {
			sb.append(buffer, 0, len);
		}
		return awaited.cast(sb.toString());
	}

	@Override
	public <T> void serialize(final T content, final OutputStream os) throws IOException {
		final Writer	wr = new OutputStreamWriter(os);
		
		wr.write(content.toString());
		wr.flush();
	}
}
