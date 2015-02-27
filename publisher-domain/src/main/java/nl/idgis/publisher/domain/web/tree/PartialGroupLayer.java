package nl.idgis.publisher.domain.web.tree;

public class PartialGroupLayer extends AbstractLayer {

	private static final long serialVersionUID = -3873187204544671166L;

	public PartialGroupLayer(String id, String name, String title, String abstr, Tiling tiling) {
		super(id, name, title, abstr, tiling);
	}

	@Override
	public String toString() {
		return "PartialGroupLayer [id=" + id + ", name=" + name + ", title=" + title
				+ ", abstr=" + abstr + ", tiling=" + tiling
				+ "]";
	}
	
}
