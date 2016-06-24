package nl.idgis.publisher.provider.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import oracle.sql.STRUCT;

import org.deegree.geometry.io.WKBWriter;
import org.deegree.sqldialect.oracle.sdo.SDOGeometryConverter;

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

@SuppressWarnings("deprecation")
public class DatabaseCursor extends StreamCursor<ResultSet, Records> {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final SDOGeometryConverter converter = new SDOGeometryConverter();
	
	private final FetchTable fetchTable;
	
	private final ExecutorService executorService;

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
			return new WKBGeometry(WKBWriter.write(converter.toGeometry((STRUCT) value, null)));
		} else {
			return value;
		}
	}
	
	private Record toRecord() throws Exception {
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
