package nl.idgis.publisher.database;

import java.sql.Connection;

import com.mysema.query.sql.RelationalPath;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLTemplates;
import com.mysema.query.sql.dml.SQLDeleteClause;
import com.mysema.query.sql.dml.SQLInsertClause;
import com.mysema.query.sql.dml.SQLUpdateClause;
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
	
	protected SQLQuery query() {
		return new SQLQuery(connection, templates);
	}
	
	protected SQLInsertClause insert(RelationalPath<?> entity) {
		return new SQLInsertClause(connection, templates, entity);
	}
	
	protected SQLUpdateClause update(RelationalPath<?> entity) {
		return new SQLUpdateClause(connection, templates, entity);
	}
	
	protected SQLDeleteClause delete(RelationalPath<?> entity) {
		return new SQLDeleteClause(connection, templates, entity);
	}
}
