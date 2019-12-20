package nl.idgis.publisher.domain.query;

import java.util.List;

public class CleanupLdapUserGroups implements DomainQuery<Boolean> {
	private static final long serialVersionUID = -5127329940654918463L;
	
	private final List<String> ldapUserGroupNames;
	
	public CleanupLdapUserGroups (final List<String> ldapUserGroupNames) {
		this.ldapUserGroupNames = ldapUserGroupNames;
	}

	public List<String> getLdapUserGroupNames () {
		return ldapUserGroupNames;
	}
}
