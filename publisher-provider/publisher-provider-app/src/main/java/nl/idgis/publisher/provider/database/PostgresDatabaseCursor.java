package nl.idgis.publisher.provider.database;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.provider.database.messages.AbstractDatabaseColumnInfo;
import nl.idgis.publisher.provider.database.messages.FetchTable;
import org.apache.commons.io.IOUtils;
import org.deegree.sqldialect.oracle.sdo.SDOGeometryConverter;

import java.sql.*;
import java.util.concurrent.ExecutorService;

public class PostgresDatabaseCursor extends AbstractDatabaseCursor {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final SDOGeometryConverter converter = new SDOGeometryConverter();

	public PostgresDatabaseCursor(ResultSet t, FetchTable fetchTable, ExecutorService executorService) {
		super(t, fetchTable, executorService);
	}

	public static Props props(ResultSet t, FetchTable fetchTable, ExecutorService executorService) {
		return Props.create(PostgresDatabaseCursor.class, t, fetchTable, executorService);
	}

	Object convert(AbstractDatabaseColumnInfo columnInfo, Object value) throws Exception {
		if (value == null) {
			return null;
		}

		Type columnType = columnInfo.getType();
		String typeName = columnInfo.getTypeName();
		log.debug(String.format("database column typeName, Type and value: %s, %s, %s", typeName, columnType, value));

		if (columnType == Type.DATE) {
			if ("TIMESTAMP WITHOUT TIME ZONE".equalsIgnoreCase(typeName)) {
				if (value instanceof Timestamp) {
					Timestamp timestamp = (Timestamp) value;
					Date newDate = new Date(timestamp.getTime());
					log.debug(String.format("Converting a timestamp to a date value: %s", newDate.toString()));
					return newDate;
				}
			}
			/*
			if ("TIMESTAMP(6)".equals(typeName)) {
				log.debug("database column value is of type timestamp(6)");
				if (value instanceof TIMESTAMP) {
					log.debug("database column value is of type oracle.sql.timestamp");

					TIMESTAMP timestamp = (TIMESTAMP) value;
					return timestamp.timestampValue();
				} else {
					log.error("unsupported value class: {}", value.getClass().getCanonicalName());
					return null;
				}
			} else {
				return value;
			}
			*/

			return value;
		} else if (columnType == Type.GEOMETRY) {
			/*
			if ("SDO_GEOMETRY".equals(typeName)) {
				log.debug("database column value is of type sdo_geometry");

				if (value instanceof STRUCT) {
					log.debug("database column value is of type struct");

					STRUCT struct = (STRUCT) value;
					log.debug("struct object: " + struct);

					// De converter is een specifieke ORACLE converter.
					// Hoe krijg ik hier een Postgis converter
					Geometry geom = converter.toGeometry(struct, null);
					log.debug("geom object: " + geom);

					return new WKBGeometry(WKBWriter.write(geom));
				} else {
					log.error("unsupported value class: {}", value.getClass().getCanonicalName());
					return null;
				}
			} else if ("ST_GEOMETRY".equals(typeName)) {
				log.debug("database column value is of type st_geometry");

				if (value instanceof Blob) {
					Blob blob = (Blob) value;
					long blobLength = blob.length();
					if (blobLength > Integer.MAX_VALUE) {
						log.error("blob value too large: {}", blobLength);
						return null;
					}
					if (blobLength == 0) { // known to be returned by SDE.ST_ASBINARY on empty geometries
						log.error("empty blob");
						return null;
					} else {
						return new WKBGeometry(blob.getBytes(1l, (int) blobLength));
					}
				} else {
					log.error("unsupported value class: {}", value.getClass().getCanonicalName());
					return null;
				}
			} else {
				log.error("unsupported geometry type: {}", typeName);
				return null;
			}*/
			return value;
		} else {
			if (value instanceof Clob) {
				Clob clob = (Clob) value;
				if (clob.length() > Integer.MAX_VALUE) {
					log.error("clob value too large: {}", clob.length());
					return null;
				}

				return IOUtils.toString(clob.getCharacterStream());
			}

			return value;
		}
	}
}