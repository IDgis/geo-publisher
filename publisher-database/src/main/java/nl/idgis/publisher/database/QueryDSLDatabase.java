package nl.idgis.publisher.database;

import nl.idgis.publisher.database.messages.Query;

import com.mysema.query.sql.SQLTemplates;
import com.typesafe.config.Config;

public abstract class QueryDSLDatabase extends JdbcDatabase {
		
	private SQLTemplates templates;	

	public QueryDSLDatabase(Config config) {
		super(config);
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
