package nl.idgis.publisher.domain.web.tree;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class DefaultVectorDatasetLayer extends AbstractDatasetLayer implements VectorDatasetLayer {

	private static final long serialVersionUID = -1773177921425540543L;	
	
	private final String tableName;
	
	private final List<String> columnNames;
	
	public DefaultVectorDatasetLayer(String id, String name, String title, String abstr, Tiling tiling, Optional<String> metadataFileIdentification,
		List<String> keywords, String tableName, List<String> columnNames, List<StyleRef> styleRef, boolean confidential, Timestamp importTime) {
		
		super(id, name, title, abstr, tiling, confidential, metadataFileIdentification, importTime, keywords, styleRef);
		
		this.tableName = tableName;
		this.columnNames = columnNames;
	}

	@Override
	public String getTableName() {
		return tableName;
	}
	
	@Override
	public List<String> getColumnNames() {
		return columnNames;
	}

	@Override
	public boolean isVectorLayer() {
		return true;
	}

	@Override
	public VectorDatasetLayer asVectorLayer() {
		return this;
	}

	@Override
	public String toString() {
		return "DefaultVectorDatasetLayer [keywords=" + keywords + ", tableName="
				+ tableName + ", columnNames=" + columnNames + ", styleRef="
				+ styleRef + "]";
	}
	
}
