package nl.idgis.publisher.database;

import java.sql.Connection;

import nl.idgis.publisher.database.messages.Query;

import com.mysema.query.sql.SQLTemplates;
import com.typesafe.config.Config;

public abstract class QueryDSLTransaction extends JdbcTransaction {
		
	private SQLTemplates templates;
	private Config config;

	public QueryDSLTransaction(Config config, Connection connection) {
		super(connection);
		
		this.config = config;
	}
	
	@Override
	public void preStart() throws Exception {
		super.preStart();
		
		templates = Class.forName(config.getString("templates"))
				.asSubclass(SQLTemplates.class)
				.newInstance();
	}

	@Override
	protected void executeQuery(JdbcContext context, Query query) throws Exception {		
		executeQuery(new QueryDSLContext(context, templates), query);
	}
	
	protected abstract void executeQuery(QueryDSLContext context, Query query) throws Exception;
}
