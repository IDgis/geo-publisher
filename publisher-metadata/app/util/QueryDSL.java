package util;

import javax.inject.Singleton;

import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLTemplates;

import nl.idgis.publisher.database.ExtendedPostgresTemplates;

import play.db.Database;

import java.sql.SQLException;

import javax.inject.Inject;

@Singleton
public class QueryDSL {
	
	private final Database d;
	
	private final SQLTemplates t;
	
	public interface Transaction {
		SQLQuery query();
	}
	
	@Inject
	public QueryDSL(Database d) {
		this.d = d;
		
		t = new ExtendedPostgresTemplates();
	}
	
	public interface TransactionCallable<A> {
		public A call(Transaction transaction) throws Exception;
	}
	
	public <T> T withTransaction(TransactionCallable<T> f) {
		return d.withTransaction(c -> {
			try {
				return f.call(new Transaction() {
	
					@Override
					public SQLQuery query() {
						return new SQLQuery(c, t);
					}
					
				});
			} catch(SQLException e) {
				throw e;
			} catch(Exception e) {
				throw new SQLException(e);
			}
		});
	}
}
