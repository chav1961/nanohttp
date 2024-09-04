package chav1961.nanohttp.server.serializers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import chav1961.nanohttp.server.interfaces.NanoContentSerializer;
import chav1961.purelib.basic.MimeType;

public class TextContentSerializer implements NanoContentSerializer {
	private static final MimeType[]	MIMES = {MimeType.MIME_HTML_TEXT, MimeType.MIME_MARKDOWN_TEXT, MimeType.MIME_CREOLE_TEXT};
	private static final Map<MimeType, Map<MimeType, Function<OutputStream, OutputStream>>> CONVERTORS = new HashMap<>();

	static {
		Map<MimeType, Function<OutputStream, OutputStream>>	data = new HashMap<>();
		
		data.put(MimeType.MIME_HTML_TEXT, (os)->creole2Html(os)); 
		data.put(MimeType.MIME_PLAIN_TEXT, (os)->creole2Plain(os)); 
		data.put(MimeType.MIME_ANY_TEXT, (os)->creole2Plain(os)); 
		CONVERTORS.put(MimeType.MIME_CREOLE_TEXT, data);

		data = new HashMap<>();
		
		data.put(MimeType.MIME_HTML_TEXT, (os)->markdown2Html(os)); 
		data.put(MimeType.MIME_PLAIN_TEXT, (os)->markdown2Plain(os)); 
		data.put(MimeType.MIME_ANY_TEXT, (os)->markdown2Plain(os)); 
		
		CONVERTORS.put(MimeType.MIME_MARKDOWN_TEXT, data);

		data = new HashMap<>();
		data.put(MimeType.MIME_PLAIN_TEXT, (os)->html2Plain(os)); 
		data.put(MimeType.MIME_ANY_TEXT, (os)->html2Plain(os)); 
		
		CONVERTORS.put(MimeType.MIME_HTML_TEXT, data);
	}
	
	@Override
	public MimeType[] getMimeTypes() {
		return MIMES;
	}

	@Override
	public boolean canServe(final MimeType mimeFrom, final MimeType mimeTo) {
		if (mimeFrom == null) {
			throw new NullPointerException("From MIME type can't be null");
		}
		else if (mimeTo == null) {
			throw new NullPointerException("To MIME type can't be null");
		}
		else {
			return CONVERTORS.containsKey(mimeFrom)  && CONVERTORS.get(mimeFrom).containsKey(mimeTo);
		}
	}

	@Override
	public OutputStream serialize(final OutputStream os, final MimeType mimeFrom, final MimeType mimeTo) throws IOException {
		if (os == null) {
			throw new NullPointerException("Output stream can't be null");
		}
		else if (mimeFrom == null) {
			throw new NullPointerException("From MIME type can't be null");
		}
		else if (mimeTo == null) {
			throw new NullPointerException("To MIME type can't be null");
		}
		else if (!canServe(mimeFrom, mimeTo)) {
			throw new IllegalArgumentException("Conversion from ["+mimeFrom+"] to ["+mimeTo+"] is not supported");
		}
		else {
			return CONVERTORS.get(mimeFrom).get(mimeTo).apply(os);
		}
	}
	
	private static OutputStream creole2Html(final OutputStream os) {
		return null;
	}

	private static OutputStream creole2Plain(final OutputStream os) {
		return null;
	}

	private static OutputStream markdown2Html(final OutputStream os) {
		return null;
	}

	private static OutputStream markdown2Plain(final OutputStream os) {
		return null;
	}

	private static OutputStream html2Plain(final OutputStream os) {
		return null;
	}
}
