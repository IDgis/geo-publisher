import com.google.inject.AbstractModule;

import util.ZooKeeper;

public class MetadataModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(ZooKeeper.class).asEagerSingleton();
	}
}
