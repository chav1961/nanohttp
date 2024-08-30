package chav1961.nanohttp.server.parser;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("/root")
public class PostAnnotated {
	@POST
	@Path("/simple")
	public String post() {
		return "response";
	}

	@POST
	@Path("/simple/{uuid}/uuid")
	public String post(@PathParam("uuid")final UUID param) {
		return "response "+param;
	}

	@POST
	@Path("/simple/ret")
	@Produces({"application/json"})
	public Returned postReturned() {
		return new Returned("assa");
	}
	
	@POST
	@Path("/simple/parm")
	public String postReturned(final String value) {
		return value;
	}
	
	@POST
	@Path("/simple/body")
	@Consumes({"application/json"})
	@Produces({"application/json"})
	public Returned postReturned(final Returned value) {
		return value;
	}
}