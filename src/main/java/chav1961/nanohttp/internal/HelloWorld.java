package chav1961.nanohttp.internal;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/helloWorld")
public class HelloWorld {
	@GET
	@Path("/demo")
	@Produces({"text/html"})
	public String get() {
		return "<html><body><h1>Hello world!</h1></body></html>";
	}
}
