package controllers;

import java.util.Arrays;

import javax.inject.Inject;

import nl.idgis.dav.router.SimpleWebDAV;

public class Index extends SimpleWebDAV {
	
	private static final String SERVICE_PATH = "service/";
	
	private static final String DATASET_PATH = "dataset/";
	
	// If this value is changed it also needs to be changed in the
	// withPrefix method in the DatasetMetadata class
	private static final String DATASET_PATH_NGR = "ngr/dataset/";
	
	private final ServiceMetadata serviceMetadata;
	
	private final DatasetMetadata datasetMetadata;
	
	@Inject
	public Index(ServiceMetadata serviceMetadata, DatasetMetadata datasetMetadata) {
		this("/", serviceMetadata, datasetMetadata);
	}
	
	public Index(String prefix, ServiceMetadata serviceMetadata, DatasetMetadata datasetMetadata) {
		super(
			prefix, 
			Arrays.asList(
				serviceMetadata.withPrefix(prefix + SERVICE_PATH),
				datasetMetadata.withPrefix(prefix + DATASET_PATH),
				datasetMetadata.withPrefix(prefix + DATASET_PATH_NGR)));
		
		this.serviceMetadata = serviceMetadata;
		this.datasetMetadata = datasetMetadata;
	}

	public Index withPrefix(String prefix) {
		return new Index(prefix, serviceMetadata, datasetMetadata);
	}
}
