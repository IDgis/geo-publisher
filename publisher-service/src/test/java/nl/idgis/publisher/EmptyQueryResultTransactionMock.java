package nl.idgis.publisher;

import java.util.Collections;

import akka.actor.Props;

import nl.idgis.publisher.database.messages.PerformQuery;
import nl.idgis.publisher.database.messages.Query;

import nl.idgis.publisher.utils.TypedList;

public class EmptyQueryResultTransactionMock extends TransactionMock {
	
	public static Props props() {
		return Props.create(EmptyQueryResultTransactionMock.class);
	}

	@Override
	protected void handleQuery(Query query) throws Exception {
		if(query instanceof PerformQuery) {
			getSender().tell(new TypedList<>(Object.class, Collections.emptyList()), getSelf());
		} else {
			super.handleQuery(query);
		}
	}		
	
}