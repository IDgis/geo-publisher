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
import nl.idgis.publisher.provider.protocol.database.WKBGeometry;

public class GeometryTransaction extends JdbcTransaction {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public GeometryTransaction(Connection connection) {
		super(connection);
	}

	@Override
	protected void executeQuery(JdbcContext context, Query query) throws Exception {
		if(query instanceof CreateTable) {
			CreateTable ct = (CreateTable)query;			
			createTable(context, ct.getName(), ct.getColumns());
		} else if(query instanceof InsertRecord) {
			InsertRecord ir = (InsertRecord)query;
			insertRecord(context, ir.getTableName(), ir.getColumns(), ir.getValues());
		} else {
			throw new IllegalArgumentException("Unknown query");
		}
	}
	
	private void insertRecord(JdbcContext context, String tableName, List<Column> columns, List<Object> values) throws Exception {
		StringBuilder sb = new StringBuilder("insert into ");
		sb.append(tableName);
		sb.append("(");
		
		String separator = "";
		for(Column column : columns) {
			sb.append(separator);
			sb.append(column.getName());
			
			separator = ", ";
		}
		
		sb.append(") values (");
		
		separator = "";
		for(Column column : columns) {
			sb.append(separator);
			if(column.getDataType() == Type.GEOMETRY) {
				sb.append("ST_GeomFromWKB(?)");
			} else {
				sb.append("?");
			}
			
			separator = ", ";
		}	
		
		sb.append(")");
		
		String sql = sb.toString();
		log.debug(sql);
		context.prepare(sql).execute(values, new Function<Object, Object>() {

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
		context.answer(new Ack());
	}
	
	private void createTable(JdbcContext context, String name, List<Column> columns) throws Exception {
		context.execute("drop table if exists " + name);
		
		StringBuilder sb = new StringBuilder("create table ");
		sb.append(name);
		sb.append(" (");
		
		String separator = "";
		for(Column column : columns) {
			sb.append(separator);
			sb.append(column.getName());
			sb.append(" ");
			sb.append(column.getDataType().toString().toLowerCase());
			
			separator = ", ";
		}
		
		sb.append(")");
		
		String sql = sb.toString();
		log.debug(sql);
		context.execute(sql);
		
		log.debug("ack");		
		context.answer(new Ack());
	}
}
