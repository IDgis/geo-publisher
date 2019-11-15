package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QPublishedService.publishedService;
import static nl.idgis.publisher.database.QConstants.constants;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QEnvironment.environment;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QJob.job;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QServiceJob.serviceJob;
import static nl.idgis.publisher.database.QServiceKeyword.serviceKeyword;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.service.manager.QServiceStructure.serviceStructure;
import static nl.idgis.publisher.service.manager.QServiceStructure.withServiceStructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import akka.event.Logging;
import akka.event.LoggingAdapter;
import nl.idgis.publisher.database.AsyncSQLQuery;
import nl.idgis.publisher.database.QGenericLayer;
import nl.idgis.publisher.domain.query.ListEnvironments;
import nl.idgis.publisher.domain.query.ListServiceKeywords;
import nl.idgis.publisher.domain.query.ListServices;
import nl.idgis.publisher.domain.query.PerformPublish;
import nl.idgis.publisher.domain.query.PutServiceKeywords;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.QService;
import nl.idgis.publisher.domain.web.Service;
import nl.idgis.publisher.domain.web.ServicePublish;
import nl.idgis.publisher.mx.messages.PublicationServiceUpdate;
import nl.idgis.publisher.mx.messages.ServiceUpdateType;
import nl.idgis.publisher.mx.messages.StagingServiceUpdate;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.manager.messages.PublishService;

import akka.actor.ActorRef;
import akka.actor.Props;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.support.Expressions;
import com.mysema.query.types.Ops;
import com.mysema.query.types.expr.BooleanExpression;
import nl.idgis.publisher.service.manager.messages.PublishServiceResult;

public class ServiceAdmin extends AbstractAdmin {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	protected final static QGenericLayer child = new QGenericLayer("child"), parent = new QGenericLayer("parent");
	
	private final ActorRef serviceManager, messageBroker;
	
	public ServiceAdmin(ActorRef database, ActorRef serviceManager, ActorRef messageBroker) {
		super(database); 
		
		this.serviceManager = serviceManager;
		this.messageBroker = messageBroker;
	}
	
	public static Props props(ActorRef database, ActorRef serviceManager, ActorRef messageBroker) {
		return Props.create(ServiceAdmin.class, database, serviceManager, messageBroker);
	}

	@Override
	protected void preStartAdmin() {
		doList(Service.class, this::handleListServices);
		doGet(Service.class, this::handleGetService);
		doPut(Service.class, this::handlePutService);
		doDelete(Service.class, this::handleDeleteService);
		
		doQuery(ListEnvironments.class, this::handleListEnvironments);
		doQuery(PerformPublish.class, this::handlePerformPublish);
		
		doQuery(ListServiceKeywords.class, this::handleListServiceKeywords);
		doQuery(PutServiceKeywords.class, this::handlePutServiceKeywords);
		
		doQuery (ListServices.class, this::handleListServicesWithQuery);
	}

	private CompletableFuture<Boolean> handlePerformPublish(PerformPublish performPublish) {
		String serviceId = performPublish.getServiceId();
		return performPerformPublish(performPublish).thenApply(result -> {
			Optional<String> optionalPreviousEnvironmentId = result.getPreviousEnvironmentId();
			Optional<String> optionalCurrentEnvironmentId = result.getCurrentEnvironmentId();

			if (optionalCurrentEnvironmentId.isPresent()) {
				String currentEnvironmentId =  optionalCurrentEnvironmentId.get();
				if (optionalPreviousEnvironmentId.isPresent()) {
					String previousEnvironmentId = optionalPreviousEnvironmentId.get();

					if (!currentEnvironmentId.equals(previousEnvironmentId)) {
						messageBroker.tell(
								new PublicationServiceUpdate(ServiceUpdateType.REMOVE, serviceId, previousEnvironmentId),
								getSelf());
					}
				}

				messageBroker.tell(
						new PublicationServiceUpdate(ServiceUpdateType.CREATE, serviceId, currentEnvironmentId),
						getSelf());
			} else {
				optionalPreviousEnvironmentId.ifPresent(previousEnvironmentId ->
					messageBroker.tell(
							new PublicationServiceUpdate(ServiceUpdateType.REMOVE, serviceId, previousEnvironmentId),
							getSelf()));
			}

			return true;
		}).exceptionally(t -> {
			log.error(t, "failed to publish service");
			return false;
		});
	}
	
	private CompletableFuture<PublishServiceResult> performPerformPublish(PerformPublish performPublish) {
		return f.ask(
			serviceManager, 
			new PublishService(
				performPublish.getServiceId(), 
				performPublish.getEnvironmentId()),
			PublishServiceResult.class);
	}

	private CompletableFuture<Page<Service>> handleListServices () {
		return handleListServicesWithQuery (new ListServices (null, null, null));
	}
	
	private CompletableFuture<Page<ServicePublish>> handleListEnvironments(ListEnvironments listEnvironments) {
		String serviceId = listEnvironments.getServiceId();
		final Page.Builder<ServicePublish> builder = new Page.Builder<> ();
		
		BooleanExpression inUse = new SQLSubQuery().from(publishedService)
			.join(service).on(service.id.eq(publishedService.serviceId))
			.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
			.where(genericLayer.identification.eq(serviceId))
			.where(environment.id.eq(publishedService.environmentId))
			.exists();
		
		return db.query().from(environment)
			.orderBy(environment.name.asc())
			.list(
					environment.identification,
					environment.name,
					inUse,
					environment.confidential,
					environment.wmsOnly
					).thenApply(publish -> {
						for (Tuple service : publish.list()) {
							builder.add(new ServicePublish(
									serviceId,
									service.get(environment.identification),
									service.get(environment.name),
									service.get(inUse),
									service.get(environment.confidential),
									service.get(environment.wmsOnly)
								));
						}
						return builder.build();
					});
	}
	
	private BooleanExpression isConfidential () {
		return new SQLSubQuery ()
			.from (serviceStructure)
			.join (leafLayer).on (serviceStructure.childLayerId.eq (leafLayer.genericLayerId))
			.join (dataset).on (leafLayer.datasetId.eq (dataset.id))
			.join (sourceDataset).on (dataset.sourceDatasetId.eq (sourceDataset.id))
			.where (serviceStructure.serviceIdentification.eq (genericLayer.identification))
			.where (Expressions.booleanOperation (Ops.EQ, Expressions.constant (true), new SQLSubQuery ().from (sourceDatasetVersion).where (sourceDatasetVersion.sourceDatasetId.eq (sourceDataset.id)).orderBy (sourceDatasetVersion.createTime.desc ()).limit (1).list (sourceDatasetVersion.confidential)))
			.exists ();
	}
	
	private BooleanExpression isWmsOnly () {
		return new SQLSubQuery ()
			.from (serviceStructure)
			.join (leafLayer).on (serviceStructure.childLayerId.eq (leafLayer.genericLayerId))
			.join (dataset).on (leafLayer.datasetId.eq (dataset.id))
			.join (sourceDataset).on (dataset.sourceDatasetId.eq (sourceDataset.id))
			.where (serviceStructure.serviceIdentification.eq (genericLayer.identification))
			.where (Expressions.booleanOperation (Ops.EQ, Expressions.constant (true), new SQLSubQuery ().from (sourceDatasetVersion).where (sourceDatasetVersion.sourceDatasetId.eq (sourceDataset.id)).orderBy (sourceDatasetVersion.createTime.desc ()).limit (1).list (sourceDatasetVersion.wmsOnly)))
			.exists ();
	}
	
	private BooleanExpression isPublished () {		
		return new SQLSubQuery ()			
			.from(publishedService)			
			.where(service.id.eq(publishedService.serviceId))
			.exists().as("isPublished");
	}
	
	private CompletableFuture<Page<Service>> handleListServicesWithQuery (final ListServices listServices) {
		final AsyncSQLQuery baseQuery = db
				.query()
				.from(service)
				.leftJoin(genericLayer).on(service.genericLayerId.eq(genericLayer.id))
				.leftJoin(constants).on(service.constantsId.eq(constants.id))
				.leftJoin(publishedService).on(service.id.eq(publishedService.serviceId))
				.distinct()
				.orderBy (genericLayer.name.asc ());
		
		// Add a filter for the query string:
		if (listServices.getQuery () != null) {
			baseQuery.where (genericLayer.name.containsIgnoreCase (listServices.getQuery ())
					.or (genericLayer.title.containsIgnoreCase (listServices.getQuery ()))
				);
		}
		
		// Add a filter for the published flag:
		if (listServices.getIsPublished () != null) {
			baseQuery.where (publishedService.serviceId.isNotNull().eq(listServices.getIsPublished ()));
		}
		
		final AsyncSQLQuery listQuery = baseQuery.clone ();
		
		singlePage (listQuery, listServices.getPage ());
		
		withServiceStructure (listQuery, parent, child);
		
		return baseQuery
				.count ()
				.thenCompose ((count) -> {
					final Page.Builder<Service> builder = new Page.Builder<> ();
					
					addPageInfo (builder, listServices.getPage (), count);
					
					return listQuery
						.list (new QService(
								genericLayer.identification,
								genericLayer.name,
								genericLayer.title, 
								service.alternateTitle, 
								genericLayer.abstractCol,
								service.metadata,
								genericLayer.identification,
								constants.identification,
								service.wfsMetadataFileIdentification,
								service.wmsMetadataFileIdentification,
								isConfidential (),
								isWmsOnly (),
								isPublished ()
							))
						.thenApply ((styles) -> {
							builder.addAll (styles.list ());
							return builder.build ();
						});
				});
	}
	
	private CompletableFuture<Optional<Service>> handleGetService (String serviceId) {
		log.debug ("handleGetService: " + serviceId);
		
		
		return 
			withServiceStructure (db.query (), parent, child).from(service)
			.leftJoin(genericLayer).on(service.genericLayerId.eq(genericLayer.id))
			.leftJoin(constants).on(service.constantsId.eq(constants.id))
			.where(genericLayer.identification.eq(serviceId))
			.singleResult(new QService(
					genericLayer.identification,
					genericLayer.name,
					genericLayer.title, 
					service.alternateTitle, 
					genericLayer.abstractCol,
					service.metadata,
					genericLayer.identification,
					service.wfsMetadataFileIdentification,
					service.wmsMetadataFileIdentification,
					constants.identification,
					isConfidential (),
					isWmsOnly (),
					isPublished ()
			));		
	}

	private CompletableFuture<Response<?>> handlePutService(Service theService) {
		return performPutService(theService).whenComplete( (response, throwable) -> {
			if (response != null && response.getOperationResponse() == CrudResponse.OK) {
				log.debug("sending 'create' update notification to message broker");
				messageBroker.tell(new StagingServiceUpdate(ServiceUpdateType.CREATE, theService.id()), getSelf());
			}
		});
	}
	
	private CompletableFuture<Response<?>> performPutService(Service theService) {
		String serviceId = theService.id();
		String serviceName = theService.name();
		log.debug ("handle update/create service: " + serviceId);
		
		return db.transactional(tx ->
			// Check if there is another service with the same id
			tx.query().from(service)
			.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
			.where(genericLayer.identification.eq(serviceId))
			.singleResult(genericLayer.identification)
			.thenCompose(msg -> {
				if (!msg.isPresent()){
					// INSERT generic Layer (rootgroup)
					String newGenericLayerId = UUID.randomUUID().toString();
					return tx.insert(genericLayer)
						.set(genericLayer.identification, newGenericLayerId)
						.set(genericLayer.name, serviceName)
						.set(genericLayer.title, theService.title())
						.set(genericLayer.abstractCol, theService.abstractText())
						.execute()
						.thenCompose(n -> {
							return tx.query().from(genericLayer)
								.where(genericLayer.identification.eq(newGenericLayerId))
								.singleResult(genericLayer.id)
								.thenCompose(glId -> {
									return tx.query().from(constants)
										.singleResult(constants.id)
										.thenCompose(cId -> {
											if (glId.isPresent()){
												// INSERT service
												log.debug("Inserting new service with name: " + serviceName + ", ident: "
														+ newGenericLayerId);
												return tx.insert(service)
													.set(service.alternateTitle, theService.alternateTitle())
													.set(service.metadata, theService.metadata())
													.set(service.wfsMetadataFileIdentification, UUID.randomUUID().toString())
													.set(service.wmsMetadataFileIdentification, UUID.randomUUID().toString())
													.set(service.genericLayerId, glId.get())
													.set(service.constantsId, cId.get())
													.execute()
													.thenApply(l -> new Response<String>(CrudOperation.CREATE, CrudResponse.OK, newGenericLayerId));
											} else {
												//ERROR
												return f.successful(new Response<String>(CrudOperation.CREATE, CrudResponse.NOK, serviceName));
											}
										});
							});
						});
				} else {
					return tx.query().from(genericLayer)
						.where(genericLayer.identification.eq(theService.genericLayerId()))
						.singleResult(genericLayer.id)
						.thenCompose(glId -> {
							if (glId.isPresent()){
								// UPDATE generic Layer (rootgroup)
								return tx.update(genericLayer)
									.set(genericLayer.name, serviceName)
									.set(genericLayer.title, theService.title())
									.set(genericLayer.abstractCol, theService.abstractText())
									.where(genericLayer.id.eq(glId.get()))
									.execute()
									.thenCompose(n -> {
										// UPDATE service
										log.debug("Updating service with name: " + serviceName);
										return tx.update(service)
											.set(service.alternateTitle, theService.alternateTitle())
											.set(service.metadata, theService.metadata())
											.where(service.genericLayerId.eq(glId.get()))
											.execute()
											.thenApply(l -> new Response<String>(CrudOperation.UPDATE, CrudResponse.OK, serviceId));
									});
							} else {
								//ERROR
								return f.successful(new Response<String>(CrudOperation.UPDATE, CrudResponse.NOK, serviceName));
							}
						});
				}
		}));
	}

	private CompletableFuture<Response<?>> handleDeleteService(String serviceId) {
		return performDeleteService(serviceId).whenComplete( (response, throwable) -> {
			if (response != null && response.getOperationResponse() == CrudResponse.OK) {
				log.debug("sending 'remove' update notification to message broker");
				messageBroker.tell(new StagingServiceUpdate(ServiceUpdateType.REMOVE, serviceId), getSelf());
			}
		});
	}

	private CompletableFuture<Response<?>> performDeleteService(String serviceId) {
		log.debug ("handleDeleteService: " + serviceId);
		return db.transactional(tx ->
				tx.query().from(service)
				.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
				.where(genericLayer.identification.eq(serviceId))
				.singleResult(service.id)
				.thenCompose(svId -> {
					log.debug("delete jobs for service id: {}", svId.get());
					return tx.delete(job)
						.where(new SQLSubQuery().from(serviceJob)
								.where(serviceJob.serviceId.eq(svId.get())
									.and(job.id.eq(serviceJob.jobId)))
								.exists())
						.execute()
						.thenCompose(jobs -> {
							log.debug("delete service id: {}", svId.get());
							return tx.delete(service)
								.where(service.id.eq(svId.get()))
								.execute()
								.thenCompose(n -> {
									log.debug("delete generic layer id: {}", serviceId);
									return tx.delete(genericLayer)
										.where(genericLayer.identification.eq(serviceId))
										.execute()
										.thenApply(l -> 
											new Response<String>(
												CrudOperation.DELETE, 
												CrudResponse.OK, 
												serviceId));
									});
							});
					}));

	}
	
	
	
	private CompletableFuture<List<String>> handleListServiceKeywords(ListServiceKeywords listServiceKeywords) {
		final String serviceId = listServiceKeywords.serviceId();
		log.debug ("handleListServiceKeywords: " + serviceId);
		
		return db.transactional(
			tx ->
			tx.query().from(service)
			.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
			.where(genericLayer.identification.eq(serviceId))
			.singleResult(service.id)
			.thenCompose(
				svId -> {
				log.debug("service id: " + svId.get());
				return tx.query()
					.from(serviceKeyword)
					.where(serviceKeyword.serviceId.eq(svId.get()))
					.list(serviceKeyword.keyword)
					.thenApply(resp -> resp.list());
			})
		);
	}
	
	private CompletableFuture<Response<?>> handlePutServiceKeywords(PutServiceKeywords putServiceKeywords) {
		final String serviceId = putServiceKeywords.serviceId();
		final List<String> serviceKeywords = putServiceKeywords.keywordList();
		
		log.debug ("handlePutServiceKeywords: " + serviceId);
		return db.transactional(
			tx ->
			tx.query().from(service)
			.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
			.where(genericLayer.identification.eq(serviceId))
			.singleResult(service.id)
			.thenCompose(
				svId -> {
				// A. delete the existing keywords of this service
				log.debug("service id: " + svId.get());
				return tx.delete(serviceKeyword)
					.where(serviceKeyword.serviceId.eq(svId.get()))
					.execute()
					.thenCompose(
						skNr -> {
						// B. insert items of service keywords	
						return f.sequence(
							serviceKeywords.stream()
							    .map(name -> 
							    	tx.insert(serviceKeyword)
						            .set(serviceKeyword.serviceId, svId.get()) 
				            		.set(serviceKeyword.keyword, name)
						            .execute())
							    .collect(Collectors.toList())).thenApply(whatever ->
							        new Response<String>(CrudOperation.CREATE,
							                CrudResponse.OK, serviceId));
						});
//					.thenCompose(
//						n -> {
//						log.debug("delete keywords: #" + n);
//						return tx
//							.insert(serviceKeyword)							
//							.set(serviceKeyword.serviceId, svId.get())
//							.set(serviceKeyword.keyword, "abcdef")
//							.execute()
//							.thenApply(resp -> new Response<String>(CrudOperation.CREATE, CrudResponse.OK, serviceId));
//						});
				})
		);		
		
	}	
}
