package nl.idgis.publisher.provider.database;

import nl.idgis.publisher.protocol.database.UnsupportedType;
import nl.idgis.publisher.provider.database.messages.Convert;

import akka.actor.Props;
import akka.actor.UntypedActor;

public class DatabaseConverter extends UntypedActor {
	
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
		if(msg instanceof Convert) {
			Convert convert = (Convert)msg;
			getSender().tell(convert(convert.getValue()), getSelf());
		} else {
			unhandled(msg);
		}
	}
}
