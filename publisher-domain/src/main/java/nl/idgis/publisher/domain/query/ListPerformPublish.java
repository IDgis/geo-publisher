package nl.idgis.publisher.domain.query;

import java.util.List;

public class ListPerformPublish implements DomainQuery<Boolean> {
	
	private static final long serialVersionUID = 1L;
	
	private final List<PerformPublish> performPublishes;
	
	public ListPerformPublish(List<PerformPublish> performPublishes) {
		this.performPublishes = performPublishes;
	}

	public List<PerformPublish> getPerformPublishes() {
		return performPublishes;
	}
}
