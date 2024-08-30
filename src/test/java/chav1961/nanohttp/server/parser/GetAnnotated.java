package chav1961.nanohttp.server.parser;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("/root")
public class GetAnnotated {
	@GET
	@Path("/simple")
	public String get() {
		return "response";
	}

	@GET
	@Path("/simple/{uuid}/uuid")
	public String get(@PathParam("uuid")final UUID param) {
		return "response "+param;
	}

	@GET
	@Path("/simple/{long}/long")
	public String get(@PathParam("long")final long param) {
		return "response "+param;
	}

	@GET
	@Path("/simple/{int}/int")
	public String get(@PathParam("int")final int param) {
		return "response "+param;
	}

	@GET
	@Path("/simple/{string}/string")
	public String get(@PathParam("string")final String param) {
		return "response "+param;
	}
	
	@GET
	@Path("/simple/uuid")
	public String getQuery(@QueryParam("q")final UUID value) {
		return "response "+value;
	}

	@GET
	@Path("/simple/long")
	public String getQuery(@QueryParam("q")final long value) {
		return "response "+value;
	}

	@GET
	@Path("/simple/int")
	public String getQuery(@QueryParam("q")final int value) {
		return "response "+value;
	}

	@GET
	@Path("/simple/string")
	public String getQuery(@QueryParam("q")final String value) {
		return "response "+value;
	}
	
	@GET
	@Path("/simple/ret")
	@Produces({"application/json"})
	public Returned getReturned() {
		return new Returned("assa");
	}
	
	@GET
	@Path("/simple/parm/ext/string")
	public String getReturned(final String value) {
		return value;
	}
	
	@GET
	@Path("/simple/parm/json")
	@Consumes({"application/json"})
	@Produces({"application/json"})
	public Returned getReturned(final Returned value) {
		return value;
	}
}