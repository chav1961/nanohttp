package chav1961.nanohttp.server.parser;

import java.lang.annotation.Annotation;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;

public enum RequestType {
	GET(GET.class),
	POST(POST.class),
	PUT(PUT.class),
	DELETE(DELETE.class),
	HEAD(HEAD.class),
	OPTIONS(OPTIONS.class);
	
	private final Class<? extends Annotation> anno;
	
	private RequestType(final Class<? extends Annotation> anno) {
		this.anno = anno;
	}
	
	public Class<? extends Annotation> getAnnotationAssociated() {
		return anno;
	}
}
