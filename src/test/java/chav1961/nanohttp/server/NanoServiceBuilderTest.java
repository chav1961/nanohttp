package chav1961.nanohttp.server;

import java.io.IOException;
import java.net.URI;

import org.junit.Assert;
import org.junit.Test;

import chav1961.purelib.basic.SubstitutableProperties;

public class NanoServiceBuilderTest {
	@Test
	public void defaultsTest() {
		final NanoServiceBuilder	bldr = NanoServiceBuilder.newInstance();
		
		Assert.assertEquals(8080, bldr.getSocketAddress().getPort());
		Assert.assertEquals(URI.create("fsys:file:./root"), bldr.getRoot());
		Assert.assertTrue(bldr.isLocalhostOnly());
		Assert.assertTrue(bldr.isDisableLoopback());
		Assert.assertEquals(10, bldr.getExecutorPoolSize());
		Assert.assertEquals(0, bldr.getTemporaryCacheSize());
		Assert.assertNull(bldr.getCreolePrologueURI());
		Assert.assertNull(bldr.getCreoleEpilogueURI());
		Assert.assertFalse(bldr.needUseSSL());
	}
	
	@Test
	public void explicitsTest() throws IOException {
		final NanoServiceBuilder	bldr = NanoServiceBuilder.of(SubstitutableProperties.of(getClass().getResourceAsStream("/nanoservice.conf")));

		Assert.assertEquals(6666, bldr.getSocketAddress().getPort());
		Assert.assertEquals(URI.create("fsys:file:./src/test/resources/content"), bldr.getRoot());
		Assert.assertTrue(bldr.isLocalhostOnly());
		Assert.assertTrue(bldr.isDisableLoopback());
		Assert.assertEquals(3, bldr.getExecutorPoolSize());
		Assert.assertEquals(0, bldr.getTemporaryCacheSize());
		Assert.assertNull(bldr.getCreolePrologueURI());
		Assert.assertNull(bldr.getCreoleEpilogueURI());
		Assert.assertFalse(bldr.needUseSSL());
		
		try {
			NanoServiceBuilder.of(null);
			Assert.fail("Mandatory exception was ot detected (null 1-st argument)");
		} catch (NullPointerException exc) {
		}
	}	
}
