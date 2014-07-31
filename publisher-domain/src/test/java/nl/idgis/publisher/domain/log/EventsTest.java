package nl.idgis.publisher.domain.log;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EventsTest {

	@Test
	public void testToEvent() {
		assertEquals(GenericEvent.REQUESTED, Events.toEvent("GENERIC.REQUESTED"));
	}
	
	@Test
	public void testToString() {
		assertEquals("GENERIC.REQUESTED", Events.toString(GenericEvent.REQUESTED));
	}
}
