package nl.idgis.publisher.database;

import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLTemplates;
import com.typesafe.config.Config;

public abstract class QueryDSLDatabase extends JdbcDatabase {
	
	private final String dialect;
	
	private SQLTemplates templates;

	public QueryDSLDatabase(Config config) {
		super(config);
		
		dialect = config.getString("dialect");
	}
	
	@Override
	public void preStart() throws Exception {
		super.preStart();
		
		templates = Class.forName(dialect).asSubclass(SQLTemplates.class).newInstance();
	}
	
	protected SQLQuery query() {
		return new SQLQuery(connection, templates);
	}
}
