package nl.idgis.publisher.provider.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import scala.concurrent.Future;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.japi.Function2;
import akka.pattern.Patterns;
import nl.idgis.publisher.protocol.Failure;
import nl.idgis.publisher.protocol.stream.StreamCursor;
import nl.idgis.publisher.provider.database.messages.Convert;
import nl.idgis.publisher.provider.database.messages.Converted;
import nl.idgis.publisher.provider.protocol.database.Record;

public class DatabaseCursor extends StreamCursor<ResultSet, Record> {
	
	private final ActorRef converter;

	public DatabaseCursor(ResultSet t, ActorRef converter) {
		super(t);
		
		this.converter = converter;
	}
	
	public static Props props(ResultSet t, ActorRef converter) {
		return Props.create(DatabaseCursor.class, t, converter);
	}

	@Override
	protected Future<Record> next() {
		try {
			int columnCount = t.getMetaData().getColumnCount();
			
			List<Future<Object>> valueFutures = new ArrayList<>();
			for(int i = 0; i < columnCount; i++) {
				Object o = t.getObject(i + 1);
				
				if(o == null) {
					valueFutures.add(Futures.successful(null));
				} else {
					valueFutures.add(Patterns.ask(converter, new Convert(o), 1000));
				}
			}
			
			return Futures.fold(new ArrayList<Object>(), valueFutures, new Function2<List<Object>, Object, List<Object>>() {

				@Override
				public List<Object> apply(List<Object> values, Object value) throws Exception {
					if(value instanceof Converted) {
						values.add(((Converted) value).getValue());
					} else if(value instanceof Failure) {
						throw new Exception(((Failure) value).getCause());
					}
					
					return values;
				}				
			}, getContext().dispatcher()).map(new Mapper<List<Object>, Record>() {
				
				@Override
				public Record apply(List<Object> values) {
					return new Record(values);
				}
			},  getContext().dispatcher());
		} catch(Throwable t) {
			return Futures.failed(t);
		}
	}

	@Override
	protected boolean hasNext() throws Exception {		
		return t.next();
	}
	
	@Override
	public void postStop() throws SQLException {
		t.getStatement().close();
		t.close();
	}
}
