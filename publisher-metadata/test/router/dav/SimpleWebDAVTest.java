package router.dav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

import org.junit.Test;
import org.openqa.selenium.io.IOUtils;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;

import model.dav.DefaultResource;
import model.dav.DefaultResourceDescription;
import model.dav.DefaultResourceProperties;

import play.server.Server;

public class SimpleWebDAVTest {
	
	final static int HTTP_PORT = 7000;
	
	@Test
	public void testEmpty() throws Exception {
		Server server = Server.forRouter(new SimpleWebDAVMock("/test/", Collections.emptyList()), HTTP_PORT);
		
		Iterator<DavResource> i = SardineFactory.begin().list("http://localhost:7000/test/").iterator();
		assertTrue(i.hasNext());
		
		DavResource dr = i.next();
		assertNotNull(dr);
		assertTrue(dr.isDirectory());
		
		assertFalse(i.hasNext());
		
		server.stop();
	}
	
	@Test
	public void testFile() throws Exception {
		Date lastModified = new Date(0);
		
		ResourceMock testResource = mock(ResourceMock.class);
		when(testResource.resource()).thenReturn(new DefaultResource("text/plain", "Hello, world!".getBytes("utf-8")));
		when(testResource.resourceDescription()).thenReturn(new DefaultResourceDescription("file", new DefaultResourceProperties(false, lastModified)));
		
		Server server = Server.forRouter(new SimpleWebDAVMock("/test/", Collections.singletonList(testResource)), HTTP_PORT);
		
		Iterator<DavResource> i = SardineFactory.begin().list("http://localhost:7000/test/").iterator();
		assertTrue(i.hasNext());
		
		DavResource dr = i.next();
		assertNotNull(dr);
		assertTrue(dr.isDirectory());
		
		assertTrue(i.hasNext());
		
		dr = i.next();
		assertNotNull(dr);
		assertFalse(dr.isDirectory());
		assertEquals("file", dr.getName());		
		assertEquals(lastModified, dr.getModified());
		
		assertFalse(i.hasNext());
		
		assertEquals("Hello, world!", IOUtils.readFully(new URL("http://localhost:7000/test/file").openStream()));
		
		server.stop();
	}
	
	@Test
	public void testDirectory() throws Exception {
		ResourceMock testResource = mock(ResourceMock.class);
		when(testResource.resource()).thenReturn(new DefaultResource("text/plain", "Hello, world!".getBytes("utf-8")));
		when(testResource.resourceDescription()).thenReturn(new DefaultResourceDescription("file", new DefaultResourceProperties(false)));
		
		Server server = Server.forRouter(
				new SimpleWebDAVMock(
						"/test/",
						Collections.singletonList(
							new SimpleWebDAVMock("/test/directory/", 
								Collections.singletonList(testResource))),
						Collections.emptyList()),
						HTTP_PORT);
		
		Sardine sardine = SardineFactory.begin();
 
		Iterator<DavResource> i = sardine.list("http://localhost:7000/test/").iterator();
		assertTrue(i.hasNext());
		
		DavResource dr = i.next();
		assertNotNull(dr);
		assertTrue(dr.isDirectory());
		assertEquals(new URI("/test/"), dr.getHref());
		
		assertTrue(i.hasNext());
		
		dr = i.next();
		assertNotNull(dr);
		assertEquals(new URI("/test/directory/"), dr.getHref());
		assertTrue(dr.isDirectory());
		
		assertFalse(i.hasNext());
		
		i = sardine.list("http://localhost:7000/test/directory/").iterator();
		assertTrue(i.hasNext());
		
		dr = i.next();
		assertNotNull(dr);
		assertEquals(new URI("/test/directory/"), dr.getHref());
		assertTrue(dr.isDirectory());
		
		assertTrue(i.hasNext());
		
		dr = i.next();
		assertNotNull(dr);
		assertEquals(new URI("/test/directory/file"), dr.getHref());
		assertFalse(dr.isDirectory());
		
		assertFalse(i.hasNext());
 
		i = sardine.list("http://localhost:7000/test/", 0).iterator();
		assertTrue(i.hasNext());
		
		dr = i.next();
		assertNotNull(dr);
		assertEquals(new URI("/test/"), dr.getHref());
		assertTrue(dr.isDirectory());
		
		assertFalse(i.hasNext());

		i = sardine.list("http://localhost:7000/test/", -1).iterator();
		assertTrue(i.hasNext());
		
		dr = i.next();
		assertNotNull(dr);
		assertEquals(new URI("/test/"), dr.getHref());
		assertTrue(dr.isDirectory());
		
		assertTrue(i.hasNext());
		
		dr = i.next();
		assertNotNull(dr);
		assertEquals(new URI("/test/directory/"), dr.getHref());
		assertTrue(dr.isDirectory());
		
		assertTrue(i.hasNext());
		
		dr = i.next();
		assertNotNull(dr);
		assertEquals(new URI("/test/directory/file"), dr.getHref());
		assertFalse(dr.isDirectory());
		
		assertFalse(i.hasNext());
		
		assertEquals("Hello, world!", IOUtils.readFully(new URL("http://localhost:7000/test/directory/file").openStream()));
		
		server.stop();
	}
}
