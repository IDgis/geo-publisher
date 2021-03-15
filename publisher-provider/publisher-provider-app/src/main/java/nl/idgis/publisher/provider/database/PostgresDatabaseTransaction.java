package nl.idgis.publisher.provider.database;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.Config;
import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.provider.database.messages.*;
import nl.idgis.publisher.utils.UniqueNameGenerator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class PostgresDatabaseTransaction extends AbstractDatabaseTransaction {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();

	public PostgresDatabaseTransaction(Config config, Connection connection) {
		super(config, connection);
	}

	public static Props props(Config config, Connection connection) {
		return Props.create(PostgresDatabaseTransaction.class, config, connection);
	}

	@Override
	Object handleDescribeTable(DescribeTable query) throws SQLException {
		log.debug("Describing table");

		String schema = query.getScheme();
		String tableName = query.getTableName();

		if (schema==null) {
			log.warning("No scheme defined in metadata");
			return new TableNotFound();
		} else {
			// https://stackoverflow.com/questions/20194806/how-to-get-a-list-column-names-and-datatype-of-a-table-in-postgresql
			String sql = "SELECT pg_attribute.attname AS column_name, pg_catalog.format_type(pg_attribute.atttypid, pg_attribute.atttypmod) AS data_type "
					+ "FROM pg_catalog.pg_attribute "
					+ "INNER JOIN pg_catalog.pg_class ON pg_class.oid = pg_attribute.attrelid "
					+ "INNER JOIN pg_catalog.pg_namespace ON pg_namespace.oid = pg_class.relnamespace "
					+ "WHERE pg_attribute.attnum > 0 AND NOT pg_attribute.attisdropped AND pg_namespace.nspname = '" + schema + "' AND pg_class.relname = '" + tableName + "' "
					+ "ORDER BY attnum";


			log.debug("executing data dictionary query: {}", sql);

			ArrayList<AbstractDatabaseColumnInfo> columns = new ArrayList<>();
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				String name = rs.getString(1);
				String typeName = rs.getString(2);

				AbstractDatabaseColumnInfo columnInfo = new PostgresDatabaseColumnInfo(name, typeName);
				// not reporting columns with unsupported data types
				if ("CLOB".equals(typeName) || columnInfo.getType() == null) {
					log.debug("unsupported data type: " + columnInfo.getTypeName() + " in column: " + name);
				} else {
					log.debug("Found column: " + name + " with data type: " + columnInfo.getTypeName() + ". Converted to: " + columnInfo.getType());
					columns.add(columnInfo);
				}
			}

			rs.close();
			stmt.close();

			if (columns.isEmpty()) {
				return new TableNotFound();
			} else {
				return new DatabaseTableInfo(columns.toArray(new AbstractDatabaseColumnInfo[0]));
			}
		}
	}

	@Override
	Object handlePerformCount(PerformCount query) throws SQLException {

		String schema = query.getScheme();
		String tableName = query.getTableName();

		String sql;
		if (schema==null) {
			sql  = "SELECT count(*) FROM \"" + tableName + "\"";
		} else {
			sql  = "SELECT count(*) FROM \"" + schema + "\".\"" + tableName + "\"";
		}

		log.debug(String.format("executing count query: %s", sql));
		Statement stmt = connection.createStatement();
		
		ResultSet rs = stmt.executeQuery(sql);
		rs.next();
		
		Object retval = rs.getLong(1);
		
		rs.close();		
		stmt.close();	
		
		return retval;
	}

	@Override
	ActorRef handleFetchTable(FetchTable msg) throws SQLException {
		log.debug("Fetch table: " + msg);
		log.debug("Table scheme: " + msg.getScheme()==null ? "No scheme defined" : msg.getScheme());
		log.debug("Table name: " + msg.getTableName());
		
		StringBuilder sb = new StringBuilder("SELECT ");
		
		String separator = "";
		for(AbstractDatabaseColumnInfo columnInfo : msg.getColumns()) {
			sb.append(separator);

			String typeName = columnInfo.getTypeName();
			String columnName = columnInfo.getName();
			Type type = columnInfo.getType();

			log.debug("Column Info: ");
			log.debug("name: " + columnName);
			log.debug("typeName: " + typeName);
			log.debug("type: " + type.toString());

			if ("GEOMETRY".equals(typeName)) {
				sb.append("ST_AsBinary(\"").append(columnName).append("\") AS \"").append(columnName).append("\"");
			} else {
				sb.append("\"").append(columnName).append("\"");
			}
			
			separator = ", ";
		}
		log.debug("Maken FROM deel");
		sb.append(" FROM \"");

		String schema = msg.getScheme();
		String tableName = msg.getTableName();

		if(schema==null) {
			sb.append(tableName);
		} else {
			sb
				.append(schema)
				.append("\".\"")
				.append(tableName);
		}
		
		sb.append("\" AS T");
		log.debug("Maken WHERE deel");
		
		msg.getFilter().ifPresent(filter -> {
			sb.append(" WHERE ");
			writeFilter(filter, sb);
		});
		
		String query = sb.toString();
		
		log.debug("Executing fetchtable query: " + query);
		
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery(query);

		return getContext().actorOf(
				PostgresDatabaseCursor.props(rs, msg, executorService),
				nameGenerator.getName(PostgresDatabaseCursor.class));
	}
}
