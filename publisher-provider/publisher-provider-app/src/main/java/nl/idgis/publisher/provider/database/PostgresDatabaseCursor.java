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
					log.debug("Found a timestamp. Looks fine");
					return value;
				} else {
					log.error("unsupported DATE class: {}", value.getClass().getCanonicalName());
					return null;
				}
			}
			return value;
		} else if (columnType == Type.GEOMETRY) {
			if (value instanceof PGobject) {
				log.warning("Value is a PostGis PGobject. Skipping...");
				return null;
			} else if (value instanceof byte[]) {
				log.debug("Value is a byte[]. Converting to WKBGeometry");
				byte[] newValue = (byte[]) value;
				return new WKBGeometry(newValue);
			} else {
				log.error("unsupported GEOMETRY class: {}", value.getClass().getCanonicalName());
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