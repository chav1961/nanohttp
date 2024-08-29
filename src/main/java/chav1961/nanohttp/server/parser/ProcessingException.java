package chav1961.nanohttp.server.parser;

public class ProcessingException extends Exception {
	private static final long serialVersionUID = 3345781917486988335L;

	private final int statusCode;

	public ProcessingException(final int statusCode) {
		super();
		this.statusCode = statusCode;
	}

	public ProcessingException(final int statusCode, final String message, final Throwable cause) {
		super(message, cause);
		this.statusCode = statusCode;
	}

	public ProcessingException(final int statusCode, final String message) {
		super(message);
		this.statusCode = statusCode;
	}

	public ProcessingException(final int statusCode, final Throwable cause) {
		super(cause);
		this.statusCode = statusCode;
	}

	public int getStatusCode() {
		return statusCode;
	}
}
