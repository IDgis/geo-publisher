package nl.idgis.publisher.function;

@FunctionalInterface
public interface Function1<T, R> {
	
	R apply(T t) throws Throwable;
}