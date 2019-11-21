package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.LdapUserGroup;

public class ListLdapUserGroups implements DomainQuery<Page<LdapUserGroup>> {
	private static final long serialVersionUID = 2174744761644686829L;
	
	private final Long page;
	private final String query;

	public ListLdapUserGroups (final Long page, final String query) {
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
