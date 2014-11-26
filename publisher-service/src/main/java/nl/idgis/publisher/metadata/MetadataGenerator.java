package nl.idgis.publisher.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.typesafe.config.Config;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;
import akka.util.Timeout;

import scala.concurrent.Future;
import scala.runtime.AbstractFunction1;
import scala.runtime.AbstractFunction2;

import nl.idgis.publisher.database.DatabaseRef;

import nl.idgis.publisher.metadata.messages.GenerateMetadata;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.messages.GetContent;
import nl.idgis.publisher.service.messages.Layer;
import nl.idgis.publisher.service.messages.ServiceContent;
import nl.idgis.publisher.service.messages.VirtualService;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;

import nl.idgis.publisher.database.QServiceJob;
import nl.idgis.publisher.database.QJobState;

import nl.idgis.publisher.domain.job.JobState;

import nl.idgis.publisher.harvester.messages.GetDataSource;
import nl.idgis.publisher.harvester.sources.messages.GetDatasetMetadata;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QServiceJob.serviceJob;
import static nl.idgis.publisher.database.QJobState.jobState;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QCategory.category;

public class MetadataGenerator extends UntypedActor {
	
	private static final String ENDPOINT_CODE_LIST_VALUE = "WebServices";

	private static final String ENDPOINT_CODE_LIST = "http://www.isotc211.org/2005/iso19119/resources/Codelist/gmxCodelists.xml#DCPList";

	private static final String ENDPOINT_OPERATION_NAME = "GetCapabilitities";

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef service, harvester;
	
	private final DatabaseRef database;
	
	private final MetadataStore serviceMetadataSource, datasetMetadataTarget, serviceMetadataTarget;
	
	private final Config constants;
	
	private FutureUtils f;
	
	public MetadataGenerator(ActorRef database, ActorRef service, ActorRef harvester, MetadataStore serviceMetadataSource, MetadataStore datasetMetadataTarget, MetadataStore serviceMetadataTarget, Config constants) {
		this.database = new DatabaseRef(database, Timeout.apply(15, TimeUnit.SECONDS), getContext().dispatcher(), log);
		this.service = service;
		this.harvester = harvester;
		this.serviceMetadataSource = serviceMetadataSource;
		this.datasetMetadataTarget = datasetMetadataTarget;
		this.serviceMetadataTarget = serviceMetadataTarget;
		this.constants = constants;
	}
	
	public static Props props(ActorRef database, ActorRef service, ActorRef harvester, MetadataStore serviceMetadataSource, MetadataStore datasetMetadataTarget, MetadataStore serviceMetadataTarget, Config constants) {
		return Props.create(MetadataGenerator.class, database, service, harvester, serviceMetadataSource, datasetMetadataTarget, serviceMetadataTarget, constants);
	}
	
	@Override
	public void preStart() throws Exception {		
		f = new FutureUtils(getContext().dispatcher());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof GenerateMetadata) {
			generateMetadata();
		} else {
			unhandled(msg);
		}
	}

	private void generateMetadata() {		
		log.debug("generating metadata");
		
		getContext().become(new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				log.debug("busy");
				
				unhandled(msg);
			}
			
		});
		
		final ActorRef sender = getSender();
		
		QServiceJob otherServiceJob = new QServiceJob("otherServiceJob");
		QJobState otherJobState = new QJobState("otherJobState");
		
		f.collect(
			database.query()
				.from(serviceJob)
				.join(dataset).on(dataset.id.eq(serviceJob.datasetId))
				.join(sourceDatasetVersion).on(sourceDatasetVersion.id.eq(serviceJob.sourceDatasetVersionId))
				.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
				.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
				.join(category).on(category.id.eq(sourceDatasetVersion.categoryId))			
				.join(jobState).on(
						jobState.jobId.eq(serviceJob.jobId)
					.and(
						jobState.state.eq(JobState.SUCCEEDED.name())))			
				.where(new SQLSubQuery()
					.from(otherServiceJob)
					.join(otherJobState).on(
							otherJobState.jobId.eq(otherServiceJob.jobId)
						.and(
							otherJobState.state.eq(JobState.SUCCEEDED.name())))
					.where(
							otherServiceJob.datasetId.eq(serviceJob.datasetId)
						.and(
							otherJobState.createTime.after(jobState.createTime)))
					.notExists())
				.list(
						dataSource.identification,
						sourceDataset.identification,
						category.identification, 
						dataset.identification,
						dataset.uuid))
			.collect(
				f.ask(service, new GetContent(), ServiceContent.class))		
			.result(new AbstractFunction2<TypedList<Tuple>, ServiceContent, Void>() {

				@Override
				public Void apply(final TypedList<Tuple> queryResult, final ServiceContent serviceContent) {
					log.debug("queryResult and serviceContent collected");
					
					Future<Map<String, ActorRef>> dataSources = getDataSources(queryResult);
					
					f					
						.collect(getMetadataDocuments(dataSources, queryResult))
						.result(new AbstractFunction1<Map<String, MetadataDocument>, Void>() {

							@Override
							public Void apply(Map<String, MetadataDocument> metadataDocuments) {
								log.debug("metadata documents collected");
								
								Map<String, Set<Dataset>> operatesOn = new HashMap<>();
								
								List<Future<Void>> pendingWork = new ArrayList<Future<Void>>();
								for(Tuple item : queryResult) {
									String sourceDatasetId = item.get(sourceDataset.identification);
									String datasetId = item.get(dataset.identification);
									String datasetUuid = item.get(dataset.uuid);
									
									MetadataDocument metadataDocument = metadataDocuments.get(sourceDatasetId);
									
									pendingWork.add(processDataset(operatesOn, metadataDocument, datasetId, datasetUuid, item.get(category.identification), datasetId, serviceContent));
								}
								
								for(Entry<String, Set<Dataset>> operatesOnEntry : operatesOn.entrySet()) {
									String serviceName = operatesOnEntry.getKey();
									Set<Dataset> serviceOperatesOn = operatesOnEntry.getValue();
									
									Future<MetadataDocument> metadataDocument = serviceMetadataSource.get(serviceName, getContext().dispatcher());
									pendingWork.add(processService(serviceName, metadataDocument, serviceOperatesOn));
								}
								
								Futures.sequence(pendingWork, getContext().dispatcher())
									.onComplete(new OnComplete<Iterable<Void>>() {

										@Override
										public void onComplete(Throwable t, Iterable<Void> i) throws Throwable {
											getContext().unbecome();
											
											sender.tell(new Ack(), getSelf());
											
											if(t != null) {
												log.error("metadata generation failed: {}", t);
											} else {											
												log.debug("metadata generated");	
											}
										}
										
									}, getContext().dispatcher());
								
								return null;
							}
							
						});
					
					return null;
				}
				
			});
	}

	private Future<Map<String, ActorRef>> getDataSources(TypedList<Tuple> queryResult) {
		Map<String, Future<ActorRef>> dataSources = new HashMap<String, Future<ActorRef>>();
		
		for(Tuple item : queryResult) {
			log.debug(item.toString());
			
			String dataSourceId = item.get(dataSource.identification);
			if(!dataSources.containsKey(dataSourceId)) {
				log.debug("fetching dataSource: " + dataSourceId);
				
				dataSources.put(dataSourceId, f.ask(harvester, new GetDataSource(dataSourceId), ActorRef.class));
			}
		}
		
		return f.map(dataSources);	
	}
	
	private Future<Void> processService(final String serviceName, Future<MetadataDocument> metadataDocument, final Set<Dataset> operatesOn) {
		return metadataDocument.flatMap(new Mapper<MetadataDocument, Future<Void>>() {
			
			@Override
			public Future<Void> apply(MetadataDocument metadataDocument) {
				return processService(serviceName, metadataDocument, operatesOn);
			}
			
		}, getContext().dispatcher());
	}
	
	private Future<Void> processWFS(String serviceName, MetadataDocument metadataDocument, Set<Dataset> operatesOn) {
		try {
			final String linkage = constants.getString("onlineResource.wfs") ;
			
			metadataDocument.addServiceType("OGC:WFS");
			metadataDocument.addServiceEndpoint(ENDPOINT_OPERATION_NAME, ENDPOINT_CODE_LIST, ENDPOINT_CODE_LIST_VALUE, linkage);
			
			for(Dataset dataset : operatesOn) {
				final String uuid = dataset.getUuid();
				final String layerName = dataset.getLayerName();
				final String scopedName = layerName;
			
				metadataDocument.addServiceLinkage(linkage, "OGC:WFS", layerName);
				metadataDocument.addSVCoupledResource("GetFeature", uuid, scopedName);
			}
		
			return serviceMetadataTarget.put(serviceName + "-wfs", metadataDocument, getContext().dispatcher());
		} catch(Exception e) {
			return Futures.failed(e);
		}
	}
	
	private Future<Void> processWMS(String serviceName, MetadataDocument metadataDocument, Set<Dataset> operatesOn) {
		try {
			final String linkage = constants.getString("onlineResource.wms") ;			
			final String browseGraphicBaseUrl = linkage 
					+ "request=GetMap&Service=WMS&SRS=EPSG:28992&CRS=EPSG:28992"
					+ "&Bbox=180000,459000,270000,540000&Width=600&Height=662&Format=image/png&Styles=";
			
			metadataDocument.addServiceType("OGC:WMS");
			metadataDocument.addServiceEndpoint(ENDPOINT_OPERATION_NAME, ENDPOINT_CODE_LIST, ENDPOINT_CODE_LIST_VALUE, linkage);
			
			for(final Dataset dataset : operatesOn) {
				final String uuid = dataset.getUuid();
				final String layerName = dataset.getLayerName();
				final String scopedName = layerName;
				
				metadataDocument.addBrowseGraphic(browseGraphicBaseUrl + "&layers=" + layerName);
				metadataDocument.addServiceLinkage(linkage, "OGC:WMS", layerName);
				metadataDocument.addSVCoupledResource("GetMap", uuid, scopedName); 
			}
		
			return serviceMetadataTarget.put(serviceName + "-wms", metadataDocument, getContext().dispatcher());
		} catch(Exception e) {
			return Futures.failed(e);
		}
	}
	
	private Future<Void> processService(final String serviceName, MetadataDocument metadataDocument, Set<Dataset> operatesOn) {
		try {
			metadataDocument.removeOperatesOn();
			
			String href = constants.getString("operatesOn.href");
			log.debug("service href: " + href);
		
			for (Dataset dataset : operatesOn) {
				String uuid = dataset.getUuid();
				String uuidref = href + uuid;
				log.debug("service operatesOn uuidref: " + uuidref);
				
				metadataDocument.addOperatesOn(uuid, uuidref);			
			}

			metadataDocument.removeServiceType();
			metadataDocument.removeServiceEndpoint();			
			metadataDocument.removeBrowseGraphic();
			metadataDocument.removeServiceLinkage();
			metadataDocument.removeSVCoupledResource();
			
			return Futures.sequence(Arrays.asList(
					processWFS(serviceName, metadataDocument.clone(), operatesOn),
					processWMS(serviceName, metadataDocument.clone(), operatesOn)),
					
					getContext().dispatcher())
					
					.map(new Mapper<Iterable<Void>, Void>() {

						@Override
						public Void apply(Iterable<Void> i) {
							log.debug("service processed: " + serviceName);
							
							return null;
						}
						
					}, getContext().dispatcher());
		} catch(Exception e) {
			return Futures.failed(e);
		}
	}
	
	private Future<Void> processDataset(Map<String, Set<Dataset>> operatesOn, MetadataDocument metadataDocument, String datasetId, String datasetUuid, String schemaName, String tableName, ServiceContent serviceContent) {
		try {
			metadataDocument.removeServiceLinkage();		
			
			for(VirtualService service : serviceContent.getServices()) {
				String serviceName = service.getName();
				
				for(Layer layer : service.getLayers()) {
					if(schemaName.equals(layer.getSchemaName()) && 
						tableName.equals(layer.getTableName())) {
						
						log.debug("layer for dataset " + datasetUuid + " found (table: " + schemaName + "." + tableName + ": " + layer.getName() + " , service: " + serviceName + ")");
						
						final Set<Dataset> serviceOperatesOn;
						if(operatesOn.containsKey(serviceName)) {
							serviceOperatesOn = operatesOn.get(serviceName);						
						} else {
							serviceOperatesOn = new HashSet<>();
							operatesOn.put(serviceName, serviceOperatesOn);
						}
						
						String layerName = layer.getName();
						serviceOperatesOn.add(new Dataset(datasetUuid, layerName));
						
						// WMS
						String linkage = constants.getString("onlineResource.wms") ;
						String protocol = "OGC:WMS";
						
						metadataDocument.addServiceLinkage(linkage, protocol, layerName);					
						
						// WFS
						linkage = constants.getString("onlineResource.wfs") ;
						protocol = "OGC:WFS";						
						
						metadataDocument.addServiceLinkage(linkage, protocol, layerName);
					}
				}
			}
			
			log.debug("dataset processed: " + datasetId);
			
			return datasetMetadataTarget.put(datasetUuid, metadataDocument, getContext().dispatcher());
		} catch(Exception e) {
			return Futures.failed(e);
		}
	}
	
	private Future<Map<String, MetadataDocument>> getMetadataDocuments(Future<Map<String, ActorRef>> dataSources, final TypedList<Tuple> queryResult) {
		return f.flatMap(dataSources, new Mapper<Map<String, ActorRef>, Future<Map<String, MetadataDocument>>>() {
			
			public Future<Map<String, MetadataDocument>> apply(Map<String, ActorRef> dataSources) {
				log.debug("dataSources collected");
				
				return getMetadataDocuments(dataSources, queryResult);
			}
		});
	}
	
	private Future<Map<String, MetadataDocument>> getMetadataDocuments(Map<String, ActorRef> dataSources, TypedList<Tuple> queryResult) {
		Map<String, Future<MetadataDocument>> metadataDocuments = new HashMap<String, Future<MetadataDocument>>();
		
		for(Tuple item : queryResult) {
			String sourceDatasetId = item.get(sourceDataset.identification);
			String dataSourceId = item.get(dataSource.identification);
			
			log.debug("fetching metadata: " + sourceDatasetId);
			
			ActorRef dataSource = dataSources.get(dataSourceId);
			log.debug("dataSource: " + dataSource);
			
			metadataDocuments.put(sourceDatasetId, f.ask(dataSource, new GetDatasetMetadata(sourceDatasetId), MetadataDocument.class));
		}
		
		return f.map(metadataDocuments);
	}
}
