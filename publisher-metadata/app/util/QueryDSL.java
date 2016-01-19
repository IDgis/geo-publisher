package util;

import javax.inject.Singleton;

import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLTemplates;

import nl.idgis.publisher.database.ExtendedPostgresTemplates;

import play.db.Database;

import java.util.function.Consumer;
import java.util.function.Function;

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
	
	public void transactional(Consumer<Transaction> c) {
		withTransaction(db -> {
			c.accept(db);
			return null;
		});
	}
	
	public <T> T withTransaction(Function<Transaction, T> f) {
		return d.withTransaction(c -> {
			return f.apply(new Transaction() {

				@Override
				public SQLQuery query() {
					return new SQLQuery(c, t);
				}
				
			});
		});
	}
}
