package nl.idgis.publisher.provider.database;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import oracle.sql.STRUCT;

import org.deegree.geometry.io.WKBWriter;
import org.deegree.sqldialect.oracle.sdo.SDOGeometryConverter;

import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;
import nl.idgis.publisher.provider.protocol.UnsupportedType;
import nl.idgis.publisher.provider.protocol.WKBGeometry;
import nl.idgis.publisher.stream.StreamCursor;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

@SuppressWarnings("deprecation")
public class DatabaseCursor extends StreamCursor<ResultSet, Records> {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final SDOGeometryConverter converter = new SDOGeometryConverter();
	
	private final int messageSize;
	
	private final ExecutorService executorService;

	public DatabaseCursor(ResultSet t, int messageSize, ExecutorService executorService) {
		super(t);
		
		this.messageSize = messageSize;
		this.executorService = executorService;
	}
	
	public static Props props(ResultSet t, int messageSize, ExecutorService executorService) {
		return Props.create(DatabaseCursor.class, t, messageSize, executorService);
	}
	
	private Object convert(Object value) throws Exception {
		if(value == null 
				|| value instanceof String
				|| value instanceof Number
				|| value instanceof Date
				|| value instanceof Timestamp) {
			
			return value;
		} else if(value instanceof STRUCT) {
			return new WKBGeometry(WKBWriter.write(converter.toGeometry((STRUCT) value, null)));
		}
		
		return new UnsupportedType(value.getClass().getCanonicalName());
	}
	
	private Record toRecord() throws Exception {
		int columnCount = t.getMetaData().getColumnCount();
		
		List<Object> values = new ArrayList<>();
		for(int j = 0; j < columnCount; j++) {
			Object o = t.getObject(j + 1);
			values.add(convert(o));
		}
		
		return new Record(values);
	}

	@Override
	protected CompletableFuture<Records> next() {
		log.debug("fetching next records");
		
		CompletableFuture<Records> future = new CompletableFuture<>();
		
		executorService.execute(() -> {
			try {
				List<Record> records = new ArrayList<>();
				records.add(toRecord());
				
				for(int i = 1; i < messageSize; i++) {
					if(!t.next()) {
						break;
					}
					
					records.add(toRecord());
				}
				
				future.complete(new Records(records));
			} catch(Throwable t) {
				log.error("failed to fetch records: {}", t);
				
				future.completeExceptionally(t);
			}
		});
		
		return future;
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
