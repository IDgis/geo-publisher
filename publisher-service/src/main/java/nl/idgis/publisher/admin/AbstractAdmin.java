package nl.idgis.publisher.admin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.mysema.query.SimpleQuery;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.Timeout;

import nl.idgis.publisher.database.AsyncDatabaseHelper;

import nl.idgis.publisher.protocol.messages.Failure;

import nl.idgis.publisher.domain.query.DomainQuery;
import nl.idgis.publisher.domain.response.Page;

import nl.idgis.publisher.utils.FutureUtils;

public abstract class AbstractAdmin extends UntypedActor {
	
	private static final long ITEMS_PER_PAGE = 20;
	
	protected final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database;	
	
	protected FutureUtils f;
	
	protected AsyncDatabaseHelper db;
	
	@SuppressWarnings("rawtypes")
	protected Map<Class, Function> queryHandlers;
	
	public AbstractAdmin(ActorRef database) {
		this.database = database;
	}
	
	protected void singlePage(SimpleQuery<?> query, Long page) {
		if(page == null) {
			return;
		}
		
		query.offset((page - 1) * ITEMS_PER_PAGE);
		query.limit(ITEMS_PER_PAGE);
	}
	
	protected void addPageInfo(Page.Builder<?> pageBuilder, Long page, long count) {
		if(page != null) {
			long pages = count / ITEMS_PER_PAGE + Math.min(1, count % ITEMS_PER_PAGE);
			
			if(pages > 1) {
				pageBuilder
					.setHasMorePages(true)
					.setPageCount(pages)
					.setCurrentPage(page);
			}
		}
	}
	
	protected <T, U extends DomainQuery<T>> void query(Class<U> query, Function<? super U, CompletableFuture<? extends T>> func) {	
		queryHandlers.put(query, func);
	}
	
	protected abstract void queries();
	
	@Override
	public final void preStart() throws Exception {
		f = new FutureUtils(getContext().dispatcher(), Timeout.apply(15000));		
		db = new AsyncDatabaseHelper(database, f, log);
		
		queryHandlers = new HashMap<>();
		queries();
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		Class<?> clazz = msg.getClass();
		
		if(msg instanceof DomainQuery) {
			@SuppressWarnings("unchecked")
			Function<DomainQuery<?>, CompletableFuture<?>> queryHandler = queryHandlers.get(clazz);
			if(queryHandler == null) {
				log.debug("forwarding to parent: {}", msg);
				
				getContext().parent().forward(msg, getContext());
			} else {
				log.debug("handling query: {}", msg);
				
				ActorRef sender = getSender(), self = getSelf();
				queryHandler.apply((DomainQuery<?>)msg).whenComplete((resp, t) -> {
					if(t != null) {
						sender.tell(new Failure(t), self);
					} else {
						sender.tell(resp, self);
					}
				});
			}
		} else {
			unhandled(msg);
		}
	}

}
