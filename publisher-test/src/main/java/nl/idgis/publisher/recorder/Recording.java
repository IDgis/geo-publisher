package nl.idgis.publisher.recorder;

import java.util.function.Consumer;

public interface Recording {

	public Recording assertHasNext();

	public Recording assertNotHasNext();

	public <T> Recording assertNext(Class<T> clazz) throws Exception;

	public <T> Recording assertNext(Class<T> clazz, Consumer<T> procedure) throws Exception;

}