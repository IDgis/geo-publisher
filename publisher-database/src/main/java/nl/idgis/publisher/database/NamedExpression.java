package nl.idgis.publisher.database;

import com.mysema.query.types.Expression;
import com.mysema.query.types.Visitor;

public final class NamedExpression<T> implements Expression<T> {
	
	private static final long serialVersionUID = -4459806672038560682L;
		
	private final String name;
	
	public NamedExpression(String name) {
		this.name = name;
	}

	@Override
	public <R, C> R accept(Visitor<R, C> v, C context) {
		throw new RuntimeException("NamedExpression does not accept visitors");
	}

	@Override
	public Class<? extends T> getType() {
		throw new RuntimeException("NamedExpression not associated to a type");		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NamedExpression<?> other = (NamedExpression<?>) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	
}