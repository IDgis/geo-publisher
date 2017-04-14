package nl.idgis.publisher.domain.web.tree;

import java.io.Serializable;
import java.util.Optional;

public abstract class AbstractLayer implements Layer, Serializable {
	
	private static final long serialVersionUID = -6919582427234370708L;
	
	protected final String id, name, title, abstr;
	
	protected final Tiling tiling;
	
	protected final boolean confidential;
	
	public AbstractLayer(String id, String name, String title, String abstr, Tiling tiling, final boolean confidential) {
		this.id = id;
		this.name = name;
		this.title = title;
		this.abstr = abstr;
		this.tiling = tiling;
		this.confidential = confidential;
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
	
	@Override
	public boolean isConfidential () {
		return confidential;
	}
	
	@Override
	public boolean isWmsOnly() {
		return false;
	}
}
