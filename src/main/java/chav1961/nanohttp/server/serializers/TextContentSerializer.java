package chav1961.nanohttp.server.serializers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import chav1961.nanohttp.server.interfaces.NanoContentSerializer;
import chav1961.purelib.basic.MimeType;

public class TextContentSerializer implements NanoContentSerializer {
	private static final MimeType[]	MIMES = {MimeType.MIME_HTML_TEXT, MimeType.MIME_PLAIN_TEXT, MimeType.MIME_MARKDOWN_TEXT, MimeType.MIME_CREOLE_TEXT, MimeType.MIME_ANY_TEXT};

	@Override
	public MimeType[] getMimeTypes() {
		return MIMES;
	}

	@Override
	public boolean canServe(final MimeType mimeFrom, final MimeType mimeTo) {
		// TODO Auto-generated method stub
		if (mimeFrom == null) {
			throw new NullPointerException("From MIME type can't be null");
		}
		else if (mimeTo == null) {
			throw new NullPointerException("To MIME type can't be null");
		}
		else {
			return false;
		}
	}

	@Override
	public InputStream deserialize(final InputStream is, final MimeType mimeFrom, final MimeType mimeTo) throws IOException {
		// TODO Auto-generated method stub
		if (is == null) {
			throw new NullPointerException("Input stream can't be null");
		}
		else if (mimeFrom == null) {
			throw new NullPointerException("From MIME type can't be null");
		}
		else if (mimeTo == null) {
			throw new NullPointerException("To MIME type can't be null");
		}
		else {
			return null;
		}
	}

	@Override
	public OutputStream serialize(final OutputStream os, final MimeType mimeFrom, final MimeType mimeTo) throws IOException {
		// TODO Auto-generated method stub
		if (os == null) {
			throw new NullPointerException("Output stream can't be null");
		}
		else if (mimeFrom == null) {
			throw new NullPointerException("From MIME type can't be null");
		}
		else if (mimeTo == null) {
			throw new NullPointerException("To MIME type can't be null");
		}
		else {
			return null;
		}
	}
}
