package nl.idgis.publisher.utils;

import java.util.Optional;
import java.util.function.Function;

public class Either<T, U> {

	private final T t;
	
	private final U u;
	
	private Either(T t, U u) {
		this.t = t;
		this.u = u;
	}
	
	public static <T, U> Either<T, U> left(T t) {
		return new Either<>(t, null);
	}
	
	public static <T, U> Either<T, U> right(U u) {
		return new Either<>(null, u);
	}
	
	public Optional<T> getLeft() {				
		return Optional.ofNullable(t);
	}
	
	public Optional<U> getRight() {			
		return Optional.ofNullable(u);
	}
	
	public U mapLeft(Function<? super T, ? extends U> mapper) {
		if(u == null) {
			return mapper.apply(t);
		} else {
			return u;
		}
	}
	
	public T mapRight(Function<? super U, ? extends T> mapper) {
		if(t == null) {
			return mapper.apply(u);
		} else {
			return t;
		}
	}
	
	public Either<U, T> swap() {
		return new Either<>(u, t);
	}
}
