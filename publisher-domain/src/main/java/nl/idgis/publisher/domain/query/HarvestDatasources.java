package nl.idgis.publisher.domain.query;


public class HarvestDatasources implements DomainQuery<Boolean> {
	private static final long serialVersionUID = -3512249423258148930L;
	
	private final String datasourceId;
	
	public HarvestDatasources () {
		this (null);
	}
	
	public HarvestDatasources (final String datasourceId) {
		this.datasourceId = datasourceId;
	}

	public String getDatasourceId () {
		return datasourceId;
	}
}
