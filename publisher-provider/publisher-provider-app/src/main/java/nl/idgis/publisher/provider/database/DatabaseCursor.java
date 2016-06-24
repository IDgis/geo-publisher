package nl.idgis.publisher.provider.database;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;


import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.provider.database.messages.FetchTable;
import nl.idgis.publisher.provider.protocol.ColumnInfo;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;
import nl.idgis.publisher.provider.protocol.WKBGeometry;
import nl.idgis.publisher.stream.StreamCursor;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class DatabaseCursor extends StreamCursor<ResultSet, Records> {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
	private final FetchTable fetchTable;
	
	private final ExecutorService executorService;
	
	private Boolean currentHasNext = null;

	public DatabaseCursor(ResultSet t, FetchTable fetchTable, ExecutorService executorService) {
		super(t);
		
		this.fetchTable = fetchTable;
		this.executorService = executorService;
	}
	
	public static Props props(ResultSet t, FetchTable fetchTable, ExecutorService executorService) {
		return Props.create(DatabaseCursor.class, t, fetchTable, executorService);
	}
	
	private Object convert(ColumnInfo columnInfo, Object value) throws Exception {
		if(columnInfo.getType() == Type.GEOMETRY) {
			if(value instanceof Blob) {
				Blob blob = (Blob)value;
				if(blob.length() > Integer.MAX_VALUE) {
					log.error("blob value too large: {}", blob.length());
					return null;
				} else {
					return new WKBGeometry(blob.getBytes(1l, (int)blob.length()));
				}
			} else {
				log.error("unsupported value: {}", value.getClass().getCanonicalName());
				return null;
			}
		} else {
			return value;
		}
	}
	
	private Record toRecord() throws Exception {
		currentHasNext = null;
		
		List<Object> values = new ArrayList<>();
		
		int j = 1;
		for(ColumnInfo columnInfo : fetchTable.getColumnInfos()) {
			Object value = t.getObject(j++);
			values.add(convert(columnInfo, value));
		}
		
		return new Record(values);
	}

	@Override
	protected CompletableFuture<Records> next() {
		log.debug("fetching next records");
		
		CompletableFuture<Records> future = new CompletableFuture<>();
		
		int messageSize = fetchTable.getMessageSize();
		executorService.execute(() -> {
			try {
				List<Record> records = new ArrayList<>();
				
				for(int i = 0; i < messageSize; i++) {
					if(!hasNext()) {
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
		if(currentHasNext == null) {
			currentHasNext = t.next();
		}
		
		return currentHasNext;
	}
	
	@Override
	public void postStop() throws SQLException {
		t.getStatement().close();
		t.close();
	}
}
