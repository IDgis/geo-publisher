package nl.idgis.publisher.admin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mysema.query.SimpleQuery;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.Timeout;

import nl.idgis.publisher.database.AsyncDatabaseHelper;

import nl.idgis.publisher.domain.query.DomainQuery;
import nl.idgis.publisher.domain.query.GetEntity;
import nl.idgis.publisher.domain.query.ListEntity;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Entity;

import nl.idgis.publisher.utils.FutureUtils;

public abstract class AbstractAdmin extends UntypedActor {
	
	private static final long ITEMS_PER_PAGE = 20;
	
	protected final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database;	
	
	protected FutureUtils f;
	
	protected AsyncDatabaseHelper db;
	
	@SuppressWarnings("rawtypes")
	protected Map<Class, Function> queryHandlers, getHandlers;
	
	@SuppressWarnings("rawtypes")
	protected Map<Class, Supplier> listHandlers; 
	
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
	
	protected <T, U extends DomainQuery<T>> void addQuery(Class<U> query, Function<? super U, CompletableFuture<? extends T>> func) {	
		queryHandlers.put(query, func);
	}
	
	protected <T extends Entity> void addList(Class<T> entity, Supplier<CompletableFuture<Page<? extends T>>> func) {	
		listHandlers.put(entity, func);
	}
	
	protected <T extends Entity> void addGet(Class<T> entity, Function<String, CompletableFuture<? extends T>> func) {	
		getHandlers.put(entity, func);
	}
	
	protected abstract void addQueries();
	
	@Override
	public final void preStart() throws Exception {
		f = new FutureUtils(getContext().dispatcher(), Timeout.apply(15000));		
		db = new AsyncDatabaseHelper(database, f, log);
		
		queryHandlers = new HashMap<>();
		listHandlers = new HashMap<>();
		getHandlers = new HashMap<>();
		
		addQueries();
	}
	
	private void toSender(CompletableFuture<?> future) {
		ActorRef sender = getSender(), self = getSelf();
		future.thenAccept(resp -> sender.tell(resp, self));
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		Class<?> clazz = msg.getClass();
		
		if(msg instanceof GetEntity) {
			Class<?> entity = ((GetEntity<?>)msg).cls();
			
			@SuppressWarnings("unchecked")
			Function<String, CompletableFuture<?>> getHandler = getHandlers.get(entity);
			if(getHandler == null) {
				log.debug("forwarding get entity to parent: {}", entity);
				
				getContext().parent().forward(msg, getContext());
			} else {
				log.debug("handling get entity: {}", entity);
				
				toSender(getHandler.apply(((GetEntity<?>)msg).id()));
			}
		} else if(msg instanceof ListEntity) {
			Class<?> entity = ((ListEntity<?>)msg).cls();
			
			@SuppressWarnings("unchecked")
			Supplier<CompletableFuture<?>> listHandler = listHandlers.get(entity);
			if(listHandler == null) {
				log.debug("forwarding list entity to parent: {}", entity);
				
				getContext().parent().forward(msg, getContext());
			} else {
				log.debug("handling list entity: {}", entity);
				
				toSender(listHandler.get());
			}
		} else if(msg instanceof DomainQuery) {
			@SuppressWarnings("unchecked")
			Function<DomainQuery<?>, CompletableFuture<?>> queryHandler = queryHandlers.get(clazz);
			if(queryHandler == null) {
				log.debug("forwarding query to parent: {}", msg);
				
				getContext().parent().forward(msg, getContext());
			} else {
				log.debug("handling query: {}", msg);
				
				toSender(queryHandler.apply((DomainQuery<?>)msg));
			}
		} else {
			unhandled(msg);
		}
	}

}
