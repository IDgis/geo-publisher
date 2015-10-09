package nl.idgis.publisher.database;

import com.mysema.query.types.Expression;

public class EmptyTuple extends AbstractTuple {
	
	private final int size;
	
	public EmptyTuple(int size) {
		this.size = size;
	}

	@Override
	public <T> T get(int index, Class<T> type) {
		return null;
	}

	@Override
	public <T> T get(Expression<T> expr) {
		return null;
	}

	@Override
	public int size() {		
		return size;
	}

	@Override
	public Object[] toArray() {
		return new Object[size];
	}
}
