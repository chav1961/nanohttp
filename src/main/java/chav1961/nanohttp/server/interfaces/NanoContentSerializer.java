package chav1961.nanohttp.server.interfaces;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import chav1961.nanohttp.server.NanoServiceBuilder;
import chav1961.purelib.basic.MimeType;

/**
 * <p>This interface is used to convert byte or character stream from one MIME type to another (for example
 * from 'text/markdown' to 'text/html'). Any class that implements this interface must be declared
 * as SPI service implementation in it's module-info.</p> 
 * @author Alexander Chernomyrdin aka chav1961
 * @since 0.0.1
 */
public interface NanoContentSerializer {
	/**
	 * <p>Get supported MIME types.</p>
	 * @return list of MIME types. Can't be null and must contain at least one element. No any empties should be in the array. 
	 */
	MimeType[] getMimeTypes();
	
	/**
	 * <p>Can support conversions from one MIME type to another.</p>
	 * @param mimeFrom MIME to convert from. Can be nether null nor empty.
	 * @param mimeTo MIME to convert to. Can be nether null nor empty.
	 * @return true when conversion supported, false otherwise.
	 * @throws IllegalArgumentException any parameter is null or empty.
	 */
	boolean canServe(MimeType mimeFrom, MimeType mimeTo) throws IllegalArgumentException;
	
	/**
	 * <p>Is conversion supported for char-oriented streams.</p>
	 * @return true if supported, false otherwise.
	 * @see #isOutputStreamSupported()
	 */
	boolean isWriterSupported();
	
	/**
	 * <p>Is conversion supported for byte-oriented streams.</p>
	 * @return true if supported, false otherwise.
	 * @see #isWriterSupported()
	 */
	boolean isOutputStreamSupported();
	
	/**
	 * <p>Get output stream to convert nested stream from one MIME to another.</p> 
	 * @param os nested stream with target MIME to convert. Can't be null.
	 * @param mimeFrom MIME to convert from. Can be nether null nor empty. 
	 * @param mimeTo MIME to convert to. Can be nether null nor empty.
	 * @param props service builder was used to create server. Can't be null.
	 * @return stream to convert. Can't be null.
	 * @throws IOException on any I/O errors.
	 * @throws NullPointerException output stream or properties are null.
	 * @throws IllegalArgumentException mime types are null or empty.
	 */
	OutputStream serialize(OutputStream os, MimeType mimeFrom, MimeType mimeTo, NanoServiceBuilder props) throws IOException, NullPointerException, IllegalArgumentException;
	
	/**
	 * <p>Get output stream to convert nested stream from one MIME to another.</p> 
	 * @param os nested stream with target MIME to convert. Can't be null.
	 * @param mimeFrom MIME to convert from. Can be nether null nor empty. 
	 * @param mimeTo MIME to convert to. Can be nether null nor empty.
	 * @param props service builder was used to create server. Can't be null.
	 * @return stream to convert. Can't be null.
	 * @throws IOException on any I/O errors.
	 * @throws NullPointerException output stream or properties are null.
	 * @throws IllegalArgumentException mime types are null or empty.
	 */
	Writer serialize(Writer os, MimeType mimeFrom, MimeType mimeTo, NanoServiceBuilder props) throws IOException, NullPointerException, IllegalArgumentException;
}
