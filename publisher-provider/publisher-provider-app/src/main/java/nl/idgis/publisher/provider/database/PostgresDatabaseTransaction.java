package nl.idgis.publisher.provider.database;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.Config;
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
		String requestedTableName = query.getTableName();
		
		final String sql;
		int separatorIndex = requestedTableName.indexOf(".");
		if(separatorIndex == -1) {

			// Er wordt geen gebruik gemaakt van schema.table notatie in metadata bestand
			log.warning("Schema-table name not clear from database");
			return new TableNotFound();

		} else {
			String schema = requestedTableName.substring(0, separatorIndex);
			String tableName = requestedTableName.substring(separatorIndex + 1);

			// https://stackoverflow.com/questions/20194806/how-to-get-a-list-column-names-and-datatype-of-a-table-in-postgresql
			sql = "SELECT pg_attribute.attname AS column_name, pg_catalog.format_type(pg_attribute.atttypid, pg_attribute.atttypmod) AS data_type "
				+ "FROM pg_catalog.pg_attribute "
				+ "INNER JOIN pg_catalog.pg_class ON pg_class.oid = pg_attribute.attrelid "
				+ "INNER JOIN pg_catalog.pg_namespace ON pg_namespace.oid = pg_class.relnamespace "
				+ "WHERE pg_attribute.attnum > 0 AND NOT pg_attribute.attisdropped AND pg_namespace.nspname = '" + schema + "' AND pg_class.relname = '" + tableName + "' "
				+ "ORDER BY attnum";
		}
		
		log.debug("executing data dictionary query: {}", sql);
		
		ArrayList<AbstractDatabaseColumnInfo> columns = new ArrayList<>();
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		while(rs.next()) {
			String name = rs.getString(1);
			String typeName = rs.getString(2);
			
			AbstractDatabaseColumnInfo columnInfo = new PostgresDatabaseColumnInfo(name, typeName);
			// not reporting columns with unsupported data types
			if("CLOB".equals(typeName) || columnInfo.getType() == null) {
				log.debug("unsupported data type: " + columnInfo.getTypeName() + " in column: " + name);
			} else {
				log.debug("Found column: " + name + " with data type: " + columnInfo.getTypeName() + ". Converted to: " + columnInfo.getType());
				columns.add(columnInfo);
			}
		}
		
		rs.close();		
		stmt.close();
		
		if(columns.isEmpty()) {
			return new TableNotFound();
		} else {
			return new DatabaseTableInfo(columns.toArray(new AbstractDatabaseColumnInfo[columns.size()]));
		}
	}

	@Override
	Object handlePerformCount(PerformCount query) throws SQLException {
		String sql = "select count(*) from " + query.getTableName();

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
		log.debug("fetch table: " + msg);
		
		StringBuilder sb = new StringBuilder("SELECT ");
		
		String separator = "";
		for(AbstractDatabaseColumnInfo columnInfo : msg.getColumns()) {
			sb.append(separator);

			String columnName = columnInfo.getName();
			sb.append("\"").append(columnName).append("\"");
			
			separator = ", ";
		}
		
		sb.append(" FROM \"");
		
		String tableName = msg.getTableName();
		int separatorIdx = tableName.indexOf(".");
		if(separatorIdx == -1) {
			sb.append(tableName);
		} else {
			sb
				.append(tableName.substring(0, separatorIdx))
				.append("\".\"")
				.append(tableName.substring(separatorIdx + 1));
		}
		
		sb.append("\" T");
		
		msg.getFilter().ifPresent(filter -> {
			sb.append(" WHERE ");
			writeFilter(filter, sb);
		});
		
		String query = sb.toString();
		
		log.debug("executing fetchtable query: {}", query);
		
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		
		ActorRef cursor = getContext().actorOf(
				PostgresDatabaseCursor.props(rs, msg, executorService),
				nameGenerator.getName(PostgresDatabaseCursor.class));
		
		return cursor;
	}
}
