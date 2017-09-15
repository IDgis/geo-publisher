package controllers;

import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetMetadata.sourceDatasetMetadata;

import play.mvc.*;
import util.QueryDSL;
import play.libs.Json;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.mysema.query.Tuple;

import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.xml.exceptions.NotFound;

import util.Security;



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
		return q.withTransaction(tx -> 
			dqb.fromPublishedDataset(tx)
			.list(dataset.id, dataset.identification, dataset.name, sourceDatasetVersion.confidential, sourceDatasetMetadata.document)
			);
	}

	// Fetch all published datasets and generate a hashmap for each. 
	private void populateDcatResult() {

		List<Object> datasetsDcat = new ArrayList<>();

		// get all published datasets
		List<Tuple> datasets = getPublishedDatasets();

		// Loop over all dataset to generate correct dcat metadata
		//int i = 0;
		for (Tuple ds : datasets) {

			//System.out.println(ds.get(dataset.id)+ds.get(dataset.name)+ds.get(sourceDatasetVersion.confidential));
			//datasetsDcat.put(String.valueOf(i), mapDcat(ds));
			datasetsDcat.add(mapDcat(ds));
			//i++;
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
		List<Object> distributions = new ArrayList<>();
		
		String baseUrl = "http://services.geodataoverijssel.nl/"; // Waarschijnlijk uit een config file
		String geoserverUrl = "geoserver/wfs?service=wfs&version=2.0.0&request=GetFeature&typeName=";
				
		String datasetIdent =  ds.get(dataset.identification);
		String metadateIdent = ds.get(dataset.metadataFileIdentification);
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		resultDataset.put("@type", "dcat:Dataset");
		resultDataset.put("accessLevel", ds.get(sourceDatasetVersion.confidential) ? "confidential" : "public");
		resultDataset.put("landingspage", baseUrl+"metadata/dataset/"+datasetIdent);
		
		
		Map<String, String> distributionsTypes = new HashMap<>(); 
		distributionsTypes.put("GML2", "application/gml+xml");
		distributionsTypes.put("GML3", "application/gml+xml");
		distributionsTypes.put("KML", "application/vnd.google-earth.kml+xml");
		distributionsTypes.put("CSV", "text/csv");
		distributionsTypes.put("ZIP-SHP", "application/zip");
		distributionsTypes.put("GeoJSON", "application/vnd.geo+json");
		
		for (String dt : distributionsTypes.keySet()) {
			HashMap<String, String> distribution = new HashMap<>();
			distribution.put("@type", "dcat:Distribution");
			distribution.put("title", dt);
			distribution.put("format", dt);
			distribution.put("mediaType", distributionsTypes.get(dt));
			distribution.put("downloadURL", baseUrl+geoserverUrl+"typeName:featuretype"+dt.toLowerCase().substring(0,3));
		
			distributions.add(distribution);
		}
		
		// http://services.geodataoverijssel.nl/geoserver/wfs?service=wfs&version=2.0.0&request=GetFeature&typeName=B24_spoorwegen:B2_Spoorwegen_NWB&outputFormat=csv
		
		// dataseturl = &typeName=B24_spoorwegen:B2_Spoorwegen_NWB
		// format/url = &outputformat=csv

		//r01_4_ro_algemeen/Rode_contouren
		//Rode_contouren
		// https://services.geopublisher.local/geoserver/wfs?service=wfs&version=2.0.0&request=GetFeature&typeName=r01_4_ro_algemeen:Rode_contouren
		
		
		// metadata/dataset/13e4e3d9-f3fa-4a51-ad98-445bf1d01b67.xml
		// download url
		
		//https://admin.geopublisher.local/datasets/1273a264-1d79-4cdb-afed-f06aa353bf49 is dataset.identification
		//https://metadata.geopublisher.local/metadata/dataset/85b0f332-b73f-4207-8518-b88e3fa710e1.xml is dataset.metadata_file_identification
		
		// All info from XML
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
				resultDataset.put("issued", null);
			}

			try {
				Date modDate = metadataDocument.getDatasetRevisionDate();
				resultDataset.put("modified", sdf.format(modDate));
			} catch (NotFound nf) {
				// TODO
				resultDataset.put("modified", null);
			}

			try {
				resultDataset.put("identifier", metadataDocument.getDatasetIdentifier());
			} catch (NotFound nf) {
				// TODO
			}


			// keyword
			List<String> keywords = new ArrayList<>();

			resultDataset.put("keyword", keywords);
			
			// contactPoint
			HashMap<String, String> contactPoint = new HashMap<String, String>();
			contactPoint.put("@type", "vcard:Contact");
			try {
				contactPoint.put("fn", metadataDocument.getDatasetResponsiblePartyName("point of contact"));
				contactPoint.put("hasEmail", metadataDocument.getDatasetResponsiblePartyEmail("point of contact"));	
			} catch (NotFound nf) {
				// 
			}
			resultDataset.put("contactPoint", contactPoint);


			try {
				resultDataset.put("spatial", metadataDocument.getDatasetSpatialExtent());
			} catch (NotFound nf) {
				//TODO);
			}

			// Theme TODO
			List<String> themes = new ArrayList<>();
			resultDataset.put("theme", themes);
			
			// Get the publisher
			HashMap<String, String> publisher = new HashMap<String, String>();
			try {
				publisher.put("name", metadataDocument.getDatasetResponsiblePartyName("distributor"));	
			} catch (NotFound nf) {
				// 
				publisher.put("name", "Not supplied");
			}
			resultDataset.put("publisher", publisher);

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
