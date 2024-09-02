package chav1961.nanohttp.server.serializers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import chav1961.nanohttp.server.interfaces.NanoClassSerializer;
import chav1961.purelib.basic.MimeType;

public class StringClassSerializer implements NanoClassSerializer {
	private static final MimeType[]	MIMES = {MimeType.MIME_HTML_TEXT, MimeType.MIME_PLAIN_TEXT, MimeType.MIME_MARKDOWN_TEXT, MimeType.MIME_CREOLE_TEXT, MimeType.MIME_ANY_TYPE};

	@Override
	public MimeType[] getMimeTypes() {
		return MIMES;
	}

	@Override
	public <T> boolean canServe(final MimeType mime, final Class<T> awaited) {
		if (mime == null) {
			throw new NullPointerException("Mime type can't be null");
		}
		else if (awaited == null) {
			throw new NullPointerException("Awaited class can;t be null");
		}
		else {
			if (isClassSupported(awaited) && mime != null) {
				for(MimeType item : getMimeTypes()) {
					if (mime.containsIn(item)) {
						return true;
					}
				}
				return false;
			}
			else {
				return false;
			}
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
		try(final Writer	wr = new OutputStreamWriter(os)) {
			wr.write(content.toString());
			wr.flush();
		}
	}
	
	private boolean isClassSupported(final Class<?> awaited) {
		return awaited == String.class || awaited.isPrimitive(); 
	}
}
