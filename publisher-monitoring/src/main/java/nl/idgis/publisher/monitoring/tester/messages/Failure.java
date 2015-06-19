package nl.idgis.publisher.monitoring.tester.messages;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import nl.idgis.publisher.utils.Either;

public class Failure extends Result {
	
	private static final long serialVersionUID = 2462298561529402099L;
	
	private Either<Throwable, String> cause;
	
	public Failure(URL url, Either<Throwable, String> cause) {
		super(url);
		
		this.cause = Objects.requireNonNull(cause);
	}
	
	public Failure(URL url, String message) {
		this(url, Either.right(message));
	}

	public Failure(URL url, Throwable t) {
		this(url, Either.left(t));
	}
	
	public Optional<Throwable> getThrowable() {
		return cause.getLeft();
	}
	
	public String getMessage() {
		return cause.mapLeft(Throwable::getMessage);
	}

	@Override
	public String toString() {
		return "Failure [cause=" + cause + ", message=" + getMessage() + ", url="
				+ url + "]";
	}
}
