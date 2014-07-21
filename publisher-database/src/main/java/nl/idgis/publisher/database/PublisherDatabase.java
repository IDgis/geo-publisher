package nl.idgis.publisher.database;

import nl.idgis.publisher.database.messages.GetVersion;
import akka.actor.Props;

import com.typesafe.config.Config;

import nl.idgis.publisher.database.messages.QVersion;
import static nl.idgis.publisher.database.QVersion.version;

public class PublisherDatabase extends QueryDSLDatabase {

	public PublisherDatabase(Config config) {
		super(config);	
	}
	
	public static Props props(Config config) {
		return Props.create(PublisherDatabase.class, config);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof GetVersion) {
			answer(
				query().from(version)
					.orderBy(version.id.desc())
					.limit(1)
				.singleResult(new QVersion(version.id, version.createTime)));
		} else {
			unhandled(msg);
		}
	}
}
