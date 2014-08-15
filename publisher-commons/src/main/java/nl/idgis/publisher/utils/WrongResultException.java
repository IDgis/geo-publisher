package nl.idgis.publisher.utils;

public class WrongResultException extends Exception {

	private static final long serialVersionUID = 8448858984132800140L;
	
	private final Object result, context;
	private final Class<?> expected;

	public WrongResultException(Object result, Class<?> expected, Object context) {
		this.result = result;
		this.expected = expected;
		this.context = context;
	}

	public Object getResult() {
		return result;
	}

	public Class<?> getExpected() {
		return expected;
	}
	
	public Object getContext() {
		return context;
	}

	@Override
	public String toString() {
		return "WrongResultException [result=" + result + ", context="
				+ context + ", expected=" + expected + "]";
	}

}
