package nl.idgis.publisher.recorder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.function.Consumer;

import nl.idgis.publisher.recorder.messages.RecordedMessage;

class DefaultRecording implements Recording {
	
	private final Iterator<RecordedMessage> iterator;
	
	public DefaultRecording(Iterator<RecordedMessage> iterator) {
		this.iterator = iterator;
	}
	
	@Override	
	public Recording assertHasNext() {
		return assertHasNext(null);
	}
	
	@Override	
	public Recording assertHasNext(String message) {
		assertTrue(message, iterator.hasNext());
		
		return this;
	}
	
	@Override	
	public Recording assertNotHasNext() {
		return assertNotHasNext(null);
	}
	
	@Override	
	public Recording assertNotHasNext(String message) {
		assertFalse(message, iterator.hasNext());
		
		return this;
	}
	
	@Override
	public <T> Recording assertNext(Class<T> clazz) throws Exception {
		return assertNext(null, clazz, null);
	}
	
	@Override
	public <T> Recording assertNext(String message, Class<T> clazz) throws Exception {
		return assertNext(message, clazz, null);
	}
	
	@Override
	public <T> Recording assertNext(Class<T> clazz, Consumer<T> procedure) throws Exception {
		return assertNext(null, clazz, procedure);
	}
	
	@Override	
	public <T> Recording assertNext(String message, Class<T> clazz, Consumer<T> procedure) throws Exception {
		assertHasNext(message);
		
		Object val = iterator.next().getMessage();
		
		if(message == null) {
			assertTrue("expected: " + clazz.getCanonicalName() + " was: " 
					+ val.getClass().getCanonicalName(), clazz.isInstance(val));
		} else {
			assertTrue(message, clazz.isInstance(val));
		}
		
		if(procedure != null) {
			procedure.accept(clazz.cast(val));
		}
		
		return this;
	}
}