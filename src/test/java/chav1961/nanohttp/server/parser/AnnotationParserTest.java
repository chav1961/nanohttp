package chav1961.nanohttp.server.parser;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.ws.rs.WebApplicationException;

import org.junit.Assert;
import org.junit.Test;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import chav1961.nanohttp.server.exceptions.RestServiceException;

public class AnnotationParserTest {
	public static final UUID	TEST_UUID = UUID.fromString("ff3b7c9f-41ba-4f34-9e4b-64f7b080cdf7");

	@Test
	public void basicTest() throws WebApplicationException, IOException {
		final GetAnnotated	ga = new GetAnnotated();
		final AnnotationParser<GetAnnotated>	ap = new AnnotationParser<>(ga);
		final HttpExchange	ex = new DebugExchanger(RequestType.GET, "/unknown", new Headers());
		
		try {
			ap.processRequest(ex);
			Assert.fail("Mandatory exception was not detected (request not found)");
		} catch (RestServiceException exc) {
			Assert.assertEquals(403, exc.getResponseCode());
		}
		
		try{ap.processRequest(null);
			Assert.fail("Mandatory exception was not detected (null 1-st argument)");
		} catch (NullPointerException exc) {
		}

		try{new AnnotationParser<>(null);
			Assert.fail("Mandatory exception was not detected (null 1-st argument)");
		} catch (NullPointerException exc) {
		}
		try{new AnnotationParser<>("test string");
			Assert.fail("Mandatory exception was not detected (class is not annotated)");
		} catch (IllegalArgumentException exc) {
		}
	}

	@Test
	public void getTest() throws WebApplicationException, IOException {
		final GetAnnotated	ga = new GetAnnotated();
		final AnnotationParser<GetAnnotated>	ap = new AnnotationParser<>(ga);
		DebugExchanger	ex = new DebugExchanger(RequestType.GET, "/root/simple", new Headers());

		ap.processRequest(ex);
		Assert.assertEquals(200, ex.getResponseCode());
		Assert.assertEquals("response", ex.getResponseContent());

		ex = new DebugExchanger(RequestType.GET, "/root/simple/"+TEST_UUID+"/uuid", new Headers());

		ap.processRequest(ex);
		Assert.assertEquals(200, ex.getResponseCode());
		Assert.assertEquals("response "+TEST_UUID, ex.getResponseContent());

		ex = new DebugExchanger(RequestType.GET, "/root/simple/100/long", new Headers());

		ap.processRequest(ex);
		Assert.assertEquals(200, ex.getResponseCode());
		Assert.assertEquals("response 100", ex.getResponseContent());

		ex = new DebugExchanger(RequestType.GET, "/root/simple/200/int", new Headers());

		ap.processRequest(ex);
		Assert.assertEquals(200, ex.getResponseCode());
		Assert.assertEquals("response 200", ex.getResponseContent());
	
		ex = new DebugExchanger(RequestType.GET, "/root/simple/vassya/string", new Headers());

		ap.processRequest(ex);
		Assert.assertEquals(200, ex.getResponseCode());
		Assert.assertEquals("response vassya", ex.getResponseContent());

		ex = new DebugExchanger(RequestType.GET, "/root/simple/uuid?q="+TEST_UUID, new Headers());

		ap.processRequest(ex);
		Assert.assertEquals(200, ex.getResponseCode());
		Assert.assertEquals("response "+TEST_UUID, ex.getResponseContent());

		ex = new DebugExchanger(RequestType.GET, "/root/simple/long?q=100", new Headers());

		ap.processRequest(ex);
		Assert.assertEquals(200, ex.getResponseCode());
		Assert.assertEquals("response 100", ex.getResponseContent());

		ex = new DebugExchanger(RequestType.GET, "/root/simple/int?q=200", new Headers());

		ap.processRequest(ex);
		Assert.assertEquals(200, ex.getResponseCode());
		Assert.assertEquals("response 200", ex.getResponseContent());

		ex = new DebugExchanger(RequestType.GET, "/root/simple/string?q=vassya", new Headers());

		ap.processRequest(ex);
		Assert.assertEquals(200, ex.getResponseCode());
		Assert.assertEquals("response vassya", ex.getResponseContent());

		ex = new DebugExchanger(RequestType.GET, "/root/simple/ret", Headers.of("Accept","application/json"));

		ap.processRequest(ex);
		Assert.assertEquals(200, ex.getResponseCode());
		Assert.assertEquals("{\"value\":\"assa\"}", ex.getResponseContent());

		ex = new DebugExchanger(RequestType.GET, "/root/simple/parm/ext/string", new Headers());

		ap.processRequest(ex, "test");
		Assert.assertEquals(200, ex.getResponseCode());
		Assert.assertEquals("test", ex.getResponseContent());

		ex = new DebugExchanger(RequestType.GET, "/root/simple/parm/json", 
						Headers.of("Accept","application/json","Content-Type","application/json"),
						"{\"value\":\"msinana\"}");

		try{ap.processRequest(ex);
			Assert.fail("Mandatory exception was not detected (request can't have body)");
		} catch (RestServiceException exc) {
			Assert.assertEquals(415, exc.getResponseCode());
		}
	}

	@Test
	public void postTest() throws WebApplicationException, IOException {
		final PostAnnotated	pa = new PostAnnotated();
		final AnnotationParser<PostAnnotated>	ap = new AnnotationParser<>(pa);
		DebugExchanger	ex = new DebugExchanger(RequestType.POST, "/root/simple", new Headers());

		ap.processRequest(ex);
		Assert.assertEquals(201, ex.getResponseCode());
		Assert.assertEquals("response", ex.getResponseContent());
		
		ex = new DebugExchanger(RequestType.POST, "/root/simple/"+TEST_UUID+"/uuid", new Headers());

		ap.processRequest(ex);
		Assert.assertEquals(201, ex.getResponseCode());
		Assert.assertEquals("response "+TEST_UUID, ex.getResponseContent());
		
		ex = new DebugExchanger(RequestType.POST, "/root/simple/ret", Headers.of("Accept","application/json"));

		ap.processRequest(ex);
		Assert.assertEquals(201, ex.getResponseCode());
		Assert.assertEquals("{\"value\":\"assa\"}", ex.getResponseContent());

		ex = new DebugExchanger(RequestType.POST, "/root/simple/parm", new Headers());

		ap.processRequest(ex, "test");
		Assert.assertEquals(201, ex.getResponseCode());
		Assert.assertEquals("test", ex.getResponseContent());

		ex = new DebugExchanger(RequestType.POST, "/root/simple/body", 
				Headers.of("Accept","application/json","Content-Type","application/json"), 
				"{\"value\":\"mzinana\"}");

		ap.processRequest(ex);
		Assert.assertEquals(201, ex.getResponseCode());
		Assert.assertEquals("{\"value\":\"mzinana\"}", ex.getResponseContent());
	}

	@Test
	public void putTest() throws WebApplicationException, IOException {
		final PutAnnotated	pa = new PutAnnotated();
		final AnnotationParser<PutAnnotated>	ap = new AnnotationParser<>(pa);
		DebugExchanger	ex = new DebugExchanger(RequestType.PUT, "/root/simple", new Headers());

		ap.processRequest(ex);
		Assert.assertEquals(200, ex.getResponseCode());
		Assert.assertEquals("response", ex.getResponseContent());
		
		ex = new DebugExchanger(RequestType.PUT, "/root/simple/"+TEST_UUID+"/uuid", new Headers());

		ap.processRequest(ex);
		Assert.assertEquals(200, ex.getResponseCode());
		Assert.assertEquals("response "+TEST_UUID, ex.getResponseContent());
		
		ex = new DebugExchanger(RequestType.PUT, "/root/simple/ret", Headers.of("Accept","application/json"));

		ap.processRequest(ex);
		Assert.assertEquals(200, ex.getResponseCode());
		Assert.assertEquals("{\"value\":\"assa\"}", ex.getResponseContent());

		ex = new DebugExchanger(RequestType.PUT, "/root/simple/parm", new Headers());

		ap.processRequest(ex, "test");
		Assert.assertEquals(200, ex.getResponseCode());
		Assert.assertEquals("test", ex.getResponseContent());

		ex = new DebugExchanger(RequestType.PUT, "/root/simple/body", 
				Headers.of("Accept","application/json","Content-Type","application/json"), 
				"{\"value\":\"mzinana\"}");

		ap.processRequest(ex);
		Assert.assertEquals(200, ex.getResponseCode());
		Assert.assertEquals("{\"value\":\"mzinana\"}", ex.getResponseContent());
	}

	@Test
	public void deleteTest() throws WebApplicationException, IOException {
		final DeleteAnnotated	da = new DeleteAnnotated();
		final AnnotationParser<DeleteAnnotated>	ap = new AnnotationParser<>(da);
		DebugExchanger	ex = new DebugExchanger(RequestType.DELETE, "/root/simple", new Headers());

		ap.processRequest(ex);
		Assert.assertEquals(200, ex.getResponseCode());
		
		ex = new DebugExchanger(RequestType.DELETE, "/root/simple/"+TEST_UUID+"/uuid", new Headers());

		ap.processRequest(ex);
		Assert.assertEquals(200, ex.getResponseCode());
		
		ex = new DebugExchanger(RequestType.DELETE, "/root/simple/parm", new Headers());

		ap.processRequest(ex, "test");
		Assert.assertEquals(200, ex.getResponseCode());
	}

	@Test
	public void multipartTest() throws WebApplicationException, IOException, MessagingException {
		final MultipartAnnotated	pa = new MultipartAnnotated();
		final AnnotationParser<MultipartAnnotated>	ap = new AnnotationParser<>(pa);
		final MimeBodyPart			mm = new MimeBodyPart();
		final ByteArrayOutputStream	os = new ByteArrayOutputStream();
		final InputStream			is;

		mm.setDescription("mzinana");
		mm.setText("assa");
		
		try (final ByteArrayOutputStream	baos = new ByteArrayOutputStream()) {
			mm.writeTo(baos);
			baos.flush();
			is = new ByteArrayInputStream(baos.toByteArray());
		}
		
		DebugExchanger	ex = new DebugExchanger(RequestType.POST, "/root/multipart", Headers.of("Accept","multipart/form-data","Content-Type","multipart/form-data"));

		ex.setStreams(is, os);
		ap.processRequest(ex);
		Assert.assertEquals(201, ex.getResponseCode());

		final MimeBodyPart			mmNew = new MimeBodyPart(new ByteArrayInputStream(os.toByteArray()));
		
		Assert.assertEquals("mzinana", mmNew.getDescription());
	}
}
