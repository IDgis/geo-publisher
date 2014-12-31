package nl.idgis.publisher.function;

@FunctionalInterface
public interface Procedure2<T, U> {

	void apply(T t, U u) throws Throwable;
}
