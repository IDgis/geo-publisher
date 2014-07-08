package nl.idgis.publisher.utils;

import akka.dispatch.OnComplete;
import akka.event.LoggingAdapter;

public abstract class OnReceive<T> extends OnComplete<Object> {
	
	private final LoggingAdapter log;
	private final Class<T> clazz;
	
	public OnReceive(LoggingAdapter log, Class<T> clazz) {
		this.log = log;
		this.clazz = clazz;
	}

	@Override
	public void onComplete(Throwable t, Object o) throws Throwable {
		if(t != null) {
			log.error(t, "couldn't receive answer");
		} else {					
			if(clazz.isInstance(o)) {
				onReceive(clazz.cast(o));
			} else {
				log.error("expected " + clazz.getCanonicalName() + ", received: " + o);
			}
		}
	}
	
	protected abstract void onReceive(T t);
}
