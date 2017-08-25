package nl.idgis.publisher.domain.web.tree;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class DefaultRasterDatasetLayer extends AbstractDatasetLayer implements RasterDatasetLayer {
	
	private static final long serialVersionUID = -5210887564537472105L;
	
	private final String fileName;
	
	private final boolean wmsOnly;
	
	public DefaultRasterDatasetLayer(String id, String name, String title, String abstr, Tiling tiling, Optional<String> metadataFileIdentification,
		List<String> keywords, String fileName, List<StyleRef> styleRef, boolean confidential, boolean wmsOnly, Timestamp importTime) {
		super(id, name, title, abstr, tiling, confidential, metadataFileIdentification, importTime, keywords, styleRef);
		
		this.fileName = fileName;
		this.wmsOnly = wmsOnly;
	}
	
	@Override
	public String getFileName() {
		return fileName;
	}
	
	@Override
	public boolean isRasterLayer() {
		return true;
	}
	
	@Override
	public boolean isWmsOnly() {
		return wmsOnly;		
	}
	
	@Override
	public RasterDatasetLayer asRasterLayer() {
		return this;
	}
}
