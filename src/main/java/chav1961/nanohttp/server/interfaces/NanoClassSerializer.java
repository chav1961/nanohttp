package chav1961.nanohttp.server.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import chav1961.purelib.basic.MimeType;

/**
 * <p>THis interface is used to convert message header's or message body's content to any supported class and inverted conversion too.
 * Any class that implements this interface must be declared as SPI service implementation in it's module-info.</p> 
 * @author Alexander Chernomyrdin aka chav1961
 * @since 0.0.1
 */
public interface NanoClassSerializer {
	/**
	 * <p>Get MIME types supported.</p>
	 * @return MIME types supported. Can't be null and must contain at least one element
	 */
	MimeType[] getMimeTypes();
	
	/**
	 * <p>Can convert content with MIME type passed to or from the given class.</p>
	 * @param <T> class type to test.
	 * @param mime MIME type to test. Can't be null.
	 * @param awaited class to convert content from/to. Can't be null. 
	 * @return true if conversion supported, false otherwise.
	 * @throws NullPointerException on any argument is null
	 */
	<T> boolean canServe(MimeType mime, Class<T> awaited) throws NullPointerException;
	
	/**
	 * <p>Convert input stream content with MIME passed to the given class.</p>	
	 * @param <T> class type to convert to.
	 * @param is content to convert from. Can't be null.
	 * @param awaited awaited class. Can't be null.
	 * @return class de-serialized. Can't be null.
	 * @throws IOException on any I/O errors.
	 * @throws NullPointerException on any argument is null
	 */
	<T> T deserialize(InputStream is, Class<T> awaited) throws IOException, NullPointerException;
	
	/**
	 * <p>Convert class instance to output stream with MIME passed.</p>
	 * @param <T> class type to convert from.
	 * @param content instance to convert from. Can't be null.
	 * @param os stream to convert to. Can't be null.
	 * @throws IOException on any I/O errors.
	 * @throws NullPointerException on any argument is null
	 */
	<T> void serialize(T content, OutputStream os) throws IOException, NullPointerException;
}
