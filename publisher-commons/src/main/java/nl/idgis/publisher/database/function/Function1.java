package nl.idgis.publisher.database.function;

@FunctionalInterface
public interface Function1<T, R> {
	
	R apply(T t);
}