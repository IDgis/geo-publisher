package nl.idgis.publisher.recorder;

import akka.japi.Procedure;

public interface Recording {

	public Recording assertHasNext();

	public Recording assertNotHasNext();

	public <T> Recording assertNext(Class<T> clazz) throws Exception;

	public <T> Recording assertNext(Class<T> clazz, Procedure<T> procedure) throws Exception;

}