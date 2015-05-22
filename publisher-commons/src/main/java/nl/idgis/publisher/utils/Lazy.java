package nl.idgis.publisher.utils;

import java.io.Serializable;
import java.util.function.Supplier;

public final class Lazy<T> implements Serializable {	

	public static interface LazySupplier<T> extends Supplier<T>, Serializable {
		
	}
	
	private static final long serialVersionUID = -8261891546560310755L;

	private transient T t;
	
	private final LazySupplier<T> supplier;
	
	public Lazy(LazySupplier<T> supplier) {
		this.supplier = supplier;
	}

	public synchronized T get() {
		if(t == null) {
			t = supplier.get();
		}
		
		return t;
	}
}
