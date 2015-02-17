package nl.idgis.publisher.service.geoserver.rest;

import java.util.List;

public class ServiceSettings {

	private final String title, abstr;
	
	private final List<String> keywords;
	
	public ServiceSettings(String title, String abstr, List<String> keywords) {
		this.title = title;
		this.abstr = abstr;
		this.keywords = keywords;
	}

	public String getTitle() {
		return title;
	}
	
	public String getAbstract() {
		return abstr;
	}
	
	public List<String> getKeywords() {
		return keywords;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((abstr == null) ? 0 : abstr.hashCode());
		result = prime * result
				+ ((keywords == null) ? 0 : keywords.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
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
		ServiceSettings other = (ServiceSettings) obj;
		if (abstr == null) {
			if (other.abstr != null)
				return false;
		} else if (!abstr.equals(other.abstr))
			return false;
		if (keywords == null) {
			if (other.keywords != null)
				return false;
		} else if (!keywords.equals(other.keywords))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ServiceSettings [title=" + title + ", abstr=" + abstr
				+ ", keywords=" + keywords + "]";
	}
}
