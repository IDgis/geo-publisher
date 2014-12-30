package nl.idgis.publisher.function;

@FunctionalInterface
public interface Function4<T, U, V, W, R> {
	
	R apply(T t, U u, V v, W w) throws Throwable;
}