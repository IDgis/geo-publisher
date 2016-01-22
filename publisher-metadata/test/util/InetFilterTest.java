package util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import util.InetFilter.FilterElement;

public class InetFilterTest {
	
	// Reserved addresses from: RFC 5737 (IPv4) and RFC 3849 (IPv6)
	private static final String FILTER_CONFIGURATION = "192.0.2.1,198.51.100.0/24,2001:db8:1::1,2001:db8:2::/64";
	
	private InetFilter filter;
	
	@Before
	public void setUp() {
		filter = new InetFilter(FILTER_CONFIGURATION);
	}
	
	@Test
	public void testFilterElements() {
		Iterator<String> itr = filter.getFilterElements().stream()
			.map(Object::toString)
			.iterator();
		
		assertTrue(itr.hasNext());
		assertEquals("FilterElement [192.0.2.1/32]", itr.next());
		
		assertTrue(itr.hasNext());
		assertEquals("FilterElement [198.51.100.0/24]", itr.next());
		
		assertTrue(itr.hasNext());
		assertEquals("FilterElement [2001:db8:1:0:0:0:0:1/128]", itr.next());
		
		assertTrue(itr.hasNext());
		assertEquals("FilterElement [2001:db8:2:0:0:0:0:0/64]", itr.next());
		
		assertFalse(itr.hasNext());
	}

	@Test
	public void testAllowed() throws Exception {
		assertTrue(filter.isAllowed(InetAddress.getByName("192.0.2.1")));
		assertTrue(filter.isAllowed(InetAddress.getByName("198.51.100.1")));
		assertTrue(filter.isAllowed(InetAddress.getByName("2001:db8:1::1")));
		assertTrue(filter.isAllowed(InetAddress.getByName("2001:db8:2::1")));
	}
	
	@Test
	public void testNotAllowed() throws Exception {
		assertFalse(filter.isAllowed(InetAddress.getByName("192.0.2.2")));
		assertFalse(filter.isAllowed(InetAddress.getByName("192.0.200.1")));
		assertFalse(filter.isAllowed(InetAddress.getByName("2001:db8:1::2")));
		assertFalse(filter.isAllowed(InetAddress.getByName("2001:db8:3::1")));
	}
	
	@Test
	public void testEmptyConfig() {
		InetFilter filter = new InetFilter("");
		assertTrue(filter.getFilterElements().isEmpty());
	}
	
	@Test
	public void testSingleItemConfig() {
		InetFilter filter = new InetFilter("127.0.0.1");		
		List<FilterElement> elements = filter.getFilterElements();
		
		assertEquals(1, elements.size());
		assertEquals("FilterElement [127.0.0.1/32]", elements.get(0).toString());
	}
}
