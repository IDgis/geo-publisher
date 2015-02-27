package nl.idgis.publisher.service.geoserver.rest;

public class GroupRef extends PublishedRef {

	public GroupRef(String layerName) {
		super(layerName);
	}

	@Override
	public boolean isGroup() {
		return true;
	}
	
	@Override
	public GroupRef asGroupRef() {
		return this;
	}

}
