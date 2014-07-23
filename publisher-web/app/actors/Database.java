package actors;

import nl.idgis.publisher.domain.StatusType;
import nl.idgis.publisher.domain.query.GetEntity;
import nl.idgis.publisher.domain.query.ListEntity;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.Status;

import org.joda.time.LocalDateTime;

import play.Logger;
import akka.actor.UntypedActor;

public class Database extends UntypedActor {

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
		} else {
			unhandled (message);
		}
	}
	
	private void handleListDataSources (final ListEntity<?> listEntity) {
		Logger.debug ("List received for: " + listEntity.cls ().getCanonicalName ());
		
		final DataSource dataSource = new DataSource ("ds-1", "DataSource: ds-1", new Status (DataSourceStatusType.OK, LocalDateTime.now ()));
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
		final DataSource dataSource = new DataSource (getEntity.id (), "DataSource: " + getEntity.id (), new Status (DataSourceStatusType.OK, LocalDateTime.now ()));
		
		sender ().tell (dataSource, self ());
	}
	
	private void handleGetCategory (final GetEntity<?> getEntity) {
		final Category category = new Category (getEntity.id (), "Category: " + getEntity.id ());
		
		sender ().tell (category, self ());
	}
}
