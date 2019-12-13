package nl.idgis.publisher.domain.web.tree;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public abstract class AbstractDatasetLayer extends AbstractLayer implements DatasetLayer {

	private static final long serialVersionUID = -2474202014945607410L;

	protected final String metadataFileIdentification;
	
	protected final List<String> keywords;
	
	protected final List<StyleRef> styleRef;
	
	protected final Timestamp importTime;
	
	protected final List<String> userGroups;

	public AbstractDatasetLayer(String id, String name, String title, String abstr, Tiling tiling, boolean confidential, Optional<String> metadataFileIdentification, Timestamp importTime, List<String> keywords, List<StyleRef> styleRef, List<String> userGroups) {
		super(id, name, title, abstr, tiling, confidential);

		this.metadataFileIdentification = metadataFileIdentification.orElse (null);
		this.keywords = keywords;
		this.styleRef = styleRef;
		this.userGroups = userGroups;
		this.importTime = importTime;
	}
	
	@Override
	public Optional<String> getMetadataFileIdentification() {
		return Optional.ofNullable (metadataFileIdentification);
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
	public List<String> getUserGroups() {
		return userGroups;
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
