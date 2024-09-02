package chav1961.nanohttp.server;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

@Path("/child")
public class TestClass {
	@GET
	@Path("/get")
	public String get() {
		return "response";
	}

	@POST
	@Path("/post")
	public String post(final String body) {
		return body;
	}

	@PUT
	@Path("/put")
	public String put(final String body) {
		return body;
	}

	@DELETE
	@Path("/delete")
	public void delete() {
	}
}
