package chav1961.nanohttp.server.parser;

import javax.mail.internet.MimeBodyPart;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/root")
public class MultipartAnnotated {
	@POST
	@Path("/multipart")
	@Consumes({"multipart/form-data"})
	@Produces({"multipart/form-data"})
	public MimeBodyPart get(final MimeBodyPart body) {
		return body;
	}
}
