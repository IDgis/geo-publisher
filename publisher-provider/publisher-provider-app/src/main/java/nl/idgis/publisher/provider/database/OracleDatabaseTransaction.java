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

public class OracleDatabaseTransaction extends AbstractDatabaseTransaction {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	public OracleDatabaseTransaction(Config config, Connection connection) {
		super(config, connection);
	}
	
	public static Props props(Config config, Connection connection) {
		return Props.create(OracleDatabaseTransaction.class, config, connection);
	}
	
	Object handleDescribeTable(DescribeTable query) throws SQLException {

		String owner = query.getScheme();
		String tableName = query.getTableName().toUpperCase();

		final String sql;

		if(owner == null) {
			sql = "select column_name, data_type from user_tab_columns "
					+ "where table_name = '" + tableName + "' "
					+ "order by column_id";
		} else {
			sql = "select column_name, data_type from all_tab_columns "
					+ "where owner = '" + owner.toUpperCase() + "' and table_name = '" + tableName
					+ "' " + "order by column_id";
		}
		
		log.debug("executing data dictionary query: {}", sql);
		
		ArrayList<AbstractDatabaseColumnInfo> columns = new ArrayList<>();
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		while(rs.next()) {
			String name = rs.getString(1);
			String typeName = rs.getString(2);
			
			AbstractDatabaseColumnInfo columnInfo = new OracleDatabaseColumnInfo(name, typeName);
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
			return new DatabaseTableInfo(columns.toArray(new AbstractDatabaseColumnInfo[columns.size()]));
		}
	}
	
	Object handlePerformCount(PerformCount query) throws SQLException {

		String owner = query.getScheme();
		String tableName = query.getTableName();

		String sql;
		if (owner == null) {
			sql = "select count(*) from " + tableName;
		} else {
			sql = "select count(*) from " + owner + "." + tableName;
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
	
	ActorRef handleFetchTable(FetchTable msg) throws SQLException {
		log.debug("fetch table: " + msg);
		
		StringBuilder sb = new StringBuilder("SELECT ");
		
		String separator = "";
		for(AbstractDatabaseColumnInfo columnInfo : msg.getColumns()) {
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

		String owner = msg.getScheme();
		String tableName = msg.getTableName();

		if (owner == null) {
			sb.append(tableName);
		} else {
			sb
				.append(owner)
				.append("\".\"")
				.append(tableName);
		}
		
		sb.append("\" T");
		
		msg.getFilter().ifPresent(filter -> {
			sb.append(" WHERE ");
			writeFilter(filter, sb, DatabaseType.ORACLE);
		});
		
		String query = sb.toString();
		
		log.debug("executing fetchtable query: {}", query);
		
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		
		ActorRef cursor = getContext().actorOf(
				OracleDatabaseCursor.props(rs, msg, executorService),
				nameGenerator.getName(OracleDatabaseCursor.class));
		
		return cursor;
	}
}
