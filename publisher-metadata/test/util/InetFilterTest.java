package util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;

import org.junit.Before;
import org.junit.Test;

public class InetFilterTest {
	
	// Reserved addresses from: RFC 5737 (IPv4) and RFC 3849 (IPv6)
	private static final String FILTER_CONFIGURATION = "192.0.2.1,198.51.100.0/24,2001:db8:1::1,2001:db8:2::/64";
	
	private InetFilter filter;
	
	@Before
	public void setUp() {
		filter = new InetFilter(FILTER_CONFIGURATION);
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
}
