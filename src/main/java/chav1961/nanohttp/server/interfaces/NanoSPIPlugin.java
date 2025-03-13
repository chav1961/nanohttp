package chav1961.nanohttp.server.interfaces;


/**
 * <p>This interface describes SPI plug-in for automatic deploy with the server.Any class that implements 
 * this interface must be declared as SPI service implementation in it's module-info.</p> 
 * @author Alexander Chernomyrdin aka chav1961
 * @since 0.0.1
 */
public interface NanoSPIPlugin {
	/**
	 * <p>Get plug-in instance.</p>
	 * @return any instance annotated with JSR-311. Can't be null. The instance can also implements 
	 * {@linkplain AutoCloseable} interface or it's child to close any resources after un-deploying.</p>
	 * @see <a href="https://jcp.org/en/jsr/detail?id=311">JSR-311</a>
	 */
	Object getPlugin();
	
	/**
	 * <p>Get root path to place plug-in to.</p>
	 * @return root path to place plug-in to. Can be neither null nor empty and must start with '/'. 
	 * Value "/" is correct.
	 */
	String getPath();
}
