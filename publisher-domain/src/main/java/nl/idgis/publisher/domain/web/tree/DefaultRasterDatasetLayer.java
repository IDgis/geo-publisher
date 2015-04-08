package nl.idgis.publisher.domain.web.tree;

import java.util.List;

public class DefaultRasterDatasetLayer extends AbstractDatasetLayer implements RasterDatasetLayer {

	private static final long serialVersionUID = -7004287324470421060L;
	
	private final String fileName;

	public DefaultRasterDatasetLayer(String id, String name, String title, String abstr, Tiling tiling, List<String> keywords, String fileName, List<StyleRef> styleRef) {
		super(id, name, title, abstr, tiling, keywords, styleRef);
		
		this.fileName = fileName;
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
	public RasterDatasetLayer asRasterLayer() {
		return this;
	}
	
	@Override
	public String toString() {
		return "DefaultRasterDatasetLayer [fileName=" + fileName
				+ ", keywords=" + keywords + ", styleRef=" + styleRef + ", id="
				+ id + ", name=" + name + ", title=" + title + ", abstr="
				+ abstr + ", tiling=" + tiling + "]";
	}
}
