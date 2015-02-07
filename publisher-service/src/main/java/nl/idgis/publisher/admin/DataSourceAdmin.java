package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QDataSource.dataSource;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.database.messages.DataSourceInfo;
import nl.idgis.publisher.database.messages.QDataSourceInfo;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.DataSourceStatusType;
import nl.idgis.publisher.domain.web.Status;

import nl.idgis.publisher.harvester.messages.GetActiveDataSources;

public class DataSourceAdmin extends AbstractAdmin {
	
	private ActorRef harvester;

	public DataSourceAdmin(ActorRef database, ActorRef harvester) {
		super(database); 
		
		this.harvester = harvester;
	}
	
	public static Props props(ActorRef database, ActorRef harvester) {
		return Props.create(DataSourceAdmin.class, database, harvester);
	}

	@Override
	protected void preStartAdmin() {
		addList(DataSource.class, this::handleListDataSources);
		addGet(DataSource.class, this::handleGetDataSource);
	}
	
	private CompletableFuture<Page<DataSource>> handleListDataSources () {
		return
			db.query().from(dataSource)
			.orderBy(dataSource.identification.asc())
			.list(new QDataSourceInfo(dataSource.identification, dataSource.name))
			.thenCompose(dataSourceInfos -> 
				f.ask(harvester, new GetActiveDataSources(), Set.class).thenApply(activeDataSources -> {
					final Page.Builder<DataSource> pageBuilder = new Page.Builder<> ();
					
					for(DataSourceInfo dataSourceInfo : dataSourceInfos) {
						final String id = dataSourceInfo.getId() ;
						final DataSource dataSourceBuilt = new DataSource (
							id, 
							dataSourceInfo.getName(),
							new Status (activeDataSources.contains(id) 
								? DataSourceStatusType.OK
								: DataSourceStatusType.NOT_CONNECTED, new Timestamp (new Date ().getTime ())));
						
						pageBuilder.add (dataSourceBuilt);
					}
					
					return pageBuilder.build ();
				}));
	}
	
	private CompletableFuture<Optional<DataSource>> handleGetDataSource (String dataSourceId) {
		return f.successful(Optional.of(new DataSource (dataSourceId, "DataSource: " + dataSourceId, new Status (DataSourceStatusType.OK, new Timestamp (new Date ().getTime ())))));
	}

}
