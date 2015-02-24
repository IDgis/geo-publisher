package nl.idgis.publisher.domain.web.tree;

import java.util.List;

public class DatasetNode extends Node implements Dataset {

	private static final long serialVersionUID = -8355292953356781634L;

	private final List<String> keywords;
	
	private final String tableName;
	
	public DatasetNode(String id, String name, String title, String abstr, Tiling tiling, List<String> keywords, String tableName) {
		super(id, name, title, abstr, tiling);
		
		this.keywords = keywords;
		this.tableName = tableName;
	}	
	
	public List<String> getKeywords() {
		return keywords;
	}

	public String getTableName() {
		return tableName;
	}

	@Override
	public String toString() {
		return "DatasetNode [keywords=" + keywords + ", tableName=" + tableName
				+ ", id=" + id + ", name=" + name + ", title=" + title
				+ ", abstr=" + abstr + ", tiling=" + tiling + "]";
	}	
	
}
