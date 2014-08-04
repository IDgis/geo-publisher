package nl.idgis.publisher.database;

import java.sql.Connection;
import java.util.List;

import nl.idgis.publisher.database.messages.CreateTable;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.protocol.messages.Ack;

public class GeometryTransaction extends JdbcTransaction {

	public GeometryTransaction(Connection connection) {
		super(connection);
	}

	@Override
	protected void executeQuery(JdbcContext context, Query query) throws Exception {
		if(query instanceof CreateTable) {
			CreateTable ct = (CreateTable)query;			
			createTable(context, ct.getName(), ct.getColumns());
		} else {
			throw new IllegalArgumentException("Unknown query");
		}
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
			sb.append("text");
			
			separator = ", ";
		}
		
		sb.append(")");
		
		context.execute(sb.toString());
		
		context.answer(new Ack());
	}
}
