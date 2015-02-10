package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;

public abstract class AbstractLayer implements Layer, Serializable {	

	private static final long serialVersionUID = 6734327906748493507L;
	
	private final boolean isGroup;
	
	public AbstractLayer(boolean isGroup) {		
		this.isGroup = isGroup;
	}
	
	@Override
	public final boolean isGroup() {
		return isGroup;
	}

	@Override
	public final GroupLayer asGroup() {
		if(!isGroup) {
			throw new UnsupportedOperationException("Not a group");
		}
		
		return (GroupLayer)this;
	}
	
	@Override
	public final boolean isDataset() {
		return !isGroup;
	}

	@Override
	public final DatasetLayer asDataset() {
		if(isGroup) {
			throw new UnsupportedOperationException("Not a dataset");
		}
		
		return (DatasetLayer)this;
	}	
}
