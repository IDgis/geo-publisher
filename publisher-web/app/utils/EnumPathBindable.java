package utils;

import play.mvc.PathBindable;

public abstract class EnumPathBindable<T extends Enum<T>, U extends EnumPathBindable<T, U>> implements PathBindable<U> {
	
	public final T value;
	
	private final Class<T> t;
			
	protected EnumPathBindable(T value, Class<T> t) {
		this.value = value;
		this.t = t;
	}
	
	protected abstract U valueOf(T t);

	@Override
	public U bind(String key, String value) {
		return valueOf(Enum.valueOf(t, value.toUpperCase().replace("-", "_")));
	}

	@Override
	public String javascriptUnbind() {
		return null;
	}

	@Override
	public String unbind(String key) {
		return value.name().toLowerCase().replace("_", "-");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		@SuppressWarnings("rawtypes")
		EnumPathBindable other = (EnumPathBindable) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	
	
	
}