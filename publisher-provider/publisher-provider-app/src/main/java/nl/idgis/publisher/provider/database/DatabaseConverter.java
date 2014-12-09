package nl.idgis.publisher.provider.database;

import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.provider.database.messages.ConvertValue;
import nl.idgis.publisher.provider.database.messages.ConvertedValue;
import nl.idgis.publisher.provider.protocol.UnsupportedType;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class DatabaseConverter extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private static final Class<?>[] NO_CONVERSION_NEEDED = 
			new Class<?>[]{String.class, Number.class};
	
	protected Object convert(Object value) throws Exception {
		for(Class<?> clazz : NO_CONVERSION_NEEDED) {
			if(clazz.isInstance(value)) {				
				return value;
			}
		}
		
		return new UnsupportedType(value.getClass().getCanonicalName());
	}
	
	public static Props props() {
		return Props.create(DatabaseConverter.class);
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		if(msg instanceof ConvertValue) {
			ConvertValue convert = (ConvertValue)msg;
			
			Object response;
			try {
				response = new ConvertedValue(convert(convert.getValue()));
			} catch(Exception e) {
				log.warning("couldn't convert value");
				response = new Failure(e);
			}
			
			getSender().tell(response, getSelf());
		} else {
			unhandled(msg);
		}
	}
}
