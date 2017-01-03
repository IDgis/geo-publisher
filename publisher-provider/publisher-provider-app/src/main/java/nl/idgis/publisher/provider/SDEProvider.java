package nl.idgis.publisher.provider;

import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.protocol.messages.Hello;
import nl.idgis.publisher.provider.protocol.EchoRequest;
import nl.idgis.publisher.provider.protocol.EchoResponse;
import nl.idgis.publisher.provider.protocol.GetDatasetInfo;
import nl.idgis.publisher.provider.protocol.GetRasterDataset;
import nl.idgis.publisher.provider.protocol.GetVectorDataset;
import nl.idgis.publisher.provider.protocol.ListDatasetInfo;
import nl.idgis.publisher.provider.sde.SDEGetDatasetInfoHandler;
import nl.idgis.publisher.provider.sde.SDEGetRasterDatasetHandler;
import nl.idgis.publisher.provider.sde.SDEGetVectorDatasetHandler;
import nl.idgis.publisher.provider.sde.SDEListDatasetInfoHandler;
import nl.idgis.publisher.utils.UniqueNameGenerator;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class SDEProvider extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	private final Props databaseProps;
	
	private final Props rasterFolderProps;
	
	private ActorRef database;
	
	private ActorRef rasterFolder;
	
	public SDEProvider(Props databaseProps, Props rasterFolderProps) {
		this.databaseProps = databaseProps;
		this.rasterFolderProps = rasterFolderProps;
	}
	
	public static Props props(Props databaseProps, Props rasterFolderProps) {
		return Props.create(SDEProvider.class, databaseProps, rasterFolderProps);
	}
	
	@Override
	public void preStart() {
		database = getContext().actorOf(databaseProps, "database");
		rasterFolder = getContext().actorOf(rasterFolderProps, "raster-folder");
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Hello) {
			log.info("registered with: " + ((Hello)msg).getName());
		} else if(msg instanceof EchoRequest) {
			getSender().tell(new EchoResponse(((EchoRequest) msg).getPayload()), getSelf());
		} else if(msg instanceof ListDatasetInfo) {
			handleListDatasetInfo((ListDatasetInfo)msg);
		} else if(msg instanceof GetDatasetInfo) {
			handleGetDatasetInfo((GetDatasetInfo)msg);
		} else if(msg instanceof GetVectorDataset) {
			handleGetVectorDataset((GetVectorDataset)msg);
		} else if(msg instanceof GetRasterDataset) {
			handleGetRasterDataset((GetRasterDataset)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleGetRasterDataset(GetRasterDataset msg) {
		log.debug("get raster dataset");
		
		ActorRef handler = getContext().actorOf(
			SDEGetRasterDatasetHandler.props(getSender(), msg, rasterFolder),
			nameGenerator.getName(SDEGetRasterDatasetHandler.class));
			
		database.tell(new StartTransaction(), handler);
	}

	private void handleGetVectorDataset(GetVectorDataset msg) {
		log.debug("get vector dataset");
		
		ActorRef handler = getContext().actorOf(
			SDEGetVectorDatasetHandler.props(getSender(), msg),
			nameGenerator.getName(SDEGetVectorDatasetHandler.class));
		
		database.tell(new StartTransaction(), handler);
	}

	private void handleGetDatasetInfo(GetDatasetInfo msg) {
		log.debug("get dataset info");
		
		ActorRef handler = getContext().actorOf(
			SDEGetDatasetInfoHandler.props(getSender(), msg, rasterFolder),
			nameGenerator.getName(SDEGetDatasetInfoHandler.class));
			
		database.tell(new StartTransaction(), handler);
	}

	private void handleListDatasetInfo(ListDatasetInfo msg) {
		log.debug("list dataset info");
		
		ActorRef handler = getContext().actorOf(
			SDEListDatasetInfoHandler.props(getSender(), msg, rasterFolder),
			nameGenerator.getName(SDEListDatasetInfoHandler.class));
		
		database.tell(new StartTransaction(), handler);
	}
}
