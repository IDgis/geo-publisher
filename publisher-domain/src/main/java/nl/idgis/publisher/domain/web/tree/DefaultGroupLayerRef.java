package nl.idgis.publisher.domain.web.tree;

public class DefaultGroupLayerRef extends AbstractLayerRef<GroupLayer> implements GroupLayerRef {

	private static final long serialVersionUID = 6992740585594990456L;

	public DefaultGroupLayerRef(GroupLayer layer) {
		super(layer);
	}
	
	@Override
	public boolean isGroupRef() {
		return true;
	}
	
	@Override
	public GroupLayerRef asGroupRef() {
		return this;
	}

	@Override
	public String toString() {
		return "DefaultGroupLayerRef [layer=" + layer + "]";
	}

}
