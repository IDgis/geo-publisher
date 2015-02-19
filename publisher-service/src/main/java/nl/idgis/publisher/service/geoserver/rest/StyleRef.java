package nl.idgis.publisher.service.geoserver.rest;

public class StyleRef {

	private final String styleName;
	
	public StyleRef(String styleName) {
		this.styleName = styleName;		
	}

	public String getStyleName() {
		return styleName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((styleName == null) ? 0 : styleName.hashCode());
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
		StyleRef other = (StyleRef) obj;
		if (styleName == null) {
			if (other.styleName != null)
				return false;
		} else if (!styleName.equals(other.styleName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "StyleRef [styleName=" + styleName + "]";
	}
	
}

	
