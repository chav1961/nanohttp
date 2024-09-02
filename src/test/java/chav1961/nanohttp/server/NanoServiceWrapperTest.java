package chav1961.nanohttp.server;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URI;

import org.junit.Assert;
import org.junit.Test;

import chav1961.nanohttp.server.parser.RequestType;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.SyntaxException;
import chav1961.purelib.fsys.FileSystemOnFile;
import chav1961.purelib.fsys.interfaces.FileSystemInterface;

public class NanoServiceWrapperTest {
	@Test
	public void lifeCycleTest() throws IOException {
		final NanoServiceBuilder	bldr = NanoServiceBuilder.of(SubstitutableProperties.of(getClass().getResourceAsStream("/nanoservice.conf")));
		
		try(final NanoServiceWrapper wrapper = bldr.build()) {
			Assert.assertFalse(wrapper.isStarted());
			Assert.assertFalse(wrapper.isSuspended());
			wrapper.start();
			Assert.assertTrue(wrapper.isStarted());
			Assert.assertFalse(wrapper.isSuspended());
			
			try{wrapper.start();
				Assert.fail("Mandatory exception was not detected (server already started)");
			} catch (IllegalStateException exc) {
			}
			
			wrapper.suspend();
			Assert.assertTrue(wrapper.isStarted());
			Assert.assertTrue(wrapper.isSuspended());

			try{wrapper.suspend();
				Assert.fail("Mandatory exception was not detected (server already susended)");
			} catch (IllegalStateException exc) {
			}
		
			wrapper.resume();
			Assert.assertTrue(wrapper.isStarted());
			Assert.assertFalse(wrapper.isSuspended());

			try{wrapper.resume();
				Assert.fail("Mandatory exception was not detected (server not susended)");
			} catch (IllegalStateException exc) {
			}

			wrapper.stop();
			Assert.assertFalse(wrapper.isStarted());
			Assert.assertFalse(wrapper.isSuspended());
			
			try{wrapper.stop();
				Assert.fail("Mandatory exception was not detected (server already stopped)");
			} catch (IllegalStateException exc) {
			}
			
			Assert.assertEquals(6666, wrapper.getServerAddress().getPort());
			Assert.assertEquals(new File("./src/test/resources/content/").getAbsoluteFile().toURI().normalize().toString(), wrapper.getServiceRoot().getAbsoluteURI().normalize().toASCIIString());
			Assert.assertTrue(wrapper.getServiceRoot().clone().open("/x.txt").exists());
		}
	}

	@Test
	public void callTest() throws IOException {
		final NanoServiceBuilder	bldr = NanoServiceBuilder.of(SubstitutableProperties.of(getClass().getResourceAsStream("/nanoservice.conf")));
		
		try(final NanoServiceWrapper wrapper = bldr.build()) {
			wrapper.start();
			try(final InputStream 		is = URI.create("http://localhost:6666/x.txt").toURL().openStream();
				final Reader			rdr = new InputStreamReader(is);
				final BufferedReader	brdr = new BufferedReader(rdr)) {
				
				Assert.assertEquals("test string", brdr.readLine());
			}
		}
	}

	@Test
	public void staticDeploymentTest() throws IOException, SyntaxException, ContentException {
		final NanoServiceBuilder	bldr = NanoServiceBuilder.of(SubstitutableProperties.of(getClass().getResourceAsStream("/nanoservice.conf")));
		
		try(final NanoServiceWrapper 	wrapper = bldr.build();
			final FileSystemInterface	child = new FileSystemOnFile(URI.create("file:./src/test/resources/child"))) {
			
			wrapper.start();
			wrapper.deploy("/inner", child);
			
			try {
				wrapper.deploy("/inner", child);
				Assert.fail("Mandatory exception was not detected (duplicate deployment on the same path)");
			} catch (IllegalStateException exc) {
			}
			
			try(final InputStream 		is = URI.create("http://localhost:6666/inner/y.txt").toURL().openStream();
				final Reader			rdr = new InputStreamReader(is);
				final BufferedReader	brdr = new BufferedReader(rdr)) {
				
				Assert.assertEquals("another test string", brdr.readLine());
			}
			Assert.assertEquals(child, wrapper.undeploy("/inner"));

			try {
				wrapper.undeploy("/inner");
				Assert.fail("Mandatory exception was not detected (no any deployment on this same path)");
			} catch (IllegalStateException exc) {
			}
		}
	}

	@Test
	public void servletDeploymentTest() throws IOException, SyntaxException, ContentException {
		final NanoServiceBuilder	bldr = NanoServiceBuilder.of(SubstitutableProperties.of(getClass().getResourceAsStream("/nanoservice.conf")));
		
		try(final NanoServiceWrapper 	wrapper = bldr.build()) {
			final TestClass		tc = new TestClass();
			
			wrapper.start();
			wrapper.deploy("/inner", tc);

			Assert.assertEquals("response", processRequest("/child/get", RequestType.GET, null));
			Assert.assertEquals("test string", processRequest("/child/post", RequestType.POST, "test string"));
			Assert.assertEquals("test string", processRequest("/child/put", RequestType.PUT, "test string"));
			processRequest("/child/delete", RequestType.DELETE, null);
			
			wrapper.undeploy("/inner/child");
			wrapper.stop();
		}
	}
	
	private String processRequest(final String uriTail, final RequestType method, final String content) throws IOException {
		final HttpURLConnection	conn = (HttpURLConnection) URI.create("http://localhost:6666/inner"+uriTail).toURL().openConnection();
		
		conn.setDoInput(true);
		conn.setRequestMethod(method.name());
		if (content != null) {
			conn.setDoOutput(true);
			try(final OutputStream	os = conn.getOutputStream();
				final Writer		wr = new OutputStreamWriter(os)) {
				
				wr.write(content);
				wr.flush();
			}
		}
		try(final InputStream 		is = conn.getInputStream();
			final Reader			rdr = new InputStreamReader(is);
			final BufferedReader	brdr = new BufferedReader(rdr)) {
			
			return brdr.readLine();
		}
		
	}
}
