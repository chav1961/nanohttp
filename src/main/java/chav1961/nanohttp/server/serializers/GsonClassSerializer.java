package chav1961.nanohttp.server.serializers;



import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import com.google.gson.Gson;

import chav1961.nanohttp.server.interfaces.NanoClassSerializer;
import chav1961.purelib.basic.MimeType;

public class GsonClassSerializer implements NanoClassSerializer {
	private static final MimeType[]	MIMES = {MimeType.MIME_JSON_TEXT};

	private final Gson	gson = new Gson();
	
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
			return MimeType.MIME_JSON_TEXT.equals(mime);
		}
	}

	@Override
	public <T> T deserialize(final InputStream is, final Class<T> awaited) throws IOException {
		final Reader	rdr = new InputStreamReader(is, "UTF-8");
		
		return gson.fromJson(rdr, awaited);
	}

	@Override
	public <T> void serialize(final T content, final OutputStream os) throws IOException {
		final Writer	wr = new OutputStreamWriter(os, "UTF-8");
		
		wr.write(gson.toJson(content));
		wr.flush();
	}

}
