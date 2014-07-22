package nl.idgis.publisher.database;

import java.sql.Connection;

import nl.idgis.publisher.database.messages.Query;

import com.mysema.query.sql.SQLQuery;
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
	protected Object executeQuery(Connection connection, Query msg) throws Exception {
		return executeQuery(new SQLQuery(connection, templates), msg);
	}
	
	protected abstract Object executeQuery(SQLQuery query, Query msg) throws Exception;
}
