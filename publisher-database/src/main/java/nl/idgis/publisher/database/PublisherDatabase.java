package nl.idgis.publisher.database;

import static nl.idgis.publisher.database.QVersion.version;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;

import java.sql.Timestamp;

import nl.idgis.publisher.database.messages.GetVersion;
import nl.idgis.publisher.database.messages.QVersion;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.RegisterSourceDataset;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.support.Expressions;
import com.mysema.query.types.expr.DateTimeExpression;
import com.typesafe.config.Config;

public class PublisherDatabase extends QueryDSLDatabase {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public PublisherDatabase(Config config) {
		super(config);		
	}
	
	public static Props props(Config config) {
		return Props.create(PublisherDatabase.class, config);
	}
	
	@Override
	protected void executeQuery(QueryDSLContext context, Query query) throws Exception {
		if(query instanceof GetVersion) {
			log.debug("database version requested");
			
			context.answer(
				context.query().from(version)
					.orderBy(version.id.desc())
					.limit(1)
					.singleResult(new QVersion(version.id, version.createTime)));
		} else if(query instanceof RegisterSourceDataset) {
			log.debug("registering source dataset");
			
			RegisterSourceDataset rsd = (RegisterSourceDataset)query;
			
			Tuple currentSourceDataset = 
				context.query().from(sourceDataset)
					.join(dataSource)
						.on(dataSource.id.eq(sourceDataset.dataSourceId))
					.where(sourceDataset.identification.eq(rsd.getId())
						.and(dataSource.identification.eq(rsd.getDataSource())))
					.singleResult(sourceDataset.id, sourceDataset.name, sourceDataset.deleteTime);
			
			if(currentSourceDataset != null) {
				String currentName = currentSourceDataset.get(sourceDataset.name);
				boolean isDeleted = currentSourceDataset.get(sourceDataset.deleteTime) != null;
				
				if(currentName.equals(rsd.getName()) && !isDeleted) {
					log.debug("dataset already registered");
				} else {
					context.update(sourceDataset)
						.set(sourceDataset.name, rsd.getName())
						.setNull(sourceDataset.deleteTime)						
						.set(sourceDataset.updateTime, DateTimeExpression.currentTimestamp(Timestamp.class))
						.where(sourceDataset.id.eq(currentSourceDataset.get(sourceDataset.id)))
						.execute();
					log.debug("dataset updated");
				}
			} else {
				if(context.insert(sourceDataset)
					.columns(sourceDataset.dataSourceId, sourceDataset.identification, sourceDataset.name)
					.select(new SQLSubQuery().from(dataSource)
						.where(dataSource.identification.eq(rsd.getDataSource()))
						.list(
							dataSource.id, 
							Expressions.constant(rsd.getId()),
							Expressions.constant(rsd.getName())))
					.execute() == 0) {
					log.error("couldn't find data source: " + rsd.getDataSource());
				} else {
					log.debug("dataset registered");
				}
			}
		} else {
			throw new IllegalArgumentException("Unknown query");
		}
	}
}
