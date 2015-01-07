package nl.idgis.publisher.utils;

public class WrongResultException extends Exception {
	
	private static final long serialVersionUID = 8448858984132800140L;

	private final Object result;
	
	private final Class<?> expected;

	public WrongResultException(Object result, Class<?> expected) {
		this.result = result;
		this.expected = expected;		
	}

	public Object getResult() {
		return result;
	}

	public Class<?> getExpected() {
		return expected;
	}

	@Override
	public String toString() {
		return "WrongResultException [result=" + result + ", expected="
				+ expected + "]";
	}
}
