package nl.idgis.publisher.domain.web.tree;

import java.util.List;

public class DefaultDatasetLayer extends AbstractLayer implements DatasetLayer {

	private static final long serialVersionUID = -1160957299881769454L;

	private final List<String> keywords;
	
	private final String tableName;
	
	private final List<StyleRef> styleRef;
	
	public DefaultDatasetLayer(String id, String name, String title, String abstr, Tiling tiling, 
		List<String> keywords, String tableName, List<StyleRef> styleRef) {
		
		super(id, name, title, abstr, tiling);
		
		this.keywords = keywords;
		this.tableName = tableName;
		this.styleRef = styleRef;
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
	public List<StyleRef> getStyleRefs() {
		return styleRef;
	}

	@Override
	public String toString() {
		return "DefaultDatasetLayer [keywords=" + keywords + ", tableName=" + tableName
				+ ", styleRef=" + styleRef + ", id=" + id + ", name=" + name
				+ ", title=" + title + ", abstr=" + abstr + ", tiling="
				+ tiling + "]";
	}	
	
}
