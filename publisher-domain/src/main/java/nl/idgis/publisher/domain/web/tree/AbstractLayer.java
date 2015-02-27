package nl.idgis.publisher.domain.web.tree;

import java.io.Serializable;
import java.util.Optional;

public abstract class AbstractLayer implements Layer, Serializable {		

	private static final long serialVersionUID = 5072400426320074451L;

	protected final String id, name, title, abstr;
	
	protected final Tiling tiling;
	
	public AbstractLayer(String id, String name, String title, String abstr, Tiling tiling) {
		this.id = id;
		this.name = name;
		this.title = title;
		this.abstr = abstr;
		this.tiling = tiling;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public String getAbstract() {
		return abstr;
	}
	
	@Override
	public Optional<Tiling> getTiling() {
		return Optional.ofNullable(tiling);
	}
}
