package nl.idgis.publisher.service.geoserver.rest;

public class LayerRef {

	private final String layerName;
	
	private final boolean group;
	
	public LayerRef(String layerName, boolean group) {
		this.layerName = layerName;
		this.group = group;
	}
	
	public String getLayerName() {
		return layerName;
	}
	
	public boolean isGroup() {
		return group;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (group ? 1231 : 1237);
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
		LayerRef other = (LayerRef) obj;
		if (group != other.group)
			return false;
		if (layerName == null) {
			if (other.layerName != null)
				return false;
		} else if (!layerName.equals(other.layerName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "LayerRef [layerName=" + layerName + ", group=" + group + "]";
	}
	
}
