package nl.idgis.publisher.function;

@FunctionalInterface
public interface Function2<T, U, R> {
	
	R apply(T t, U u) throws Throwable;
}