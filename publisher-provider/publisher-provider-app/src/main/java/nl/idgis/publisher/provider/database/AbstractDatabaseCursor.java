package nl.idgis.publisher.provider.database;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import nl.idgis.publisher.provider.database.messages.AbstractDatabaseColumnInfo;
import nl.idgis.publisher.provider.database.messages.FetchTable;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;
import nl.idgis.publisher.stream.StreamCursor;
import org.deegree.sqldialect.oracle.sdo.SDOGeometryConverter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class AbstractDatabaseCursor extends StreamCursor<ResultSet, Records> {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final SDOGeometryConverter converter = new SDOGeometryConverter();

	private final FetchTable fetchTable;

	private final ExecutorService executorService;

	private Boolean currentHasNext = null;

	public AbstractDatabaseCursor(ResultSet t, FetchTable fetchTable, ExecutorService executorService) {
		super(t);
		
		this.fetchTable = fetchTable;
		this.executorService = executorService;
	}
	
	public static Props props(ResultSet t, FetchTable fetchTable, ExecutorService executorService) {
		return Props.create(AbstractDatabaseCursor.class, t, fetchTable, executorService);
	}

	Object convert(AbstractDatabaseColumnInfo columnInfo, Object value) throws Exception {
		if(value == null) {
			return null;
		} else {
			return value;
		}
	}

	protected Record toRecord() throws Exception {
		currentHasNext = null;
		
		List<Object> values = new ArrayList<>();
		
		int j = 1;
		for(AbstractDatabaseColumnInfo columnInfo : fetchTable.getColumns()) {
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
				log.error(t, "failed to fetch records");
				
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
		
		log.debug("resultset is closed");
	}
}
