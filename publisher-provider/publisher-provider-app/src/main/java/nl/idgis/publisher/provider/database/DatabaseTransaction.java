package nl.idgis.publisher.provider.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.JdbcTransaction;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.StreamingQuery;
import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.provider.database.messages.DescribeTable;
import nl.idgis.publisher.provider.database.messages.FetchTable;
import nl.idgis.publisher.provider.database.messages.PerformCount;
import nl.idgis.publisher.provider.database.messages.TableNotFound;
import nl.idgis.publisher.provider.protocol.Column;
import nl.idgis.publisher.provider.protocol.TableDescription;

public class DatabaseTransaction extends JdbcTransaction {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private ActorRef converter;

	public DatabaseTransaction(Connection connection) {
		super(connection);
	}

	@Override
	protected void executeQuery(Query query) throws Exception {
		if(query instanceof DescribeTable) {
			handleDescribeTable((DescribeTable)query);
		} else if(query instanceof PerformCount){
			handlePerformCount((PerformCount)query);
		} else {
			unhandled(query);
		}
	}
	
	@Override
	protected void executeQuery(StreamingQuery query) throws Exception {
		if(query instanceof FetchTable) {
			handleFetchTable((FetchTable)query);
		} else {
			unhandled(query);
		}
	}
	
	private void handleDescribeTable(DescribeTable query) throws SQLException {
		String requestedTableName = query.getTableName();
		
		final String sql;
		int separatorIndex = requestedTableName.indexOf(".");
		if(separatorIndex == -1) {
			sql = "select column_name, data_type from user_tab_columns "
					+ "where table_name = '" + requestedTableName.toUpperCase() + "' "
					+ "order by column_id";
		} else {
			String owner = requestedTableName.substring(0, separatorIndex);
			String tableName = requestedTableName.substring(separatorIndex + 1);
			
			sql = "select column_name, data_type from all_tab_columns "
					+ "where owner = '" + owner.toUpperCase() + "' and table_name = '" + tableName.toUpperCase() 
					+ "' " + "order by column_id";
		}
		
		Statement stmt = connection.createStatement();
		
		ArrayList<Column> columns = new ArrayList<>();
		
		ResultSet rs = stmt.executeQuery(sql);
		while(rs.next()) {
			String name = rs.getString(1);
			String typeName = rs.getString(2);
			
			Type type;
			switch(typeName.toUpperCase()) {
				case "NUMBER":
					type = Type.NUMERIC;
					break;
				case "DATE":
					type = Type.DATE;
					break;
				case "VARCHAR2":
				case "NVARCHAR2":
					type = Type.TEXT;
					break;
				case "SDO_GEOMETRY":
					type = Type.GEOMETRY;
					break;
				default:
					log.debug("unknown data type: " + typeName);
					continue;
			}
			
			columns.add(new Column(name, type));
		}
		
		rs.close();		
		stmt.close();
		
		if(columns.isEmpty()) {
			answer(new TableNotFound());
		} else {
			answer(new TableDescription(columns.toArray(new Column[columns.size()])));
		}
	}
	
	private void handlePerformCount(PerformCount query) throws SQLException {
		String sql = "select count(*) from " + query.getTableName();
		
		Statement stmt = connection.createStatement();
		
		ResultSet rs = stmt.executeQuery(sql);
		rs.next();
		
		answer(rs.getLong(1));
		
		rs.close();		
		stmt.close();	
	}
	
	private void handleFetchTable(FetchTable msg) throws SQLException {
		log.debug("fetch table: " + msg);
		
		StringBuilder sb = new StringBuilder("select ");
		
		String separator = "";
		for(String columnName : msg.getColumnNames()) {
			sb.append(separator);
			sb.append(columnName);
			
			separator = ", ";
		}
		
		sb.append(" from ");
		sb.append(msg.getTableName());
		
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery(sb.toString());
		
		if(converter == null) {
			converter = getContext().actorOf(OracleConverter.props(), "converter");
		}
		
		ActorRef cursor = getContext().actorOf(DatabaseCursor.props(rs, msg.getMessageSize(), converter));
		answerStreaming(cursor);
	}
}
