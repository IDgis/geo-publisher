package nl.idgis.publisher.database.function;

@FunctionalInterface
public interface Function2<T, U, R> {
	
	R apply(T t, U u);
}