package nl.idgis.publisher.utils;

import java.util.function.Supplier;

public class Lazy<T> {
	
	private T t;
	
	private final Supplier<T> supplier;
	
	public Lazy(Supplier<T> supplier) {
		this.supplier = supplier;
	}

	public synchronized T get() {
		if(t == null) {
			t = supplier.get();
		}
		
		return t;
	}
}
