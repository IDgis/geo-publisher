package controllers;

import play.mvc.*;
import play.libs.Json;

import java.util.HashMap;

import javax.inject.Inject;

import com.mysema.query.Tuple;

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

	private HashMap<String, Object> dcatResult = new HashMap<String, Object>();
	
	/* The first four items on a DCAT/JSON  are standard. 
	 * They are filled in here
	 * See: https://project-open-data.cio.gov/v1.1/schema
	 * and: https://geoportaal-ddh.opendata.arcgis.com/data.json.
	 */
	private void setHeader() {
		String conformsTo = "https://project-open-data.cio.gov/v1.1/schema";
		String context = "https://project-open-dat�1/schema/catalog.jsonld";
		String type = "dcat:Catalog";
		String describedBy = "https://project-open-data.cio.gov/v1.1/schema/catalog.json";
		
		dcatResult.put("conformsTo", conformsTo);
		dcatResult.put("context", context);
		dcatResult.put("type", type);
		dcatResult.put("describedBy", describedBy);	
		
	}
	
	
	
	private void populateDcatResult() {
		// Loop over all dataset to generate correct dcat metadata
		HashMap<String, Object> datasetsDcat = new HashMap<String, Object>();
		//for(Tuple dataset : datasets) {
			int i = 0;
			
			//String identification = dataset.get(dataset.identification);
			String identification = "123445-1234-1234-123456789012";
			datasetsDcat.put(String.valueOf(i), mapDcat(getDatasetMetadata(identification)));
			i++;
		//}
		
		// Add hashmap with all datasets to our result
		dcatResult.put("dataset", datasetsDcat);
	}

	private Dcat getDatasetMetadata(String identification) {
		// See /publisher-commons/src/main/java/nl/idgis/publisher/metadata/MetadataDocument.java
		
		return new Dcat("public", identification, "myTitle", "Opendata Portaal geoinformatie Den Haag");
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
		
		return resultDataset;
	}
	
	public Result index() {
		setHeader();
		populateDcatResult();
		return ok(Json.toJson(dcatResult));
	}
}
