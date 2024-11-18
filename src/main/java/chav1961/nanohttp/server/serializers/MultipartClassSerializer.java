package chav1961.nanohttp.server.serializers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import chav1961.nanohttp.server.interfaces.NanoClassSerializer;
import chav1961.purelib.basic.MimeType;

public class MultipartClassSerializer implements NanoClassSerializer {
	private static final MimeType[]	MIMES = {MimeType.MIME_MULTIPART_FORM};

	@Override
	public MimeType[] getMimeTypes() {
		return MIMES;
	}

	@Override
	public <T> boolean canServe(MimeType mime, Class<T> awaited) {
		if (mime == null) {
			throw new NullPointerException("Mime type can't be null");
		}
		else if (awaited == null) {
			throw new NullPointerException("Awaited class can;t be null");
		}
		else {
			return MimeType.MIME_MULTIPART_FORM.equals(mime) && MimeBodyPart.class.isAssignableFrom(awaited);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T deserialize(final InputStream is, final Class<T> awaited) throws IOException {
		if (!MimeBodyPart.class.isAssignableFrom(awaited)) {
			throw new IOException("Only MimeBodyPart class is available here");
		}
		else {
			try {
				return (T) new MimeBodyPart(is);
			} catch (MessagingException e) {
				throw new IOException(e);
			}
		}
	}

	@Override
	public <T> void serialize(final T content, final OutputStream os) throws IOException {
		if (!(content instanceof MimeBodyPart)) {
			throw new IOException("Only MimeBodyPart class is available here");
		}
		else {
			try {
				((MimeBodyPart)content).writeTo(os);
			} catch (IOException | MessagingException e) {
				throw new IOException(e);
			}
		}
	}

}
