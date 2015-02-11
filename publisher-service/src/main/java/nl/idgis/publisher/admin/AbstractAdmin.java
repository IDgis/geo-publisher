package nl.idgis.publisher.admin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;















import com.mysema.query.SimpleQuery;















import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.Timeout;














import nl.idgis.publisher.admin.messages.AddDelete;
import nl.idgis.publisher.admin.messages.AddGet;
import nl.idgis.publisher.admin.messages.AddList;
import nl.idgis.publisher.admin.messages.AddPut;
import nl.idgis.publisher.admin.messages.AddQuery;
import nl.idgis.publisher.database.AsyncDatabaseHelper;














import nl.idgis.publisher.domain.query.DeleteEntity;
import nl.idgis.publisher.domain.query.DomainQuery;
import nl.idgis.publisher.domain.query.GetEntity;
import nl.idgis.publisher.domain.query.ListEntity;
import nl.idgis.publisher.domain.query.PutEntity;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.web.Entity;
import nl.idgis.publisher.domain.web.Identifiable;
import nl.idgis.publisher.domain.web.NotFound;














import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;

public abstract class AbstractAdmin extends UntypedActor {
	
	protected static final long ITEMS_PER_PAGE = 20;
	
	protected final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	protected final ActorRef database;	
	
	protected FutureUtils f;
	
	protected AsyncDatabaseHelper db;
	
	@SuppressWarnings("rawtypes")
	protected Map<Class, Function> queryHandlers, listHandlers, getHandlers, deleteHandlers, putHandlers; 
	
	public AbstractAdmin(ActorRef database) {
		this.database = database;
	}
	
	protected void singlePage(SimpleQuery<?> query, Long page) {
		if(page == null) {
			return;
		}
		
		if(page > 0) {
			query.offset((page - 1) * ITEMS_PER_PAGE);
			query.limit(ITEMS_PER_PAGE);
		} else {
			throw new IllegalArgumentException("page parameter should be > 0");
		}
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
	
	protected <T extends Entity> Page<T> toPage(TypedList<T> list) {
		Page.Builder<T> pageBuilder = new Page.Builder<>();
		pageBuilder.addAll(list.asCollection());
		return pageBuilder.build();
	}
	
	protected <T extends Entity> List<T> toList(TypedList<T> list) {
		return list.list();
	}
	
	protected <T, U extends DomainQuery<? super T>> void addQuery(Class<U> query, Function<U, CompletableFuture<T>> func) {	
		queryHandlers.put(query, func);
		getContext().parent().tell(new AddQuery(query), getSelf());
	}
	
	protected <T extends Entity> void addList(Class<? super T> entity, Supplier<CompletableFuture<Page<T>>> func) {
		addList(entity, (Long page) -> func.get());
	}
	
	protected <T extends Entity> void addList(Class<? super T> entity, Function<Long, CompletableFuture<Page<T>>> func) {	
		listHandlers.put(entity, func);
		getContext().parent().tell(new AddList(entity), getSelf());
	}
	
	protected <T extends Entity> void addGet(Class<? super T> entity, Function<String, CompletableFuture<Optional<T>>> func) {	
		getHandlers.put(entity, func);
		getContext().parent().tell(new AddGet(entity), getSelf());
	}
	
	protected <T extends Identifiable> void addDelete(Class<? super T> entity, Function<String, CompletableFuture<Response<?>>> func) {
		deleteHandlers.put(entity, func);
		getContext().parent().tell(new AddDelete(entity), getSelf());
	}
	
	protected <T extends Identifiable> void addPut(Class<? super T> entity, Function<T, CompletableFuture<Response<?>>> func) {
		putHandlers.put(entity, func);
		getContext().parent().tell(new AddPut(entity), getSelf());
	}
	
	protected abstract void preStartAdmin();
	
	@Override
	public final void preStart() throws Exception {
		f = new FutureUtils(getContext().dispatcher(), Timeout.apply(15000));		
		db = new AsyncDatabaseHelper(database, f, log);
		
		queryHandlers = new HashMap<>();
		listHandlers = new HashMap<>();
		getHandlers = new HashMap<>();
		deleteHandlers = new HashMap<>();
		putHandlers = new HashMap<>();
		
		preStartAdmin();
	}
	
	private void toSender(CompletableFuture<?> future) throws Exception {
		ActorRef sender = getSender(), self = getSelf();		
		future.whenComplete((resp, t) -> {
			if(t != null) {
				StringWriter sw = new StringWriter();
				t.printStackTrace(new PrintWriter(sw));
				log.error("failure: {}", sw);
			} else {
				log.debug("sending response: {}", resp);				
				sender.tell(resp, self);
			}
		});
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		if(msg instanceof GetEntity) {
			Class<?> entity = ((GetEntity<?>)msg).cls();
			
			@SuppressWarnings("unchecked")
			Function<String, CompletableFuture<Optional<Object>>> getHandler = getHandlers.get(entity);
			if(getHandler == null) {
				log.debug("get entity not handled: {}", entity);
				
				unhandled(msg);
			} else {
				log.debug("handling get entity: {}", entity);
				
				toSender(getHandler.apply(((GetEntity<?>)msg).id())
					.thenApply(resp -> resp.orElse(new NotFound())));
			}
		} else if(msg instanceof ListEntity) {
			Class<?> entity = ((ListEntity<?>)msg).cls();
			
			@SuppressWarnings("unchecked")
			Function<Long, CompletableFuture<?>> listHandler = listHandlers.get(entity);
			if(listHandler == null) {
				log.debug("list entity not handled: {}", entity);
				
				unhandled(msg);
			} else {
				log.debug("handling list entity: {}", entity);
				
				toSender(listHandler.apply(((ListEntity<?>)msg).page()));
			}
		} else if(msg instanceof DeleteEntity) {
			Class<?> entity = ((DeleteEntity<?>)msg).cls();
			
			@SuppressWarnings("unchecked")
			Function<String, CompletableFuture<?>> deleteHandler = deleteHandlers.get(entity);
			if(deleteHandler == null) {
				log.debug("delete not handled: {}", entity);
				
				unhandled(msg);
			} else {
				log.debug("handling delete: {}", entity);
				
				toSender(deleteHandler.apply(((DeleteEntity<?>)msg).id()));
			}
		} else if(msg instanceof PutEntity) {
			Object value = ((PutEntity<?>)msg).value();
			Class<?> entity = value.getClass();
			
			@SuppressWarnings("unchecked")
			Function<Object, CompletableFuture<?>> putHandler = putHandlers.get(entity);
			if(putHandler == null) {
				log.debug("put not handled: {}", entity);
				
				unhandled(msg);
			} else {
				log.debug("handling put: {}", entity);
				
				toSender(putHandler.apply(value));
			}
		} else if(msg instanceof DomainQuery) {
			Class<?> clazz = msg.getClass();
			
			@SuppressWarnings("unchecked")
			Function<DomainQuery<?>, CompletableFuture<?>> queryHandler = queryHandlers.get(clazz);
			if(queryHandler == null) {
				log.debug("query not handled: {}", msg);
				
				unhandled(msg);
			} else {
				log.debug("handling query: {}", msg);
				
				toSender(queryHandler.apply((DomainQuery<?>)msg));
			}
		} else {
			unhandled(msg);
		}
	}

}
