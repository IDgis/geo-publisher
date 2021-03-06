package actors;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;

import nl.idgis.publisher.domain.EntityType;
import nl.idgis.publisher.domain.SourceDatasetType;
import nl.idgis.publisher.domain.StatusType;
import nl.idgis.publisher.domain.query.GetEntity;
import nl.idgis.publisher.domain.query.ListEntity;
import nl.idgis.publisher.domain.query.ListSourceDatasets;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.EntityRef;
import nl.idgis.publisher.domain.web.Notification;
import nl.idgis.publisher.domain.web.SourceDataset;
import nl.idgis.publisher.domain.web.SourceDatasetStats;
import nl.idgis.publisher.domain.web.Status;
import play.Logger;
import play.libs.Akka;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class Database extends UntypedActor {

	public final static ActorRef instance = Akka.system().actorOf (Props.create (Database.class), "database");
	
	public enum DataSourceStatusType implements StatusType {
		OK;

		@Override
		public StatusCategory statusCategory() {
			return StatusCategory.SUCCESS;
		}
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
		Logger.debug ("List received for: " + listEntity.cls ().getCanonicalName ());
		
		final DataSource dataSource = new DataSource ("ds-1", "DataSource: ds-1", new Status (DataSourceStatusType.OK, new Timestamp (new Date ().getTime ())));
		final Page.Builder<DataSource> pageBuilder = new Page.Builder<> ();
		
		pageBuilder.add (dataSource);
		
		sender ().tell (pageBuilder.build (), self ());
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
		final DataSource dataSource = new DataSource (getEntity.id (), "DataSource: " + getEntity.id (), new Status (DataSourceStatusType.OK, new Timestamp (new Date ().getTime ())));
		
		sender ().tell (dataSource, self ());
	}
	
	private void handleGetCategory (final GetEntity<?> getEntity) {
		final Category category = new Category (getEntity.id (), "Category: " + getEntity.id ());
		
		sender ().tell (category, self ());
	}
	
	private void handleListSourceDatasets (final ListSourceDatasets message) {
		final Page.Builder<SourceDatasetStats> builder = new Page.Builder<> ();
		
		if (message.categoryId () == null || "cat-1".equals (message.categoryId ())) {
			builder.add (new SourceDatasetStats (new SourceDataset ("sds-1", "SourceDataset: sds-1", null, new EntityRef (EntityType.CATEGORY, "cat-1", "Category: cat-1"), new EntityRef (EntityType.DATA_SOURCE, "ds-1", "DataSource: ds-1"), SourceDatasetType.VECTOR, false, false, false, Collections.<Notification>emptyList(), "id0", null, false), 1, null, null));
		}
		if (message.categoryId () == null || "cat-2".equals (message.categoryId ())) {
			builder.add (new SourceDatasetStats (new SourceDataset ("sds-2", "SourceDataset: sds-2", null, new EntityRef (EntityType.CATEGORY, "cat-2", "Category: cat-2"), new EntityRef (EntityType.DATA_SOURCE, "ds-1", "DataSource: ds-1"), SourceDatasetType.VECTOR, false, false, false, Collections.<Notification>emptyList(), "id1", null, false), 10, null, null));
		}
		if (message.categoryId () == null || "cat-3".equals (message.categoryId ())) {
			builder.add (new SourceDatasetStats (new SourceDataset ("sds-3", "SourceDataset: sds-3", null, new EntityRef (EntityType.CATEGORY, "cat-3", "Category: cat-3"), new EntityRef (EntityType.DATA_SOURCE, "ds-1", "DataSource: ds-1"), SourceDatasetType.VECTOR, false, false, false, Collections.<Notification>emptyList(), "id2", null, false), 0, null, null));
		}
		if (message.categoryId () == null || "cat-4".equals (message.categoryId ())) {
			builder.add (new SourceDatasetStats (new SourceDataset ("sds-4", "SourceDataset: sds-4", null, new EntityRef (EntityType.CATEGORY, "cat-4", "Category: cat-4"), new EntityRef (EntityType.DATA_SOURCE, "ds-1", "DataSource: ds-1"), SourceDatasetType.VECTOR, false, false, false, Collections.<Notification>emptyList(), "id3", null, false), 4, null, null));
		}
		if (message.categoryId () == null || "cat-5".equals (message.categoryId ())) {
			builder.add (new SourceDatasetStats (new SourceDataset ("sds-5", "SourceDataset: sds-5", null, new EntityRef (EntityType.CATEGORY, "cat-5", "Category: cat-5"), new EntityRef (EntityType.DATA_SOURCE, "ds-1", "DataSource: ds-1"), SourceDatasetType.VECTOR, false, false, false, Collections.<Notification>emptyList(), "id4", null, false), 42, null, null));
		}
		
		sender ().tell (builder.build (), self ());
	}
}
