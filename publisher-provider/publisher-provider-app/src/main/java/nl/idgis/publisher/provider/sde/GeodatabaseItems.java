package nl.idgis.publisher.provider.sde;

import java.util.ArrayList;
import java.util.List;

import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.provider.database.messages.DatabaseColumnInfo;
import nl.idgis.publisher.provider.database.messages.FetchTable;
import nl.idgis.publisher.provider.sde.messages.GetAllGeodatabaseItems;
import nl.idgis.publisher.provider.sde.messages.GetGeodatabaseItem;
import nl.idgis.publisher.utils.FutureUtils;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class GeodatabaseItems extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database;
	
	private FutureUtils f;
	
	public GeodatabaseItems(ActorRef database) {
		this.database = database;
	}
	
	public static Props props(ActorRef database) {
		return Props.create(GeodatabaseItems.class, database);
	}
	

	@Override
	public void preStart() throws Exception {
		f = new FutureUtils(getContext());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof GetAllGeodatabaseItems) {
			handleGetAllGeodatabaseItems((GetAllGeodatabaseItems)msg);
		} else if(msg instanceof GetGeodatabaseItem) {
			handleGetGeodatabaseItem((GetGeodatabaseItem)msg);
		}
		
	}


	private void handleGetGeodatabaseItem(GetGeodatabaseItem msg) {
		
		
		// TODO Auto-generated method stub
		
	}
	
	private void handleStartTransactionResponse(ActorRef sender, Object msg) {
		if(msg instanceof TransactionCreated) {
			log.debug("transaction created");
			
			ActorRef transaction = ((TransactionCreated)msg).getActor();
			List<DatabaseColumnInfo> columns = new ArrayList<>();
			columns.add(new DatabaseColumnInfo("UUID", "CHAR"));
			columns.add(new DatabaseColumnInfo("PHYSICALNAME", "CHAR"));
			//columns.add(new DatabaseColumnInfo("DOCUMENTATION", "CLOB"));
			//columns.add(new DatabaseColumnInfo("DEFINITION", "CLOB"));
			transaction.tell(new FetchTable("SDE.GDB_ITEMS_VW", columns, 1), null);
		} else {
			log.error("unexpected response: {}", msg);
		}
	}


	private void handleGetAllGeodatabaseItems(GetAllGeodatabaseItems msg) {
		f.ask(database, new StartTransaction()).thenAccept(response -> 
			handleStartTransactionResponse(getSender(), response));
	}

}
