package nl.idgis.publisher.protocol.stream;

public interface StreamHandle<T> {

	void item(T t);

	void failure(String message);
}
