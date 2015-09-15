package nl.idgis.publisher.database;

import com.mysema.query.Tuple;
import com.mysema.query.types.Expression;

public class JoinedTuple extends AbstractTuple {
	
	private final Tuple left;
	
	private final Tuple right;
	
	public JoinedTuple(Tuple left, Tuple right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public <T> T get(int index, Class<T> type) {
		int leftSize = left.size();
		
		if(index >= leftSize) {
			return right.get(index - leftSize, type);
		}
		
		return left.get(index, type);
	}

	@Override
	public <T> T get(Expression<T> expr) {
		T retval = right.get(expr);
		if(retval == null) {
			return left.get(expr);
		}
		
		return retval;
	}

	@Override
	public int size() {
		return left.size() + right.size();
	}

	@Override
	public Object[] toArray() {
		Object[] retval = new Object[size()];
		
		Object[] leftArray = left.toArray();
		System.arraycopy(leftArray, 0, retval, 0, leftArray.length);
		
		Object[] rightArray = right.toArray();
		System.arraycopy(rightArray, 0, retval, leftArray.length, rightArray.length);
		
		return retval;
	}

}
