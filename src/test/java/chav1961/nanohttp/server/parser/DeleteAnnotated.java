package chav1961.nanohttp.server.parser;

import java.util.UUID;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.junit.Assert;

@Path("/root")
public class DeleteAnnotated {
	@DELETE
	@Path("/simple")
	public void delete() {
	}

	@DELETE
	@Path("/simple/{uuid}/uuid")
	public void delete(@PathParam("uuid")final UUID param) {
		Assert.assertNotNull(param);
	}

	@DELETE
	@Path("/simple/test")
	public void delete(@QueryParam("q")final String value) {
		Assert.assertNotNull(value);
	}

	@DELETE
	@Path("/simple/parm")
	public void deleteParm(final String value) {
		Assert.assertNotNull(value);
	}
}