package controllers;

import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetMetadata.sourceDatasetMetadata;

import play.mvc.*;
import util.QueryDSL;
import util.QueryDSL.Transaction;
import play.libs.Json;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import com.mysema.query.Tuple;

import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.xml.exceptions.NotFound;
import util.MetadataConfig;
import util.QueryDSL;
import util.Security;
import util.QueryDSL.Transaction;


public class DatasetMetadataDCAT extends Controller{

	/*
	 * Gebruik alleen gepubiceerde datasets
	 * 1 bestand met alle datasets erin.
	 * Voorbeeld: https://geoportaal-ddh.opendata.arcgis.com/data.json
	 * Referentie: https://www.w3.org/TR/vocab-dcat/
	 * ISO 19115 -> DCAT Mapping: https://www.w3.org/2015/spatial/wiki/ISO_19115_-_DCAT_-_Schema.org_mapping
	 * CKAN - DCAT Mapping: https://github.com/ckan/ckanext-dcat#rdf-dcat-to-ckan-dataset-mapping
	 * 
	 */
	private final MetadataDocumentFactory mdf;
	private final QueryDSL q;
	private final DatasetQueryBuilder dqb;
	private HashMap<String, Object> dcatResult = new HashMap<String, Object>();

	@Inject
	public DatasetMetadataDCAT(QueryDSL q, DatasetQueryBuilder dqb, MetadataDocumentFactory mdf) {
		this.q = q;
		this.dqb = dqb;
		this.mdf = mdf;
	}

	/* The first four items on a DCAT/JSON  are standard. 
	 * They are filled in here
	 * See: https://project-open-data.cio.gov/v1.1/schema
	 * and: https://geoportaal-ddh.opendata.arcgis.com/data.json.
	 */
	private void setHeader() {
		String conformsTo = "https://project-open-data.cio.gov/v1.1/schema";
		String context = "https://project-open-data1/schema/catalog.jsonld";
		String type = "dcat:Catalog";
		String describedBy = "https://project-open-data.cio.gov/v1.1/schema/catalog.json";

		dcatResult.put("conformsTo", conformsTo);
		dcatResult.put("@context", context);
		dcatResult.put("@type", type);
		dcatResult.put("describedBy", describedBy);	

	}

	// get datasets:
	private List<Tuple> getPublishedDatasets() {
		return q.withTransaction(tx -> {
			return dqb.fromPublishedDataset(tx)
					// .join(sourceDatasetMetadata).on(sourceDatasetMetadata.sourceDatasetId.eq(sourceDataset.id))
					.list(dataset.id, dataset.identification, dataset.name, sourceDatasetVersion.confidential, sourceDatasetMetadata.document);
		});
	}

	// Fetch all published datasets and generate a hashmap for each. 
	private void populateDcatResult() {

		HashMap<String, Object> datasetsDcat = new HashMap<String, Object>();

		// get all published datasets
		List<Tuple> datasets = getPublishedDatasets();

		// Loop over all dataset to generate correct dcat metadata
		int i = 0;
		for (Tuple ds : datasets) {
			
			//System.out.println(ds.get(dataset.id)+ds.get(dataset.name)+ds.get(sourceDatasetVersion.confidential));
			datasetsDcat.put(String.valueOf(i), mapDcat(ds));
			i++;
		}

		// Add hashmap with all datasets to our result
		dcatResult.put("dataset", datasetsDcat);
	}

	/*
	 *   at controllers.DatasetMetadataDCAT.getPublishedDatasets(DatasetMetadataDCAT.java:71) 
	 *   ~[publisher-metadata.jar:1.9.9-26-gb764e62-SNAPSHOT] 
	 *   2017-09-14T12:58:02.629423630Z  at controllers.DatasetMetadataDCAT.populateDcatResult(DatasetMetadataDCAT.java:83) 
	 *   ~[publisher-metadata.jar:1.9.9-26-gb764e62-SNAPSHOT]
	 *   2017-09-14T12:58:02.629461777Z Caused by: java.sql.SQLException: java.lang.IllegalStateException: source_dataset_metadata is already used
	 */

	private HashMap<String, Object> mapDcat(Tuple ds) {
		HashMap<String, Object> resultDataset = new HashMap<String, Object>();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		
		resultDataset.put("@type", "vcard:Dataset");
		resultDataset.put("accessLevel", ds.get(sourceDatasetVersion.confidential)?"Confidential":"Public");

		try {
			MetadataDocument metadataDocument = mdf.parseDocument(ds.get(sourceDatasetMetadata.document));

			try {
				resultDataset.put("title", metadataDocument.getDatasetTitle());
			} catch (NotFound nf) {
				// TODO
			}
			try {
				resultDataset.put("description", metadataDocument.getDatasetAbstract());
			} catch (NotFound nf) {
				// TODO
			}

			try {
				Date pubDate = metadataDocument.getDatasetPublicationDate();
				resultDataset.put("issued", sdf.format(pubDate));
			} catch (NotFound nf) {
				// TODO
			}
			
			try {
				Date modDate = metadataDocument.getDatasetRevisionDate();
				resultDataset.put("modified", sdf.format(modDate));
			} catch (NotFound nf) {
				// TODO
			}
			
			try {
				resultDataset.put("identifier", metadataDocument.getDatasetIdentifier());
			} catch (NotFound nf) {
				// TODO
			}
			
			
			// keyword
			// TODO
			
			try {
				HashMap<String, String> contactPoint = new HashMap<String, String>();
				contactPoint.put("@type", "vcard:Contact");
				contactPoint.put("fn", metadataDocument.getDatasetResponsiblePartyName("point of contact"));
				contactPoint.put("hasEmail", metadataDocument.getDatasetResponsiblePartyEmail("point of contact"));
				resultDataset.put("contactPoint", contactPoint);
			} catch (NotFound nf) {
				// 
			}
			
			try {
				resultDataset.put("spatial", metadataDocument.getDatasetIdentifier());
			} catch (NotFound nf) {
				// TODO
			}
			
			// Get the publisher
			try {
				HashMap<String, String> publisher = new HashMap<String, String>();
				publisher.put("name", metadataDocument.getDatasetResponsiblePartyName("distributor"));
				resultDataset.put("publisher", publisher);
			} catch (NotFound nf) {
				// 
			}

			return resultDataset;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Dcat getDatasetMetadata(String identification) {
		// See /publisher-commons/src/main/java/nl/idgis/publisher/metadata/MetadataDocument.java



		String description = "Opendata Portaal geoinformatie Den Haag"; 
		String[] theme = {"Geospatial"};
		String landingPage = "myUrl"; 
		String publisherName = "Utrecht"; 
		String issued = "2017-01-01";

		String modified = "2019-01-05";
		String contactPointFn = "George Lucas"; 
		String identifier = identification;
		String title = "myTtile";

		String accessLevel = "Public";
		String contactPointHasEmail= "Yes"; 
		String spatial = "4.5, 8, 52, 56";
		String license = "GPL 2.0"; 
		String[] keyword = {"Natuur"};


		// Get the source xml data for this dataset



		// create MD object from xml

		// set the fields from xml

		// set the fields from db

		// set the fields with fixed values


		// Set the distributions
		//http://example.com/geoserver/wfs?service=wfs&version=1.1.0&request=GetCapabilities
		// geoserver/wfs?service=wfs&version=1.1.0&request=GetFeature&outputFormat=json&UUID=f78389f8-a03d-4866-87dc-44b278b1858b 

		return new Dcat(description, theme, landingPage, publisherName, issued,
				modified, contactPointFn, identifier, title, accessLevel,
				contactPointHasEmail, spatial, license, keyword);
	}

	private HashMap<String, Object> mapDcat(Dcat dataset) {

		HashMap<String, Object> resultDataset = new HashMap<String, Object>();

		// Get the publisher
		HashMap<String, String> publisher = new HashMap<String, String>();
		publisher.put("name", dataset.getPublisherName());
		resultDataset.put("publisher", publisher);

		// Get the contactPoint

		// Get all other info
		resultDataset.put("title", dataset.getTitle());



		// Get acceslevel
		resultDataset.put("accesslevel", dataset.getAccessLevel());

		resultDataset.put("keyword", dataset.getKeyword());

		return resultDataset;
	}

	public Result index() {
		setHeader();
		populateDcatResult();
		return ok(Json.toJson(dcatResult));
	}
}
