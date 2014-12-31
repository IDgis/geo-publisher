package nl.idgis.publisher.function;

@FunctionalInterface
public interface Procedure1<T> {

	void apply(T t) throws Throwable;
}
