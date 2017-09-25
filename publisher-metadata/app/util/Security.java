package util;

import static play.mvc.Controller.request;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Security {
	
	private final MetadataConfig config;
	
	@Inject
	public Security(MetadataConfig config) {
		this.config = config;
	}

	public boolean isTrusted() {
		String trustedHeaderName = config.getTrustedHeader();
		String trustedHeader = request().getHeader(trustedHeaderName);
		
		return "1".equals(trustedHeader);
	}
}
