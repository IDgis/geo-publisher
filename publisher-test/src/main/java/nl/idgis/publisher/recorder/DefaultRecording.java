package nl.idgis.publisher.recorder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import nl.idgis.publisher.recorder.messages.RecordedMessage;

import akka.japi.Procedure;

class DefaultRecording implements Recording {
	
	private final Iterator<RecordedMessage> iterator;
	
	public DefaultRecording(Iterator<RecordedMessage> iterator) {
		this.iterator = iterator;
	}
	
	@Override	
	public Recording assertHasNext() {
		assertTrue(iterator.hasNext());
		
		return this;
	}
	
	@Override	
	public Recording assertNotHasNext() {
		assertFalse(iterator.hasNext());
		
		return this;
	}
	
	@Override
	public <T> Recording assertNext(Class<T> clazz) throws Exception {
		return assertNext(clazz, null);
	}
	
	@Override	
	public <T> Recording assertNext(Class<T> clazz, Procedure<T> procedure) throws Exception {
		Object val = iterator.next().getMessage();
		
		assertTrue("expected: " + clazz.getCanonicalName() + " was: " 
				+ val.getClass().getCanonicalName(), clazz.isInstance(val));
		
		if(procedure != null) {
			procedure.apply(clazz.cast(val));
		}
		
		return this;
	}
}