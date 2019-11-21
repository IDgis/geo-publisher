package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.LdapUser;

public class ListLdapUsers implements DomainQuery<Page<LdapUser>> {
	private static final long serialVersionUID = 4825717646003217196L;
	
	private final Long page;
	private final String query;
	private final boolean all;

	public ListLdapUsers(final Long page, final String query, final boolean all) {
		this.page = page;
		this.query = query;
		this.all = all;
	}

	public Long getPage() {
		return page;
	}

	public String getQuery() {
		return query;
	}

	public boolean getAll() {
		return all;
	}
}
