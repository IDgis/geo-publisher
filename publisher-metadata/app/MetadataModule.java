import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import play.Logger;
import util.InetFilter;
import util.MetadataConfig;
import util.ZooKeeper;

public class MetadataModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(ZooKeeper.class).asEagerSingleton();
	}
	
	@Provides @Singleton
	public InetFilter filter(MetadataConfig config) {
		Logger.info("Initializing IP address filter");
		
		InetFilter filter = new InetFilter(config.getTrustedAddresses());
		
		int elementCount = 0;
		for(InetFilter.FilterElement element : filter.getFilterElements()) {
			elementCount++;
			
			Logger.info(element.toString());
		}
		
		Logger.info("Total number of filter elements: {}", elementCount);
		
		return filter;
	}
}
