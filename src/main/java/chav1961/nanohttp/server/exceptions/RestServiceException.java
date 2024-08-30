package chav1961.nanohttp.server.exceptions;

public class RestServiceException extends RuntimeException {
	private static final long serialVersionUID = -713277847416829873L;
	
	private final int	rc;

	public RestServiceException(final int rc) {
		super();
		this.rc = rc;
	}

	public RestServiceException(final int rc, final String message, final Throwable cause) {
		super(message, cause);
		this.rc = rc;
	}

	public RestServiceException(final int rc, final String message) {
		super(message);
		this.rc = rc;
	}

	public RestServiceException(final int rc, final Throwable cause) {
		super(cause);
		this.rc = rc;
	}

	public int getResponseCode() {
		return rc;
	}
	
}
