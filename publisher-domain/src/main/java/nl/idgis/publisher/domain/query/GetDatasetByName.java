package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.web.Dataset;

public class GetDatasetByName implements DomainQuery<Dataset> {
	private static final long serialVersionUID = -9005248073396173502L;
	
	private final String name;
	private final boolean caseSensitive;
	
	public GetDatasetByName (final String name, final boolean caseSensitive) {
		this.name = name;
		this.caseSensitive = caseSensitive;
	}

	public String getName () {
		return name;
	}

	public boolean isCaseSensitive () {
		return caseSensitive;
	}
}
