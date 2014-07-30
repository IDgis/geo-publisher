package nl.idgis.publisher.database.messages;


public class GetSourceDatasetInfo extends Query {

	private static final long serialVersionUID = -6128241149157834193L;
	
	private final String dataSourceId;
	private final String categoryId;
	
	public GetSourceDatasetInfo (final String dataSource, final String category) {
		this.dataSourceId = (dataSource == null ? null : dataSource);
		this.categoryId = (category == null ? null : category);
	}
	
	public String getDataSourceId () {
		return this.dataSourceId;
	}
	
	public String getCategoryId () {
		return this.categoryId;
	}

	@Override
	public String toString() {
		return "GetSourceDatasetInfo [dataSourceId=" + dataSourceId
				+ ", categoryId=" + categoryId + "]";
	}

}
