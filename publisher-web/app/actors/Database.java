package actors;

import nl.idgis.publisher.domain.StatusType;
import nl.idgis.publisher.domain.query.ListEntity;
import nl.idgis.publisher.domain.response.Page;
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
			handleListEntity ((ListEntity<?>) message);
		} else {
			unhandled (message);
		}
	}
	
	private void handleListEntity (final ListEntity<?> listEntity) {
		Logger.debug ("List received for: " + listEntity.cls ().getCanonicalName ());
		
		final DataSource dataSource = new DataSource ("ds-1", "DataSource 1", new Status (DataSourceStatusType.OK, LocalDateTime.now ()));
		final Page.Builder<DataSource> pageBuilder = new Page.Builder<> ();
		
		pageBuilder.add (dataSource);
		
		sender ().tell (pageBuilder.build (), self ());
	}

}
