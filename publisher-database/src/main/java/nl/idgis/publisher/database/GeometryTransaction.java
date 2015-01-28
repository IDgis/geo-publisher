package nl.idgis.publisher.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;

import nl.idgis.publisher.database.messages.CreateTable;
import nl.idgis.publisher.database.messages.InsertRecord;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.protocol.WKBGeometry;

public class GeometryTransaction extends JdbcTransaction {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public GeometryTransaction(Connection connection) {
		super(connection);
	}
	
	public static Props props(Connection connection) {
		return Props.create(GeometryTransaction.class, connection);
	}
	
	private static class Prepared {
		
		PreparedStatement stmt;
		
		private Prepared(PreparedStatement stmt) {
			this.stmt = stmt;
		}
		
		public void execute(List<Object> args, Function<Object, Object> converter) throws Exception {
			int i = 1;
			
			for(Object arg : args) {
				stmt.setObject(i++, converter.apply(arg));
			}
			
			stmt.execute();
			stmt.close();
		}
	}
	
	private Prepared prepare(String sql) throws SQLException {
		return new Prepared(connection.prepareStatement(sql));
	}
	
	private void execute(String sql) throws SQLException {
		Statement stmt = connection.createStatement();
		stmt.execute(sql);
		stmt.close();
	}

	@Override
	protected Object executeQuery(Query query) throws Exception {
		if(query instanceof CreateTable) {						
			return createTable((CreateTable)query);
		} else if(query instanceof InsertRecord) {			
			return insertRecord((InsertRecord)query);
		} else {
			return null;
		}
	}
	
	private Object insertRecord(InsertRecord query) throws Exception {
		String schemaName = query.getSchemaName();
		String tableName = query.getTableName();
		List<Column> columns = query.getColumns();
		List<Object> values = query.getValues();
		
		StringBuilder sb = new StringBuilder("insert into \"");
		sb.append(schemaName);
		sb.append("\".\"");
		sb.append(tableName);
		sb.append("\"(");
		
		String separator = "";
		for(Column column : columns) {
			sb.append(separator);
			sb.append("\"");
			sb.append(column.getName());
			sb.append("\"");
			
			separator = ", ";
		}
		
		sb.append(") values (");
		
		separator = "";
		for(Column column : columns) {
			sb.append(separator);
			if(column.getDataType() == Type.GEOMETRY) {
				sb.append("ST_SetSRID(ST_GeomFromWKB(?), 28992)");
			} else {
				sb.append("?");
			}
			
			separator = ", ";
		}	
		
		sb.append(")");
		
		String sql = sb.toString();
		log.debug(sql);
		prepare(sql).execute(values, new Function<Object, Object>() {

			@Override
			public Object apply(Object o) throws Exception {
				if(o instanceof WKBGeometry) {
					return ((WKBGeometry) o).getBytes();
				} else {
					return o;
				}
			}
		});
		
		log.debug("ack");

		return new Ack();
	}
	
	private Object createTable(CreateTable query) throws Exception {
		String schemaName = query.getSchemaName();
		String tableName = query.getTableName();
		List<Column> columns = query.getColumns();
		
		execute("create schema if not exists \"" + schemaName + "\"");
		
		execute("drop table if exists \"" + schemaName + "\".\"" + tableName + "\"");
		
		StringBuilder sb = new StringBuilder("create table \"");
		sb.append(schemaName);
		sb.append("\".\"");
		sb.append(tableName);		
		sb.append("\" (");
		
		String separator = "";
		for(Column column : columns) {
			sb.append(separator);
			sb.append("\"");
			sb.append(column.getName());
			sb.append("\"");
			sb.append(" ");
			
			Type dataType = column.getDataType();
			if(dataType.equals(Type.GEOMETRY)) {
				sb.append("geometry(Geometry, 28992)");
			} else {
				sb.append(dataType.toString().toLowerCase());
			}
			
			separator = ", ";
		}
		
		sb.append(")");
		
		String sql = sb.toString();
		log.debug(sql);
		execute(sql);
		
		log.debug("ack");		
		
		return new Ack();
	}
}
