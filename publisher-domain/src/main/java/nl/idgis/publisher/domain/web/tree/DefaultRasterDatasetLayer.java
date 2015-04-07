package nl.idgis.publisher.domain.web.tree;

import java.util.List;

public class DefaultRasterDatasetLayer extends AbstractDatasetLayer implements RasterDatasetLayer {

	private static final long serialVersionUID = -3811811637259595680L;

	public DefaultRasterDatasetLayer(String id, String name, String title, String abstr, Tiling tiling, List<String> keywords, List<StyleRef> styleRef) {
		super(id, name, title, abstr, tiling, keywords, styleRef);
	}
}
