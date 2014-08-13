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
import akka.dispatch.OnFailure;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function2;
import akka.pattern.Patterns;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.provider.database.messages.Convert;
import nl.idgis.publisher.provider.database.messages.Converted;
import nl.idgis.publisher.provider.protocol.database.Record;
import nl.idgis.publisher.provider.protocol.database.Records;
import nl.idgis.publisher.stream.StreamCursor;

public class DatabaseCursor extends StreamCursor<ResultSet, Records> {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef converter;
	private final int messageSize;

	public DatabaseCursor(ResultSet t, int messageSize, ActorRef converter) {
		super(t);
		
		this.converter = converter;
		this.messageSize = messageSize;
	}
	
	public static Props props(ResultSet t, int messageSize, ActorRef converter) {
		return Props.create(DatabaseCursor.class, t, messageSize, converter);
	}
	
	private Future<Record> toRecord() throws SQLException {
		int columnCount = t.getMetaData().getColumnCount();
		List<Future<Object>> valueFutures = new ArrayList<>();
		
		for(int j = 0; j < columnCount; j++) {
			Object o = t.getObject(j + 1);
			
			if(o == null) {
				valueFutures.add(Futures.successful(null));
			} else {
				Future<Object> future = Patterns.ask(converter, new Convert(o), 15000);
				future.onFailure(new OnFailure() {

					@Override
					public void onFailure(Throwable t) throws Throwable {
						log.error(t, "conversion failure");
					}					
				}, getContext().dispatcher());
				valueFutures.add(future);
			}
		}
		
		return Futures.fold(new ArrayList<Object>(), valueFutures, new Function2<List<Object>, Object, List<Object>>() {

				@Override
				public List<Object> apply(List<Object> values, Object value) throws Exception {
					if(value instanceof Converted) {
						values.add(((Converted) value).getValue());
					} else if(value instanceof Failure) {
						Throwable cause = ((Failure) value).getCause();
						
						log.error(cause, "failed to convert value");								
						throw new Exception(cause);
					}
					
					return values;
				}				
			}, getContext().dispatcher()).map(new Mapper<List<Object>, Record>() {
				
				@Override
				public Record apply(List<Object> values) {
					return new Record(values);
				}
			}, getContext().dispatcher());
	}

	@Override
	protected Future<Records> next() {
		log.debug("fetching next records");
		
		try {
			List<Future<Record>> recordFutures = new ArrayList<>();
			recordFutures.add(toRecord());
			
			for(int i = 1; i < messageSize; i++) {
				if(!t.next()) {
					break;
				}
				
				recordFutures.add(toRecord());
			}
			
			Future<Records> records = Futures.fold(new ArrayList<Record>(), recordFutures, new Function2<List<Record>, Record, List<Record>>() {

				@Override
				public List<Record> apply(List<Record> records, Record record) throws Exception {
					records.add(record);					
					
					return records;
				}				
			}, getContext().dispatcher()).map(new Mapper<List<Record>, Records>() {
				
				@Override
				public Records apply(List<Record> records) {
					return new Records(records);
				}				
			}, getContext().dispatcher());
			
			log.debug("records future created");
			return records;
		} catch(Throwable t) {
			log.error(t, "failed to fetch records");			
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
