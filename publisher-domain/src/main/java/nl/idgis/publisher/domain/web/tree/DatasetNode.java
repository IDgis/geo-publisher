package nl.idgis.publisher.domain.web.tree;

import java.util.List;

public class DatasetNode extends Node implements Dataset {

	private static final long serialVersionUID = -908921909727671667L;

	private final List<String> keywords;
	
	private final String tableName;
	
	private final List<String> styles;
	
	public DatasetNode(String id, String name, String title, String abstr, Tiling tiling, 
		List<String> keywords, String tableName, List<String> styles) {
		
		super(id, name, title, abstr, tiling);
		
		this.keywords = keywords;
		this.tableName = tableName;
		this.styles = styles;
	}	
	
	@Override
	public List<String> getKeywords() {
		return keywords;
	}

	@Override
	public String getTableName() {
		return tableName;
	}
	
	@Override
	public List<String> getStyles() {
		return styles;
	}

	@Override
	public String toString() {
		return "DatasetNode [keywords=" + keywords + ", tableName=" + tableName
				+ ", styles=" + styles + ", id=" + id + ", name=" + name
				+ ", title=" + title + ", abstr=" + abstr + ", tiling="
				+ tiling + "]";
	}	
	
}
