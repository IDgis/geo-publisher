package nl.idgis.publisher.database;

import java.sql.Connection;
import java.util.List;

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

	@Override
	protected void executeQuery(Query query) throws Exception {
		if(query instanceof CreateTable) {						
			createTable((CreateTable)query);
		} else if(query instanceof InsertRecord) {			
			insertRecord((InsertRecord)query);
		} else {
			unhandled(query);
		}
	}
	
	private void insertRecord(InsertRecord query) throws Exception {
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
		answer(new Ack());
	}
	
	private void createTable(CreateTable query) throws Exception {
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
		answer(new Ack());
	}
}
