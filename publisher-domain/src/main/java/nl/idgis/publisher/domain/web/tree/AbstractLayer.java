package nl.idgis.publisher.domain.web.tree;

import java.io.Serializable;
import java.util.Optional;

public abstract class AbstractLayer<T extends Item> implements Layer, Item, Serializable {

	private static final long serialVersionUID = 5134964120505946288L;

	private final boolean isGroup;
	
	protected final T item;
	
	public AbstractLayer(T item, boolean isGroup) {
		this.item = item;
		this.isGroup = isGroup;
	}
	
	@Override
	public final boolean isGroup() {
		return isGroup;
	}

	@Override
	public final GroupLayer asGroup() {
		if(!isGroup) {
			throw new UnsupportedOperationException("Not a group");
		}
		
		return (GroupLayer)this;
	}

	@Override
	public final DatasetLayer asDataset() {
		if(isGroup) {
			throw new UnsupportedOperationException("Not a dataset");
		}
		
		return (DatasetLayer)this;
	}

	@Override
	public String getId() {
		return item.getId();
	}

	@Override
	public String getName() {
		return item.getName();
	}

	@Override
	public String getTitle() {
		return item.getTitle();
	}

	@Override
	public String getAbstract() {
		return item.getAbstract();
	}	
	
	@Override
	public Optional<Tiling> getTiling() {
		return item.getTiling();
	}
}
