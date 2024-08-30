package chav1961.nanohttp.server.parser;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("/root")
public class PutAnnotated {
	@PUT
	@Path("/simple")
	public String simplePut() {
		return "response";
	}

	@PUT
	@Path("/simple/{uuid}/uuid")
	public String simplePut(@PathParam("uuid")final UUID param) {
		return "response "+param;
	}

	@PUT
	@Path("/simple/ret")
	@Produces({"application/json"})
	public Returned simplePutReturned() {
		return new Returned("assa");
	}
	
	@PUT
	@Path("/simple/parm")
	public String simplePutReturned(final String value) {
		return value;
	}
	
	@PUT
	@Path("/simple/body")
	@Consumes({"application/json"})
	@Produces({"application/json"})
	public Returned simplePutReturned(final Returned value) {
		return value;
	}
}