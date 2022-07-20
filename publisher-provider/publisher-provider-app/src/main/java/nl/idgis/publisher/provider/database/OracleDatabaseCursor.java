package nl.idgis.publisher.provider.database;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;

import org.apache.commons.io.IOUtils;

import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.provider.database.messages.AbstractDatabaseColumnInfo;
import nl.idgis.publisher.provider.database.messages.FetchTable;
import nl.idgis.publisher.provider.protocol.WKBGeometry;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import oracle.sql.STRUCT;
import oracle.sql.TIMESTAMP;

import org.deegree.geometry.Geometry;
import org.deegree.geometry.io.WKBWriter;
import org.deegree.sqldialect.oracle.sdo.SDOGeometryConverter;

public class OracleDatabaseCursor extends AbstractDatabaseCursor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final SDOGeometryConverter converter = new SDOGeometryConverter();

	public OracleDatabaseCursor(ResultSet t, FetchTable fetchTable, ExecutorService executorService) {
		super(t, fetchTable, executorService);
	}
	
	public static Props props(ResultSet t, FetchTable fetchTable, ExecutorService executorService) {
		return Props.create(OracleDatabaseCursor.class, t, fetchTable, executorService);
	}

	@Override
	Object convert(AbstractDatabaseColumnInfo columnInfo, Object value) throws Exception {
		if(value == null) {
			return null;
		}

		String className = value.getClass().getCanonicalName();
		log.debug("database column value: {}, class: {}", value, className);
		
		if(columnInfo.getType() == Type.TIMESTAMP) {
			if(value instanceof TIMESTAMP) {
				log.debug("database column value is of type oracle.sql.TIMESTAMP");

				TIMESTAMP timestamp = (TIMESTAMP)value;
				return timestamp.timestampValue();
			} else {
				log.error("unsupported timestamp value class: {}", className);
				return null;
			}
		} else if(columnInfo.getType() == Type.GEOMETRY) {
			String typeName = columnInfo.getTypeName();
			if("SDO_GEOMETRY".equals(typeName)) {
				log.debug("database column value is of type sdo_geometry");
				
				if(value instanceof STRUCT) {
					log.debug("database column value is of type struct");
					
					STRUCT struct = (STRUCT)value;
					log.debug("struct object: " + struct);
					
					Geometry geom = converter.toGeometry(struct, null);
					log.debug("geom object: " + geom);
					
					return new WKBGeometry(WKBWriter.write(geom));
				} else {
					log.error("unsupported geometry value class: {}", className);
					return null;
				}
			} else if("ST_GEOMETRY".equals(typeName)) {
				log.debug("database column value is of type st_geometry");
				
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
					log.error("unsupported blob value class: {}", className);
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
}
