package nl.idgis.publisher.database.messages;

public class GetDatasetListInfo extends Query {

	private static final long serialVersionUID = 8201608118972323489L;
	
	private final String categoryId;

	public GetDatasetListInfo(String categoryId) {
		super();
		this.categoryId = categoryId;
	}

	public String getCategoryId() {
		return categoryId;
	}

	@Override
	public String toString() {
		return "GetDatasetListInfo [categoryId=" + categoryId + "]";
	}
	
}
