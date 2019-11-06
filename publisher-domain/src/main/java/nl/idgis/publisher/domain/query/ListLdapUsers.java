package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.LdapUser;

public class ListLdapUsers implements DomainQuery<Page<LdapUser>> {
	private static final long serialVersionUID = 6913187895594574423L;
	
	private final Long page;
	private final String query;

	public ListLdapUsers (final Long page, final String query) {
		this.page = page;
		this.query = query;
	}

	public Long getPage () {
		return page;
	}

	public String getQuery () {
		return query;
	}
}
