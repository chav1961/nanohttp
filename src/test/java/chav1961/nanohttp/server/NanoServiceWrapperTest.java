package chav1961.nanohttp.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;

import org.junit.Assert;
import org.junit.Test;

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
			} catch (IllegalArgumentException exc) {
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
			} catch (IllegalArgumentException exc) {
			}
		}
	}
}
