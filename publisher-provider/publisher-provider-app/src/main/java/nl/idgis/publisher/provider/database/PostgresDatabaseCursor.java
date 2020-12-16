package nl.idgis.publisher.provider.database;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.provider.database.messages.AbstractDatabaseColumnInfo;
import nl.idgis.publisher.provider.database.messages.FetchTable;
import nl.idgis.publisher.provider.protocol.WKBGeometry;
import org.apache.commons.io.IOUtils;
import org.postgresql.util.PGobject;

import java.sql.*;
import java.util.concurrent.ExecutorService;

public class PostgresDatabaseCursor extends AbstractDatabaseCursor {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

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
					//Timestamp timestamp = (Timestamp) value;
					//Date newDate = new Date(timestamp.getTime());
					//log.debug(String.format("Converting a timestamp to a date value: %s", newDate.toString()));
					//return newDate;
					log.debug("Found a timestamp. Looks fine");
					return value;
				} else {
					log.error("unsupported value class: {}", value.getClass().getCanonicalName());
					return null;
				}
			}
			return value;
		} else if (columnType == Type.GEOMETRY) {
			if (value instanceof PGobject) {
				log.debug("Value is a PostGis EWKB geometry. Converting to WKB");

				PGobject pgobject = (PGobject) value;
				return new WKBGeometry(pgobject.getValue().getBytes());
			} else {
				return null;
			}
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