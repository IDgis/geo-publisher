package nl.idgis.publisher.service.admin;

import java.util.List;
import java.util.Set;

import nl.idgis.publisher.database.messages.DataSourceInfo;
import nl.idgis.publisher.database.messages.GetDataSourceInfo;
import nl.idgis.publisher.database.messages.GetSourceDatasetInfo;
import nl.idgis.publisher.database.messages.SourceDatasetInfo;
import nl.idgis.publisher.domain.query.GetEntity;
import nl.idgis.publisher.domain.query.ListEntity;
import nl.idgis.publisher.domain.query.ListSourceDatasets;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.DataSourceStatusType;
import nl.idgis.publisher.domain.web.EntityRef;
import nl.idgis.publisher.domain.web.EntityType;
import nl.idgis.publisher.domain.web.SourceDataset;
import nl.idgis.publisher.domain.web.SourceDatasetStats;
import nl.idgis.publisher.domain.web.Status;
import nl.idgis.publisher.harvester.messages.GetActiveDataSources;

import org.joda.time.LocalDateTime;

import scala.concurrent.Future;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class Admin extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database, harvester;
	
	public Admin(ActorRef database, ActorRef harvester) {
		this.database = database;
		this.harvester = harvester;		
	}
	
	public static Props props(ActorRef database, ActorRef harvester) {
		return Props.create(Admin.class, database, harvester);
	}

	@Override
	public void onReceive (final Object message) throws Exception {
		if (message instanceof ListEntity<?>) {
			final ListEntity<?> listEntity = (ListEntity<?>)message;
			
			if (listEntity.cls ().equals (DataSource.class)) {
				handleListDataSources (listEntity);
			} else if (listEntity.cls ().equals (Category.class)) {
				handleListCategories (listEntity);
			} else {
				handleEmptyList (listEntity);
			}
		} else if (message instanceof GetEntity<?>) {
			final GetEntity<?> getEntity = (GetEntity<?>)message;
			
			if (getEntity.cls ().equals (DataSource.class)) {
				handleGetDataSource (getEntity);
			} else if (getEntity.cls ().equals (Category.class)) {
				handleGetCategory (getEntity);
			} else {
				sender ().tell (null, self ());
			}
		} else if (message instanceof ListSourceDatasets) {
			handleListSourceDatasets ((ListSourceDatasets)message);
		} else {
			unhandled (message);
		}
	}
	
	private void handleListDataSources (final ListEntity<?> listEntity) {
		log.debug ("List received for: " + listEntity.cls ().getCanonicalName ());
		
		final ActorRef sender = getSender(), self = getSelf();
		
		final Future<Object> activeDataSources = Patterns.ask(harvester, new GetActiveDataSources(), 15000);
		final Future<Object> dataSourceInfo = Patterns.ask(database, new GetDataSourceInfo(), 15000);
		
		activeDataSources.onSuccess(new OnSuccess<Object>() {
			
			@Override
			@SuppressWarnings("unchecked")
			public void onSuccess(Object msg) throws Throwable {
				final Set<String> activeDataSources = (Set<String>)msg;
				log.debug("active data sources received");
				
				dataSourceInfo.onSuccess(new OnSuccess<Object>() {

					@Override
					public void onSuccess(Object msg) throws Throwable {
						List<DataSourceInfo> dataSourceInfoList = (List<DataSourceInfo>)msg;
						log.debug("data sources info received");
						
						final Page.Builder<DataSource> pageBuilder = new Page.Builder<> ();
						
						for(DataSourceInfo dataSourceInfo : dataSourceInfoList) {
							final String id = dataSourceInfo.getId();
							final DataSource dataSource = new DataSource (
									id, 
									dataSourceInfo.getName(),
									new Status (activeDataSources.contains(id) 
											? DataSourceStatusType.OK
											: DataSourceStatusType.NOT_CONNECTED, LocalDateTime.now ()));
							
							pageBuilder.add (dataSource);
						}
						
						log.debug("sending data source page");
						sender.tell (pageBuilder.build (), self);
					}
				}, getContext().dispatcher());
			}			
		}, getContext().dispatcher());
	}
	
	private void handleListCategories (final ListEntity<?> listEntity) {
		final Page.Builder<Category> builder = new Page.Builder<> ();
		
		builder.add (new Category ("cat-1", "Category: cat1"));
		builder.add (new Category ("cat-2", "Category: cat2"));
		builder.add (new Category ("cat-3", "Category: cat3"));
		builder.add (new Category ("cat-4", "Category: cat4"));
		builder.add (new Category ("cat-5", "Category: cat5"));
		
		sender ().tell (builder.build (), self ());
	}
	
	private void handleEmptyList (final ListEntity<?> listEntity) {
		final Page.Builder<Category> builder = new Page.Builder<> ();
		
		sender ().tell (builder.build (), self ());
	}
	
	private void handleGetDataSource (final GetEntity<?> getEntity) {
		final DataSource dataSource = new DataSource (getEntity.id (), "DataSource: " + getEntity.id (), new Status (DataSourceStatusType.OK, LocalDateTime.now ()));
		
		sender ().tell (dataSource, self ());
	}
	
	private void handleGetCategory (final GetEntity<?> getEntity) {
		final Category category = new Category (getEntity.id (), "Category: " + getEntity.id ());
		
		sender ().tell (category, self ());
	}
	
	private void handleListSourceDatasets (final ListSourceDatasets message) {
		
		log.debug ("handleListSourceDatasets");
		
		final ActorRef sender = getSender(), self = getSelf();
		
		final Future<Object> sourceDatasetInfo = Patterns.ask(database, new GetSourceDatasetInfo(message.dataSourceId(), message.categoryId()), 15000);
		
				sourceDatasetInfo.onSuccess(new OnSuccess<Object>() {

					@SuppressWarnings("unchecked")
					@Override
					public void onSuccess(Object msg) throws Throwable {
						List<SourceDatasetInfo> sourceDatasetInfoList = (List<SourceDatasetInfo>)msg;
						log.debug("data sources info received");
						
						final Page.Builder<SourceDatasetStats> pageBuilder = new Page.Builder<> ();
						
						for(SourceDatasetInfo sourceDatasetInfo : sourceDatasetInfoList) {
							final SourceDataset sourceDataset = new SourceDataset (
									sourceDatasetInfo.getId(), 
									sourceDatasetInfo.getName(),
									new Category ("cat-3", "Category: cat-333"),
									new EntityRef (EntityType.DATA_SOURCE, sourceDatasetInfo.getDataSourceId(), sourceDatasetInfo.getDataSourceName())
							);
							
							pageBuilder.add (new SourceDatasetStats (sourceDataset, sourceDatasetInfo.getCount()));
						}
						
						log.debug("sending data source page");
						sender.tell (pageBuilder.build (), self);
					}
				}, getContext().dispatcher());
		
	}
}
