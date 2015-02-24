package nl.idgis.publisher.domain.web.tree;

public class DatasetNode extends Node implements Dataset {	

	private static final long serialVersionUID = -1242206465492970518L;
	
	private final String tableName;
	
	public DatasetNode(String id, String name, String title, String abstr, String tableName, TilingSettings tilingSettings) {
		super(id, name, title, abstr, tilingSettings);
		
		this.tableName = tableName;
	}	

	public String getTableName() {
		return tableName;
	}

	@Override
	public String toString() {
		return "DatasetNode [tableName=" + tableName + ", id=" + id + ", name="
				+ name + ", title=" + title + ", abstr=" + abstr
				+ ", tilingSettings=" + tilingSettings + "]";
	}
	
}
