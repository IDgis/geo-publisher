package nl.idgis.publisher.provider.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.JdbcTransaction;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.StreamingQuery;
import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.provider.database.messages.CompoundFilter;
import nl.idgis.publisher.provider.database.messages.DatabaseColumnInfo;
import nl.idgis.publisher.provider.database.messages.DatabaseTableInfo;
import nl.idgis.publisher.provider.database.messages.DescribeTable;
import nl.idgis.publisher.provider.database.messages.FetchTable;
import nl.idgis.publisher.provider.database.messages.Filter;
import nl.idgis.publisher.provider.database.messages.PerformCount;
import nl.idgis.publisher.provider.database.messages.ColumnFilter;
import nl.idgis.publisher.provider.database.messages.TableNotFound;
import nl.idgis.publisher.utils.UniqueNameGenerator;

public class DatabaseTransaction extends JdbcTransaction {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	public DatabaseTransaction(Connection connection) {
		super(connection);
	}
	
	public static Props props(Connection connection) {
		return Props.create(DatabaseTransaction.class, connection);
	}

	@Override
	protected Object executeQuery(Query query) throws Exception {
		if(query instanceof DescribeTable) {
			return handleDescribeTable((DescribeTable)query);
		} else if(query instanceof PerformCount){
			return handlePerformCount((PerformCount)query);
		} else {
			return null;
		}
	}
	
	@Override
	protected ActorRef executeQuery(StreamingQuery query) throws Exception {
		if(query instanceof FetchTable) {
			return handleFetchTable((FetchTable)query);
		} else {
			return null;
		}
	}
	
	private Object handleDescribeTable(DescribeTable query) throws SQLException {
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
	
	private Object handlePerformCount(PerformCount query) throws SQLException {
		String sql = "select count(*) from " + query.getTableName();
		
		Statement stmt = connection.createStatement();
		
		ResultSet rs = stmt.executeQuery(sql);
		rs.next();
		
		Object retval = rs.getLong(1);
		
		rs.close();		
		stmt.close();	
		
		return retval;
	}
	
	private static void writeFilter(Filter filter, StringBuilder sb) {
		if(filter instanceof CompoundFilter) {
			CompoundFilter compoundFilter = (CompoundFilter)filter;
			String filterSeparator = " " + compoundFilter.getOperator() + " ";
			
			String separator = "";
			for(Filter compoundFilterItem : compoundFilter.getFilters()) {
				sb
					.append(separator)
					.append("(");
				writeFilter(compoundFilterItem, sb);
				sb.append(")");
				
				separator = filterSeparator;
			}
		} else if(filter instanceof ColumnFilter) {
			ColumnFilter columnFilter = (ColumnFilter)filter;
			DatabaseColumnInfo column = columnFilter.getColumn();
			
			sb
				.append("\"")
				.append(column.getName())
				.append("\" ")
				.append(columnFilter.getOperator())
				.append(" ");
			
			columnFilter.getOperand().ifPresent(operand -> {
				if(column.getType() == Type.TEXT) {
					sb
						.append("'")
						.append(operand.toString().replace("'", "''"))
						.append("'");
				} else {
					sb.append(operand);
				}
			});
		} else {
			throw new IllegalArgumentException("unknown filter type: " + filter.getClass().getCanonicalName());
		}
	}
	
	private ActorRef handleFetchTable(FetchTable msg) throws SQLException {
		log.debug("fetch table: " + msg);
		
		StringBuilder sb = new StringBuilder("SELECT ");
		
		String separator = "";
		for(DatabaseColumnInfo columnInfo : msg.getColumns()) {
			sb.append(separator);
			
			String typeName = columnInfo.getTypeName();
			if("SDO_GEOMETRY".equals(typeName)) {
				sb
					.append("SDO_UTIL.TO_WKBGEOMETRY")
					.append("(\"")
					.append(columnInfo.getName())
					.append("\")");
			} else if("ST_GEOMETRY".equals(typeName)) {
				sb
					.append("SDE.ST_ASBINARY")
					.append("(\"")
					.append(columnInfo.getName())
					.append("\")");
			} else {
				sb
					.append("\"")
					.append(columnInfo.getName())
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
		
		sb.append("\"");
		
		msg.getFilter().ifPresent(filter -> {
			sb.append(" WHERE ");
			writeFilter(filter, sb);
		});
		
		String query = sb.toString();
		
		log.debug("executing query: {}", query);
		
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		
		ActorRef cursor = getContext().actorOf(
				DatabaseCursor.props(rs, msg, executorService), 
				nameGenerator.getName(DatabaseCursor.class));
		
		return cursor;
	}
}
