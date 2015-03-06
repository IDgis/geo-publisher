package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QDataSource.dataSource;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import nl.idgis.publisher.database.messages.DataSourceInfo;
import nl.idgis.publisher.database.messages.QDataSourceInfo;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.DataSourceStatusType;
import nl.idgis.publisher.domain.web.Status;
import nl.idgis.publisher.harvester.messages.GetActiveDataSources;
import akka.actor.ActorRef;
import akka.actor.Props;

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
		doList(DataSource.class, this::handleListDataSources);
		doGet(DataSource.class, this::handleGetDataSource);
	}
	
	@SuppressWarnings("unchecked")
	private CompletableFuture<Set<String>> activeDataSources() {
		return f.ask(harvester, new GetActiveDataSources(), Set.class).thenApply(resp -> resp);
	}
	
	private DataSource toDataSource(DataSourceInfo dataSourceInfo, Set<String> activeDataSources) {
		String id = dataSourceInfo.getId();
		return new DataSource(
			id, 
			dataSourceInfo.getName(),
			new Status (activeDataSources.contains(id) 
				? DataSourceStatusType.OK
				: DataSourceStatusType.NOT_CONNECTED, new Timestamp (new Date ().getTime ())));
	}
	
	private CompletableFuture<Page<DataSource>> handleListDataSources () {
		return
			db.query().from(dataSource)
			.orderBy(dataSource.identification.asc())
			.list(new QDataSourceInfo(dataSource.identification, dataSource.name))
			.thenCompose(dataSourceInfos -> 
				activeDataSources().thenApply(activeDataSources -> {
					Page.Builder<DataSource> pageBuilder = new Page.Builder<>();
					
					dataSourceInfos.list().stream()
						.map(dataSourceInfo -> toDataSource(dataSourceInfo, activeDataSources))
						.forEach(pageBuilder::add);
					
					return pageBuilder.build ();
				}));
	}
	
	private CompletableFuture<Optional<DataSource>> handleGetDataSource (String dataSourceId) {
		 return activeDataSources().thenCompose(activeDataSources ->
			db.query().from(dataSource)
			.where(dataSource.identification.eq(dataSourceId))
			.singleResult(new QDataSourceInfo(dataSource.identification, dataSource.name))
			.thenApply(queryResult -> 
				queryResult.map(dataSourceInfo -> 
					toDataSource(dataSourceInfo, activeDataSources))));
	}
}
