package controllers;

import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetMetadata.sourceDatasetMetadata;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QPublishedService.publishedService;
import static nl.idgis.publisher.database.QEnvironment.environment;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;

import play.mvc.*;
import util.MetadataConfig;
import util.QueryDSL;
import play.api.Environment;
import play.libs.Json;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.mysema.query.Tuple;
import com.mysema.query.types.path.PathBuilder;

import nl.idgis.publisher.database.QPublishedServiceDataset;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.xml.exceptions.NotFound;

public class DatasetMetadataDCAT extends Controller{

	/*
	 * This class generates json with metadata according to the dcat specifications. 
	 * 
	 * Gebruik alleen gepubliceerde datasets
	 * 1 bestand met alle datasets erin.
	 * Voorbeeld: https://geoportaal-ddh.opendata.arcgis.com/data.json
	 * Referentie: https://www.w3.org/TR/vocab-dcat/
	 * ISO 19115 -> DCAT Mapping: https://www.w3.org/2015/spatial/wiki/ISO_19115_-_DCAT_-_Schema.org_mapping
	 * CKAN - DCAT Mapping: https://github.com/ckan/ckanext-dcat#rdf-dcat-to-ckan-dataset-mapping
	 * 
	 */
	private final MetadataConfig mdc;
	private final MetadataDocumentFactory mdf;
	private final QueryDSL q;
	private final DatasetQueryBuilder dqb;

	private final Map<String, String[]> distributionsTypes;
	private final String language = "dut";

	private PathBuilder<String> publishedServiceDatasetLayerNamePath = new PathBuilder<> (String.class, "publishedServiceDatasetLayerNamePath");
	private PathBuilder<String> genericLayerName = new PathBuilder<> (String.class, "genericLayerName");
		
	@Inject
	public DatasetMetadataDCAT(MetadataConfig mdc, QueryDSL q, DatasetQueryBuilder dqb, MetadataDocumentFactory mdf) {
		this.mdc = mdc;
		this.q = q;
		this.dqb = dqb;
		this.mdf = mdf;

		// Distributions
		// some info over the distributions formats
		Map<String, String[]> distributionsTypes = new HashMap<>(); 
		
		/* Create a map with name, outputFormat and mime type
		 * For GML it should be "Application/gml+xml" with a version indication.
		 * See: http://portal.opengeospatial.org/files/?artifact_id=37743
		 * http://docs.geoserver.org/latest/en/user/services/wfs/webadmin.html
		 * http://docs.geoserver.org/latest/en/user/services/wfs/outputformats.html
		 */
		
		distributionsTypes.put("GML2", new String[] {"GML2", "text/xml; subtype=gml/2.1.2"}); // Advertised in getCapabilities 
		// distributionsTypes.put("GML2", new String[] {"GML2", "application/gml+xml; version=2.1"});
		distributionsTypes.put("GML3", new String[] {"GML32", "application/gml+xml; version=3.2"}); // GML3 returns gml version 3.1.1
		distributionsTypes.put("SHAPE-ZIP", new String[] {"shape-zip", "application/zip"});
		distributionsTypes.put("GeoJSON", new String[] {"application/json", "application/vnd.geo+json"});
		distributionsTypes.put("CSV", new String[] {"csv", "text/csv"});
		
		distributionsTypes.put("KML", new String[] {"kml", "application/vnd.google-earth.kml+xml"});
		
		this.distributionsTypes = Collections.unmodifiableMap(distributionsTypes);
	}

	/*
	 * Retrieves all published datasets from the database.
	 * Tables included are:
	 * dataset, published_services_dataset, service, generic_layer, source_dataset_version and source_dataset
	 */
	private List<Tuple> getPublishedDatasets() {
		final QPublishedServiceDataset publishedServiceDataset2 = new QPublishedServiceDataset("published_service_dataset2");

		return q.withTransaction(tx -> 
			dqb.fromPublishedDataset(tx)
				.join(publishedServiceDataset2).on(publishedServiceDataset2.datasetId.eq(dataset.id))
				.join(service).on(service.id.eq(publishedServiceDataset2.serviceId))
				.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
				.join(publishedService).on(publishedService.serviceId.eq(publishedServiceDataset2.serviceId))
				.join(environment).on(environment.id.eq(publishedService.environmentId))
				.where(environment.confidential.eq(false)
						.and(environment.wmsOnly.eq(false))
						.and(sourceDatasetVersion.type.eq("VECTOR")))
				.groupBy(dataset.id, dataset.identification, dataset.metadataFileIdentification, dataset.name, sourceDatasetVersion.confidential, sourceDatasetMetadata.document, environment.url)
				.orderBy(dataset.id.asc())
				.list(dataset.id, dataset.identification, dataset.metadataFileIdentification, dataset.name, sourceDatasetVersion.confidential, sourceDatasetMetadata.document, publishedServiceDataset2.layerName.min().as(publishedServiceDatasetLayerNamePath), genericLayer.name.min().as(genericLayerName), environment.url)
			);
	}

	// Fetch all published datasets and generate a hashmap for each. 
	private Map<String, Object> buildDcatResult() {
		// The mapping which will be converted to JSON
		Map<String, Object> dcatResult = new HashMap<>();

		/* The first four items on a DCAT/JSON  are standard. 
		 * They are filled in here
		 * See: https://project-open-data.cio.gov/v1.1/schema
		 * and: https://geoportaal-ddh.opendata.arcgis.com/data.json.
		 */

		String conformsTo = "https://project-open-data.cio.gov/v1.1/schema";
		String context = "https://project-open-data1/schema/catalog.jsonld";
		String type = "dcat:Catalog";
		String describedBy = "https://project-open-data.cio.gov/v1.1/schema/catalog.json";

		dcatResult.put("conformsTo", conformsTo);
		dcatResult.put("@context", context);
		dcatResult.put("@type", type);
		dcatResult.put("describedBy", describedBy);	

		// A list of all datasets.
		List<Object> datasetsDcat = new ArrayList<>();

		// get all published datasets
		List<Tuple> datasets = getPublishedDatasets();

		// Loop over all dataset to generate correct dcat metadata
		for (Tuple ds : datasets) {
			datasetsDcat.add(mapDcat(ds));		
		}

		// Add hashmap with all datasets to our result
		dcatResult.put("dataset", datasetsDcat);

		return dcatResult;
	}

	private Map<String, Object> mapDcat(Tuple ds) {
		final QPublishedServiceDataset publishedServiceDataset2 = new QPublishedServiceDataset("published_service_dataset2");

		Map<String, Object> resultDataset = new HashMap<>();
		List<Object> distributions = new ArrayList<>();

		/*
		 * For the GetFeature URL the nameSpace is the service name. It is stored in generic_layer.layerName. The typeName is stored in published_service_dataset.layerName
		 * The UUID for the landingspage URL is the metadata for this dataset contained in the xml.
		 * The UUID is from dataset.metadata_file_identification
		 */ 

		String baseServiceUrl = ds.get(environment.url);
		String baseMetadataUrl = mdc.getMetadataUrlPrefix();
		
		String getFeatureURl = "/wfs?service=wfs&version=2.0.0&request=GetFeature&typeName=";

		String metadataIdent = ds.get(dataset.metadataFileIdentification);

		String typeName = ds.get(publishedServiceDatasetLayerNamePath).replaceAll(" ", "_");
		String nameSpace = ds.get(genericLayerName).replaceAll(" ", "_");

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		resultDataset.put("@type", "dcat:Dataset");
		
		resultDataset.put("landingspage", baseMetadataUrl+"dataset/"+metadataIdent+".xml");
		resultDataset.put("accessLevel", ds.get(sourceDatasetVersion.confidential) ? "confidential" : "public");
		resultDataset.put("language", language);

		for (Map.Entry<String, String[]> entry : distributionsTypes.entrySet()) {
			HashMap<String, String> distribution = new HashMap<>();
			distribution.put("@type", "dcat:Distribution");
			distribution.put("title", entry.getKey());
			distribution.put("format", entry.getKey());
			distribution.put("mediaType", entry.getValue()[1]);
			distribution.put("downloadURL", baseServiceUrl+nameSpace+getFeatureURl+typeName+"&outputFormat="+entry.getValue()[0]);

			distributions.add(distribution);
		}
		resultDataset.put("distribution", distributions);

		// All info from XML
		try {
			MetadataDocument metadataDocument = mdf.parseDocument(ds.get(sourceDatasetMetadata.document));

			try {
				resultDataset.put("title", metadataDocument.getDatasetTitle());
			} catch (NotFound nf) {
				resultDataset.put("title",  null);
			}
			try {
				resultDataset.put("description", metadataDocument.getDatasetAbstract());
			} catch (NotFound nf) {
				resultDataset.put("description", null);
			}

			try {
				Date pubDate = metadataDocument.getDatasetPublicationDate();
				resultDataset.put("issued", sdf.format(pubDate));
			} catch (NotFound nf) {

				resultDataset.put("issued", null);
			}

			try {
				Date modDate = metadataDocument.getDatasetRevisionDate();
				resultDataset.put("modified", sdf.format(modDate));
			} catch (NotFound nf) {

				resultDataset.put("modified", null);
			}

			try {
				resultDataset.put("identifier", metadataDocument.getDatasetIdentifier());
			} catch (NotFound nf) {
				resultDataset.put("identifier", null);
			}

			try {
				resultDataset.put("rights", metadataDocument.getUseLimitations());
			} catch (NotFound nf) {
				resultDataset.put("rights", null);
			}
			
			try {
				String begin = metadataDocument.getDatasetTemporalExtentBegin();
				String end = metadataDocument.getDatasetTemporalExtentEnd();
				
				if (!begin.equals("") && !end.equals("")) {
					resultDataset.put("temporal", begin + "/" + end);
				} else {
					resultDataset.put("temporal", null);
				}
			} catch (NotFound nf) {
				resultDataset.put("temporal", null);
			}
			
			try {
				/*
				 * There could be several constraints.
				 * Pick the first one not containing "geen beperkingen" or "geen beperking"
				 */
				List<String> otherConstraints = new ArrayList<>();
				otherConstraints.addAll(metadataDocument.getOtherConstraints());
				String constraint = null;
				
				for (String c : otherConstraints) {
					if ("geen beperkingen".equals(c.toLowerCase().trim()) || "geen beperking".equals(c.toLowerCase().trim())) {
						continue;
					} else {
						constraint = c;
						break;
					}
					
				}
				resultDataset.put("license", constraint);
			} catch (NotFound nf) {
				resultDataset.put("license", null);
			}
			
			// Keyword
			List<String> keywords = new ArrayList<>();
			try {
				keywords.addAll(metadataDocument.getTopicCategories());
				resultDataset.put("keyword", keywords);
			} catch (NotFound nf) {
				resultDataset.put("keyword", null);
			}
			
			// Theme
			List<String> themes = new ArrayList<>();
			try {
				for (MetadataDocument.Keywords k : metadataDocument.getDatasetKeywords()) {
					for (String ks : k.getKeywords()) {
						themes.add(ks);
					}
				}
				resultDataset.put("theme", themes);
			} catch (NotFound nf) {
				resultDataset.put("theme", null);
			}
			
			// contactPoint
			// point of contact is in the contact node
			Map<String, String> contactPoint = new HashMap<>();
			contactPoint.put("@type", "vcard:Contact");
			try {
				contactPoint.put("fn", metadataDocument.getMetaDataPointOfContactName("pointOfContact"));
				contactPoint.put("hasEmail", metadataDocument.getMetaDataPointOfContactEmail("pointOfContact"));	
			} catch (NotFound nf) {
				contactPoint.put("fn", null);
				contactPoint.put("hasEmail", null); 
			}

			resultDataset.put("contactPoint", contactPoint);

			try {
				resultDataset.put("spatial", metadataDocument.getDatasetSpatialExtent());
			} catch (NotFound nf) {
				resultDataset.put("spatial", null); 
			}

			// Get the publisher
			/*
			 * The publisher is the party that publishes the data. It has the role of 'publisher'
			 * In dutch "uitgever". There is no publisher, so take distributor
			 */
			Map<String, String> publisher = new HashMap<>();
			try {
				publisher.put("name", metadataDocument.getDistributionResponsiblePartyName("distributor"));	
			} catch (NotFound nf) {
				publisher.put("name", null);
			}

			resultDataset.put("publisher", publisher);

			// Frequency
			try {
				resultDataset.put("accrualPeriodicity", metadataDocument.getMaintenanceFrequencyCodeListValue()) ;
			} catch (NotFound nf) {
				resultDataset.put("accrualPeriodicity", null);
			}
			
			return resultDataset;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Result index() {
		return ok(Json.toJson(buildDcatResult()));
	}
}
