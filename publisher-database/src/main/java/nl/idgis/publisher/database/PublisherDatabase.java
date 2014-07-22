package nl.idgis.publisher.database;

import static nl.idgis.publisher.database.QVersion.version;

import nl.idgis.publisher.database.messages.GetVersion;
import nl.idgis.publisher.database.messages.QVersion;
import nl.idgis.publisher.database.messages.Query;

import akka.actor.Props;

import com.mysema.query.sql.SQLQuery;
import com.typesafe.config.Config;

public class PublisherDatabase extends QueryDSLDatabase {

	public PublisherDatabase(Config config) {
		super(config);		
	}
	
	public static Props props(Config config) {
		return Props.create(PublisherDatabase.class, config);
	}
	
	@Override
	protected Object executeQuery(SQLQuery query, Query msg) throws Exception {
		if(msg instanceof GetVersion) {
			return
				query.from(version)
					.orderBy(version.id.desc())
					.limit(1)
					.singleResult(new QVersion(version.id, version.createTime));
		} else {
			throw new IllegalArgumentException("Unknown query");
		}
	}
}
