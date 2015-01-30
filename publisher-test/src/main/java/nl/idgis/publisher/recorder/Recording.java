package nl.idgis.publisher.recorder;

import java.util.function.Consumer;

public interface Recording {

	Recording assertHasNext();
	
	Recording assertHasNext(String message);

	Recording assertNotHasNext();
	
	Recording assertNotHasNext(String message);

	<T> Recording assertNext(Class<T> clazz) throws Exception;
	
	<T> Recording assertNext(String message, Class<T> clazz) throws Exception;

	<T> Recording assertNext(Class<T> clazz, Consumer<T> procedure) throws Exception;

	<T> Recording assertNext(String message, Class<T> clazz, Consumer<T> procedure) throws Exception;

}