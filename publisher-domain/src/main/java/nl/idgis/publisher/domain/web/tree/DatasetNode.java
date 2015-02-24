package nl.idgis.publisher.domain.web.tree;

public class DatasetNode extends Node implements Dataset {

	private static final long serialVersionUID = 7565019558556697339L;
	
	private final String tableName;
	
	public DatasetNode(String id, String name, String title, String abstr, String tableName, Tiling tiling) {
		super(id, name, title, abstr, tiling);
		
		this.tableName = tableName;
	}	

	public String getTableName() {
		return tableName;
	}

	@Override
	public String toString() {
		return "DatasetNode [tableName=" + tableName + ", id=" + id + ", name="
				+ name + ", title=" + title + ", abstr=" + abstr
				+ ", tiling=" + tiling + "]";
	}
	
}
