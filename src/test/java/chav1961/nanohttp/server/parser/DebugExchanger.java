package chav1961.nanohttp.server.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.URI;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

import chav1961.purelib.basic.Utils;

public class DebugExchanger extends HttpExchange {
	private final RequestType		type;
	private final URI				uri;
	private final Headers			rqHeaders;
	private final Headers			respHeaders = new Headers();
	private final InetSocketAddress	addr = new InetSocketAddress("localhost", 666);
	private final HttpPrincipal		pr = new HttpPrincipal("test", "file");
	private InputStream				is = null;
	private OutputStream			os = new ByteArrayOutputStream();
	private int						rc = 0;

	public DebugExchanger(final RequestType type, final String uri, final Headers rqHeader) throws IOException {
		this(type, uri, rqHeader, "");
	}	
	
	public DebugExchanger(final RequestType type, final String uri, final Headers rqHeader, final String body) throws IOException {
		if (type == null) {
			throw new NullPointerException("Request type can't be null");
		}
		else if (Utils.checkEmptyOrNullString(uri)) {
			throw new IllegalArgumentException("Request URI can't be null or empty");
		}
		else if (rqHeader == null) {
			throw new NullPointerException("Request headers can't be null");
		}
		else if (body == null) {
			throw new NullPointerException("Body string can't be null");
		}
		else {
			this.type = type;
			this.uri = URI.create("http://"+addr.getHostName()+":"+addr.getPort()+uri);
			this.rqHeaders = rqHeader;
			this.is = new ByteArrayInputStream(body.getBytes("UTF-8"));
		}
	}

	@Override
	public Headers getRequestHeaders() {
		return rqHeaders;
	}

	@Override
	public Headers getResponseHeaders() {
		return respHeaders;
	}

	@Override
	public URI getRequestURI() {
		return uri;
	}

	@Override
	public String getRequestMethod() {
		return type.name();
	}

	@Override
	public HttpContext getHttpContext() {
		return null;
	}

	@Override
	public void close() {
	}

	@Override
	public InputStream getRequestBody() {
		return is;
	}

	@Override
	public OutputStream getResponseBody() {
		return os;
	}

	@Override
	public void sendResponseHeaders(int rCode, long responseLength) throws IOException {
		rc = rCode;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return addr;
	}

	@Override
	public int getResponseCode() {
		return rc;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return addr;
	}

	@Override
	public String getProtocol() {
		return uri.getScheme();
	}

	@Override
	public Object getAttribute(final String name) {
		return null;
	}

	@Override
	public void setAttribute(final String name, final Object value) {
	}

	@Override
	public void setStreams(final InputStream i, final OutputStream o) {
		is = i;
		os = o;
	}

	@Override
	public HttpPrincipal getPrincipal() {
		return pr;
	}

	public String getResponseContent() throws IOException {
		if (os instanceof ByteArrayOutputStream) {
			try(final InputStream	is = new ByteArrayInputStream(((ByteArrayOutputStream)os).toByteArray());
				final Reader		rdr = new InputStreamReader(is, "UTF-8")) {
				
				return Utils.fromResource(rdr);
			}
		}
		else {
			return null;
		}
	}
}
