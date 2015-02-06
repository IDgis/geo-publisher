package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;

import com.mysema.query.sql.SQLSubQuery;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.admin.messages.QSourceDatasetInfo;
import nl.idgis.publisher.admin.messages.SourceDatasetInfo;

import nl.idgis.publisher.database.AsyncSQLQuery;
import nl.idgis.publisher.database.QSourceDatasetVersion;

import nl.idgis.publisher.domain.EntityType;
import nl.idgis.publisher.domain.query.ListSourceDatasets;
import nl.idgis.publisher.domain.web.EntityRef;
import nl.idgis.publisher.domain.web.SourceDataset;
import nl.idgis.publisher.domain.web.SourceDatasetStats;
import nl.idgis.publisher.domain.response.Page;

public class DataSources extends AbstractAdmin {
	
	private final QSourceDatasetVersion sourceDatasetVersionSub = new QSourceDatasetVersion("source_dataset_version_sub");

	public DataSources(ActorRef database) {
		super(database);
	}
	
	public static Props props(ActorRef database) {
		return Props.create(DataSources.class, database);
	}	

	@Override
	protected void preStartAdmin() {
		addQuery(ListSourceDatasets.class, msg -> {
			return db.transactional(tx -> {
				AsyncSQLQuery baseQuery = tx.query().from(sourceDataset)
					.join (sourceDatasetVersion).on(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id)
						.and(new SQLSubQuery().from(sourceDatasetVersionSub)
							.where(sourceDatasetVersionSub.sourceDatasetId.eq(sourceDatasetVersion.sourceDatasetId)
								.and(sourceDatasetVersionSub.id.gt(sourceDatasetVersion.id)))
							.notExists()))
					.join (dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
					.join (category).on(sourceDatasetVersion.categoryId.eq(category.id));
				
				String categoryId = msg.categoryId();
				if(categoryId != null) {				
					baseQuery.where(category.identification.eq(categoryId));
				}
				
				String dataSourceId = msg.dataSourceId();
				if(dataSourceId != null) {				
					baseQuery.where(dataSource.identification.eq(dataSourceId));
				}
				
				String searchStr = msg.getSearchString();
				if (!(searchStr == null || searchStr.isEmpty())){
					baseQuery.where(sourceDatasetVersion.name.containsIgnoreCase(searchStr)); 				
				}
					
				AsyncSQLQuery listQuery = baseQuery.clone()					
					.leftJoin(dataset).on(dataset.sourceDatasetId.eq(sourceDataset.id));
				
				Long page = msg.getPage();
				singlePage(listQuery, page);
				
				return f
					.collect(listQuery					
						.groupBy(sourceDataset.identification).groupBy(sourceDatasetVersion.name)
						.groupBy(dataSource.identification).groupBy(dataSource.name)
						.groupBy(category.identification).groupBy(category.name)		
						.orderBy(sourceDatasetVersion.name.trim().asc())
						.list(new QSourceDatasetInfo(sourceDataset.identification, sourceDatasetVersion.name, 
							dataSource.identification, dataSource.name,
							category.identification,category.name,
							dataset.count())))
					.collect(baseQuery.count()).thenApply((list, count) -> {
						Page.Builder<SourceDatasetStats> pageBuilder = new Page.Builder<> ();
						
						for(SourceDatasetInfo sourceDatasetInfo : list) {
							SourceDataset sourceDataset = new SourceDataset (
								sourceDatasetInfo.getId(), 
								sourceDatasetInfo.getName(),
								new EntityRef (EntityType.CATEGORY, sourceDatasetInfo.getCategoryId(),sourceDatasetInfo.getCategoryName()),
								new EntityRef (EntityType.DATA_SOURCE, sourceDatasetInfo.getDataSourceId(), sourceDatasetInfo.getDataSourceName())
							);
							
							pageBuilder.add (new SourceDatasetStats (sourceDataset, sourceDatasetInfo.getCount()));
						}
						
						addPageInfo(pageBuilder, page, count);
						
						return pageBuilder.build();
					});
			});
		});
	}
}
