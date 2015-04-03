package nl.idgis.publisher.domain.service;

import nl.idgis.publisher.domain.EntityType;
import nl.idgis.publisher.domain.MessageProperties;
import nl.idgis.publisher.domain.StatusType;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class DatasetLog<T extends DatasetLog<T>> implements MessageProperties {
	
	private static final long serialVersionUID = 2742502035495391359L;
	
	private final Dataset dataset;
	
	public DatasetLog() {
		this(null);
	}
	
	public DatasetLog(Dataset dataset) {
		this.dataset = dataset;
	}

	@Override
	@JsonIgnore
	public EntityType getEntityType() {
		return EntityType.DATASET;
	}

	@Override
	@JsonIgnore
	public String getIdentification() {
		if(dataset == null) {
			return null;
		}
		
		return dataset.getId();
	}

	@Override
	@JsonIgnore
	public String getTitle() {
		if(dataset == null) {
			return null;
		}
		
		return dataset.getName();
	}
	
	@Override
	@JsonIgnore
	public StatusType getStatus () {
		return null;
	}
	
	public abstract T withDataset(Dataset dataset);
}
