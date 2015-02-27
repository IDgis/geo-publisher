package nl.idgis.publisher.service.geoserver.rest;

public abstract class PublishedRef {
	
	protected final String layerName;
	
	protected PublishedRef(String layerName) {
		this.layerName = layerName;
	}

	public abstract boolean isGroup();
	
	public LayerRef asLayerRef() {
		throw new IllegalArgumentException("not a LayerRef");
	}
	
	public GroupRef asGroupRef() {
		throw new IllegalArgumentException("not a GroupRef");
	}
	
	public String getLayerName() {
		return layerName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((layerName == null) ? 0 : layerName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PublishedRef other = (PublishedRef) obj;
		if (layerName == null) {
			if (other.layerName != null)
				return false;
		} else if (!layerName.equals(other.layerName))
			return false;
		return true;
	}
}
