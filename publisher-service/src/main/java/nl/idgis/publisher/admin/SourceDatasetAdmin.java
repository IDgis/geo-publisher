package nl.idgis.publisher.admin;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.QTuple;

import akka.actor.ActorRef;
import akka.actor.Props;
import nl.idgis.publisher.domain.EntityType;
import nl.idgis.publisher.domain.SourceDatasetType;
import nl.idgis.publisher.domain.job.NotificationProperties;
import nl.idgis.publisher.domain.job.harvest.HarvestNotificationType;
import nl.idgis.publisher.domain.query.GetMetadata;
import nl.idgis.publisher.domain.web.EntityRef;
import nl.idgis.publisher.domain.web.Message;
import nl.idgis.publisher.domain.web.Metadata;
import nl.idgis.publisher.domain.web.Notification;
import nl.idgis.publisher.domain.web.SourceDataset;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QHarvestNotification.harvestNotification;
import static nl.idgis.publisher.database.QSourceDatasetMetadata.sourceDatasetMetadata;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;

public class SourceDatasetAdmin extends AbstractAdmin {
	
	public SourceDatasetAdmin(ActorRef database) throws Exception {
		super(database);
	}
	
	public static Props props(ActorRef database) {
		return Props.create(SourceDatasetAdmin.class, database);
	}

	@Override
	protected void preStartAdmin() {
		doQueryOptional(GetMetadata.class, this::handleGetMetadata);
		doGet(SourceDataset.class, this::handleGetSourceDataset);
	}
	
	private CompletableFuture<Optional<SourceDataset>> handleGetSourceDataset(String sourceDatasetId) {
		
		final CompletableFuture<List<Notification>> harvestNotificationsFuture = 
				db.query().from(harvestNotification)
					.join(sourceDataset).on(sourceDataset.id.eq(harvestNotification.sourceDatasetId))
					.join(sourceDatasetVersion).on(sourceDatasetVersion.id.eq(harvestNotification.sourceDatasetVersionId))
					.where(sourceDataset.identification.eq(sourceDatasetId)
							.and(harvestNotification.done.eq(false)))
					.orderBy(harvestNotification.createTime.desc())
					.list(harvestNotification.id, harvestNotification.notificationType, 
							harvestNotification.createTime, harvestNotification.sourceDatasetVersionId, 
							sourceDataset.identification, sourceDatasetVersion.name)
					.thenApply(typedList -> {
						List<Tuple> listTuple = typedList.list().stream().collect(Collectors.toList());
						
						return listTuple.stream().map(t ->
							new Notification("" + t.get(harvestNotification.id),
								new Message(
										HarvestNotificationType.getHarvestNotificationType
											(t.get(harvestNotification.notificationType)),
									new NotificationProperties(
										EntityType.SOURCE_DATASET,
										t.get(sourceDataset.identification),
										t.get(sourceDatasetVersion.name),
										t.get(harvestNotification.createTime)
											.toLocalDateTime()
											.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")),
										null))))
						.collect(Collectors.toList());
		});
		
		CompletableFuture<Optional<Tuple>> querySourceDatasetFuture = db.query().from(sourceDatasetVersion)
				.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
				.leftJoin(category).on(category.id.eq(sourceDatasetVersion.categoryId))
				.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
				.where(sourceDataset.identification.eq(sourceDatasetId)
						.and(sourceDatasetVersion.id.eq(
								new SQLSubQuery().from(sourceDatasetVersion)
									.join(sourceDataset).on(sourceDataset.id
										.eq(sourceDatasetVersion.sourceDatasetId))
									.where(sourceDataset.identification.eq(sourceDatasetId))
									.unique(sourceDatasetVersion.id.max()))))
				.singleResult(sourceDataset.id, sourceDatasetVersion.name, 
						sourceDatasetVersion.alternateTitle, category.identification, category.name, 
						dataSource.identification, dataSource.name, sourceDatasetVersion.type, 
						sourceDataset.deleteTime, sourceDatasetVersion.confidential, 
						sourceDatasetVersion.wmsOnly, sourceDataset.externalIdentification,
						sourceDatasetVersion.physicalName);
		
		return querySourceDatasetFuture.thenCompose(optionalSourceDataset -> {
			if(optionalSourceDataset.isPresent()) {
				Tuple sourceDatasetTuple = optionalSourceDataset.get();
				String sourceDataTypeString = sourceDatasetTuple.get(sourceDatasetVersion.type);
				SourceDatasetType type;
				
				switch(sourceDataTypeString) {
				case "VECTOR":
					type = SourceDatasetType.VECTOR;
					break;
				case "RASTER":
					type = SourceDatasetType.RASTER;
					break;
				default:
					type = SourceDatasetType.UNAVAILABLE;
					break;
				}
				
				EntityRef categoryRef;
				if(sourceDatasetTuple.get(category.identification) == null) {
					categoryRef = null;
				} else {
					categoryRef = new EntityRef(EntityType.CATEGORY, 
							sourceDatasetTuple.get(category.identification), 
							sourceDatasetTuple.get(category.name));
				}
				
				return harvestNotificationsFuture.thenCompose(listNotifications -> {
					return f.successful(
							Optional.of(
								new SourceDataset(
									sourceDatasetId,
									sourceDatasetTuple.get(sourceDatasetVersion.name),
									sourceDatasetTuple.get(sourceDatasetVersion.alternateTitle),
									categoryRef,
									new EntityRef(EntityType.DATA_SOURCE, 
											sourceDatasetTuple.get(dataSource.identification), 
											sourceDatasetTuple.get(dataSource.name)),
									type,
									sourceDatasetTuple.get(sourceDataset.deleteTime) != null,
									sourceDatasetTuple.get(sourceDatasetVersion.confidential),
									sourceDatasetTuple.get(sourceDatasetVersion.wmsOnly),
									listNotifications,
									sourceDatasetTuple.get(sourceDataset.externalIdentification),
									sourceDatasetTuple.get(sourceDatasetVersion.physicalName))));
				});
				
				
			} else {
				return f.successful(Optional.empty());
			}
		});
	}
	
	private CompletableFuture<Optional<Metadata>> handleGetMetadata(GetMetadata query) {
		String sourceDatasetId = query.id();
		log.debug("fetching metadata of source dataset: {}", sourceDatasetId);
		
		return db.query().from(sourceDataset)
			.join(sourceDatasetMetadata).on(sourceDatasetMetadata.sourceDatasetId.eq(sourceDataset.id))
			.where(sourceDataset.identification.eq(sourceDatasetId))
			.singleResult(new QTuple(sourceDatasetMetadata.document)).thenApply(optionalResult -> 
				optionalResult.flatMap(result ->
					Optional.ofNullable(result.get(sourceDatasetMetadata.document))
						.map(content -> new Metadata(query.id(), content))));
	}
}
