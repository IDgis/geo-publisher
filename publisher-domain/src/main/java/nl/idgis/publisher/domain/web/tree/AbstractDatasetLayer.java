package nl.idgis.publisher.domain.web.tree;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public abstract class AbstractDatasetLayer extends AbstractLayer implements DatasetLayer {

	private static final long serialVersionUID = -682116558721692820L;
	
	protected final String datasetId;
	
	protected final List<String> keywords;
	
	protected final List<StyleRef> styleRef;
	
	protected final Timestamp importTime;

	public AbstractDatasetLayer(String id, String name, String title, String abstr, Tiling tiling, boolean confidential, String datasetId, Timestamp importTime, List<String> keywords, List<StyleRef> styleRef) {
		super(id, name, title, abstr, tiling, confidential);

		this.datasetId = datasetId;
		this.keywords = keywords;
		this.styleRef = styleRef;
		this.importTime = importTime;
	}
	
	@Override
	public String getDatasetId() {
		return datasetId;
	}
	
	@Override
	public List<String> getKeywords() {
		return keywords;
	}
	
	@Override
	public List<StyleRef> getStyleRefs() {
		return styleRef;
	}
	
	@Override
	public boolean isVectorLayer() {
		return false;
	}
	
	@Override
	public VectorDatasetLayer asVectorLayer() {
		throw new IllegalStateException("DatasetLayer is not a VectorDatasetLayer");
	}
	
	@Override
	public boolean isRasterLayer() {
		return false;
	}
	
	@Override
	public RasterDatasetLayer asRasterLayer() {
		throw new IllegalStateException("DatasetLayer is not a RasterDatasetLayer");
	}
	
	@Override
	public Optional<Timestamp> getImportTime() {
		return Optional.of(importTime);
	}

}
