package chav1961.nanohttp.server.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>This interface is used to encode/decode stream of byte sequences (request and response bodies) due to
 * bodies encoding (for example, 'gzip' encoding). Any class that implements this interface must be declared
 * as SPI service implementation in it's module-info.</p> 
 * @author Alexander Chernomyrdin aka chav1961
 * @since 0.0.1
 */
public interface NanoContentEncoder {
	/**
	 * <p>Get supported encodings.<p>
	 * @return array of supported encodings. Can't be null and must contain at least one element. 
	 * No any elements can be empty 
	 */
	String[] getEncodings();
	
	/**
	 * <p>Can the encoder/decoder support the given encoding</p>
	 * @param encoding encoding to test. Can be neither null nor empty
	 * @return true if the encoding is supported, false otherwise.
	 * @throws IllegalArgumentException encoding string is null or empty
	 */
	boolean canServe(String encoding) throws IllegalArgumentException;
	
	/**
	 * <p>Get input stream to decode nested stream passed</p> 
	 * @param is nested stream to decode. Can't be null.
	 * @param encoding encoding type. Can be neither null nor empty
	 * @return stream to decode nested stream. Can't be null.
	 * @throws IOException on any I/O errors
	 * @throws NullPointerException nested stream is null
	 * @throws IllegalArgumentException encoding string is null or empty
	 */
	InputStream decode(InputStream is, String encoding) throws IOException, NullPointerException, IllegalArgumentException;
	
	/**
	 * <p>Get output stream to encode nested steam passed.</p>
	 * @param os nestes stream to encode. Can't be null.
	 * @param encoding encoding type. Can be neither null nor empty
	 * @return stream to encode nested stream. Can't be null.
	 * @throws IOException on any I/O errors
	 * @throws NullPointerException nested stream is null
	 * @throws IllegalArgumentException encoding string is null or empty
	 */
	OutputStream encode(OutputStream os, String encoding) throws IOException, NullPointerException, IllegalArgumentException;
}
