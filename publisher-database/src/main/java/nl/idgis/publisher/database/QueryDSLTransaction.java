package nl.idgis.publisher.database;

import java.sql.Connection;
import java.util.List;

import com.mysema.query.QueryMetadata;
import com.mysema.query.sql.Configuration;
import com.mysema.query.sql.RelationalPath;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLTemplates;
import com.mysema.query.sql.dml.SQLDeleteClause;
import com.mysema.query.sql.dml.SQLInsertClause;
import com.mysema.query.sql.dml.SQLUpdateClause;
import com.mysema.query.types.Expression;
import com.mysema.query.types.Path;
import com.typesafe.config.Config;

import nl.idgis.publisher.utils.TypedList;

public abstract class QueryDSLTransaction extends JdbcTransaction {
		
	private SQLTemplates templates;

	public QueryDSLTransaction(Config config, Connection connection) {
		super(config, connection);
	}
	
	@Override
	public void transactionPreStart() throws Exception {		
		templates = Class.forName(config.getString("templates"))
				.asSubclass(SQLTemplates.class)
				.newInstance();
	}
	
	protected SQLQuery query() {
		return new SQLQuery(connection, templates);
	}
	
	protected SQLQuery query(QueryMetadata metadata) {
		return new SQLQuery(connection, new Configuration(templates), metadata);
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
	
	@SuppressWarnings("unchecked")
	protected <T> TypedList<T> toTypedList(Expression<T> expression, List<T> list) {
		return new TypedList<>((Class<T>)expression.getType(), list);
	}
	
	protected <T> TypedList<T> toTypedList(SQLInsertClause insert, Path<T> path) {
		return toTypedList(path, insert.executeWithKeys(path));
	}
	
	protected <T> TypedList<T> toTypedList(QueryMetadata metadata, Expression<T> expression) {
		return toTypedList(expression, query(metadata).list(expression));		
	}
}
