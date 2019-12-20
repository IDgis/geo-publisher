package nl.idgis.publisher.domain.web.tree;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PartialGroupLayer implements Serializable {

	private static final long serialVersionUID = 2471303026788933931L;

	private final String id;
	
	private final String name;
	
	private final String title;
	
	private final String abstr;
	
	private final List<String> userGroups;
	
	private final Tiling tiling;

	public PartialGroupLayer(String id, String name, String title, String abstr, List<String> userGroups, Optional<Tiling> tiling) {
		this.id = Objects.requireNonNull(id);
		this.name = name;
		this.title = title;
		this.abstr = abstr;
		this.userGroups = userGroups;
		this.tiling = tiling.orElse(null);
	}
	
	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getTitle() {
		return title;
	}

	public String getAbstract() {
		return abstr;
	}

	public List<String> getUserGroups() {
		return userGroups;
	}

	public Optional<Tiling> getTiling() {
		return Optional.ofNullable(tiling);
	}

	@Override
	public String toString() {
		return "PartialGroupLayer [id=" + id + ", name=" + name + ", title=" + title
				+ ", abstr=" + abstr + ", userGroups=" + userGroups + ", tiling=" + tiling
				+ "]";
	}
	
}
