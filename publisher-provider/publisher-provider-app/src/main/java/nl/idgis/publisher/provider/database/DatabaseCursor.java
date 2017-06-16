package nl.idgis.publisher.provider.database;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.apache.commons.io.IOUtils;

import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.provider.database.messages.DatabaseColumnInfo;
import nl.idgis.publisher.provider.database.messages.FetchTable;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;
import nl.idgis.publisher.provider.protocol.WKBGeometry;
import nl.idgis.publisher.stream.StreamCursor;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import oracle.sql.STRUCT;

import org.deegree.geometry.io.WKBWriter;
import org.deegree.sqldialect.oracle.sdo.SDOGeometryConverter;

public class DatabaseCursor extends StreamCursor<ResultSet, Records> {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final SDOGeometryConverter converter = new SDOGeometryConverter();
		
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
	
	private Object convert(DatabaseColumnInfo columnInfo, Object value) throws Exception {
		if(value == null) {
			return null;
		}
		
		if(columnInfo.getType() == Type.GEOMETRY) {
			String typeName = columnInfo.getTypeName();
			if("SDO_GEOMETRY".equals(typeName)) {
				if(value instanceof STRUCT) {
					STRUCT struct = (STRUCT)value;
					return new WKBGeometry(WKBWriter.write(converter.toGeometry(struct, null)));
				} else {
					log.error("unsupported value class: {}", value.getClass().getCanonicalName());
					return null;
				}
			} else if("ST_GEOMETRY".equals(typeName)) {
				if(value instanceof Blob) {
					Blob blob = (Blob)value;
					long blobLength = blob.length();
					if(blobLength > Integer.MAX_VALUE) {
						log.error("blob value too large: {}", blobLength);
						return null;
					} if(blobLength == 0) { // known to be returned by SDE.ST_ASBINARY on empty geometries
						log.error("empty blob");
						return null;
					} else {
						return new WKBGeometry(blob.getBytes(1l, (int)blobLength));
					}
				} else {
					log.error("unsupported value class: {}", value.getClass().getCanonicalName());
					return null;
				}
			} else {
				log.error("unsupported geometry type: {}", typeName);
				return null;
			}
		} else {
			if(value instanceof Clob) {
				Clob clob = (Clob)value;
				if(clob.length() > Integer.MAX_VALUE) {
					log.error("clob value too large: {}", clob.length());
					return null;
				}
				
				return IOUtils.toString(clob.getCharacterStream());
			}
			
			return value;
		}
	}
	
	private Record toRecord() throws Exception {
		currentHasNext = null;
		
		List<Object> values = new ArrayList<>();
		
		int j = 1;
		for(DatabaseColumnInfo columnInfo : fetchTable.getColumns()) {
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
	}
}
