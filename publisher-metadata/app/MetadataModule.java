import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import util.InetFilter;
import util.MetadataConfig;
import util.ZooKeeper;

public class MetadataModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(ZooKeeper.class).asEagerSingleton();
	}
	
	@Provides
	public InetFilter filter(MetadataConfig config) {
		return new InetFilter(config.getTrustedAddresses());
	}
}
