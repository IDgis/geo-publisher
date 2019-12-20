package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.LdapUserGroup;

public class ListLdapUserGroups implements DomainQuery<Page<LdapUserGroup>> {
	private static final long serialVersionUID = -3814556389988838781L;
	
	private final Long page;
	private final String query;
	private final boolean all;

	public ListLdapUserGroups (final Long page, final String query, final boolean all) {
		this.page = page;
		this.query = query;
		this.all = all;
	}

	public Long getPage () {
		return page;
	}

	public String getQuery () {
		return query;
	}

	public boolean getAll() {
		return all;
	}
}
