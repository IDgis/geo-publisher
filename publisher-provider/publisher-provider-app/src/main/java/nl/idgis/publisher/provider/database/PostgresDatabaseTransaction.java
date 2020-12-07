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
			sql = " SELECT pg_attribute.attname AS column_name, pg_catalog.format_type(pg_attribute.atttypid, pg_attribute.atttypmod) AS data_type "
				+ "FROM pg_catalog.pg_attribute "
				+ "INNER JOIN pg_catalog.pg_class ON pg_class.oid = pg_attribute.attrelid "
				+ "INNER JOIN pg_catalog.pg_namespace ON pg_namespace.oid = pg_class.relnamespace "
				+ "WHERE pg_attribute.attnum > 0 AND NOT pg_attribute.attisdropped AND pg_namespace.nspname = '" + schema + "' AND pg_class.relname = '" + tableName + "' "
				+ "ORDER BY attnum";
		}
		
		log.debug("executing data dictionary query: {}", sql);
		
		ArrayList<DatabaseColumnInfo> columns = new ArrayList<>();
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		while(rs.next()) {
			String name = rs.getString(1);
			String typeName = rs.getString(2);
			
			DatabaseColumnInfo columnInfo = new DatabaseColumnInfo(name, typeName);
			// not reporting columns with unsupported data types
			if("CLOB".equals(typeName) || columnInfo.getType() == null) {
				log.debug("unsupported data type: " + columnInfo.getTypeName());
			} else {
				columns.add(columnInfo);
			}
		}
		
		rs.close();		
		stmt.close();
		
		if(columns.isEmpty()) {
			return new TableNotFound();
		} else {
			return new DatabaseTableInfo(columns.toArray(new DatabaseColumnInfo[columns.size()]));
		}
	}
	
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
	
	ActorRef handleFetchTable(FetchTable msg) throws SQLException {
		log.debug("fetch table: " + msg);
		
		StringBuilder sb = new StringBuilder("SELECT ");
		
		String separator = "";
		for(DatabaseColumnInfo columnInfo : msg.getColumns()) {
			sb.append(separator);
			
			String typeName = columnInfo.getTypeName();
			String columnName = columnInfo.getName();
			if("SDO_GEOMETRY".equals(typeName)) {
				sb
					.append("CASE ") 
						.append("WHEN T.\"").append(columnName).append("\".GET_LRS_DIM() = 0 THEN ")
							.append("CASE ")
								.append("WHEN T.\"").append(columnName).append("\".GET_DIMS() = 3 THEN SDO_CS.MAKE_2D(T.\"").append(columnName).append("\") ")
								.append("ELSE T.\"").append(columnName).append("\" ")
							.append("END ")
						.append("ELSE ") 
							.append("CASE ") 
								.append("WHEN T.\"").append(columnName).append("\".GET_DIMS() = 3 THEN SDO_LRS.CONVERT_TO_STD_GEOM(T.\"").append(columnName).append("\") ")
								.append("WHEN T.\"").append(columnName).append("\".GET_DIMS() = 4 THEN SDO_LRS.CONVERT_TO_STD_GEOM(SDO_CS.MAKE_2D(T.\"").append(columnName).append("\")) ")
							.append("END ")
					.append("END");
			} else if("ST_GEOMETRY".equals(typeName)) {
				sb
					.append("SDE.ST_ASBINARY")
					.append("(\"")
					.append(columnName)
					.append("\")");
			} else {
				sb
					.append("\"")
					.append(columnName)
					.append("\"");
			}
			
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
