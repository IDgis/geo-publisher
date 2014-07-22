package models;

import static akka.pattern.Patterns.ask;
import static play.libs.F.Promise.sequence;
import static play.libs.F.Promise.wrap;

import java.util.ArrayList;
import java.util.List;

import nl.idgis.publisher.domain.query.DeleteEntity;
import nl.idgis.publisher.domain.query.DomainQuery;
import nl.idgis.publisher.domain.query.GetEntity;
import nl.idgis.publisher.domain.query.ListEntity;
import nl.idgis.publisher.domain.query.PutEntity;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Entity;
import nl.idgis.publisher.domain.web.Identifiable;
import play.libs.F.Function;
import play.libs.F.Function2;
import play.libs.F.Function3;
import play.libs.F.Promise;
import akka.actor.ActorSelection;

public class Domain {

	public static DomainInstance from (final ActorSelection domainActor) {
		return from (domainActor, 15000);
	}
	
	public static DomainInstance from (final ActorSelection domainActor, final int timeout) {
		return new DomainInstance (domainActor, timeout);
	}
	
	public static interface Query {
	}
	
	public static interface Queryable {
		<T extends Entity> Query get (Class<T> cls, String id);
		<T extends Entity> Query list (Class<T> cls);
		<T extends Entity> Query list (Class<T> cls, long page);
		
		<T extends Identifiable> Query put (T value);
		<T extends Identifiable> Query delete (Class<? extends T> cls, String id);
		
		<T> Query query (DomainQuery<T> domainQuery);
	}
	
	public final static class DomainInstance implements Queryable {
		protected final ActorSelection domainActor;
		protected final int timeout;
		
		public DomainInstance (final ActorSelection domainActor, final int timeout) {
			if (domainActor == null) {
				throw new NullPointerException ("domainActor cannot be null");
			}
			
			this.domainActor = domainActor;
			this.timeout = timeout;
		}
		
		public ActorSelection domainActor () {
			return this.domainActor;
		}

		@Override
		public <T extends Entity> Query1<T> get (final Class<T> cls, final String id) {
			return query (new GetEntity<> (cls, id));
		}

		@Override
		public <T extends Entity> Query1<Page<T>> list (Class<T> cls) {
			return query (new ListEntity<> (cls, 0));
		}
		
		@Override
		public <T extends Entity> Query1<Page<T>> list (Class<T> cls, long page) {
			return query (new ListEntity<> (cls, page));
		}
		
		@Override
		public <T extends Identifiable> Query1<Boolean> put (final T value) {
			return query (new PutEntity<> (value));
		}
		
		@Override
		public <T extends Identifiable> Query1<Boolean> delete (final Class<? extends T> cls, final String id) {
			return query (new DeleteEntity<> (cls, id));
		}

		@Override
		public <T> Query1<T> query (final DomainQuery<T> domainQuery) {
			return new Query1<T> (this, domainQuery);
		}
	}

	public final static class Query1<A> implements Query, Queryable {

		protected final DomainInstance domainInstance;
		protected final DomainQuery<A> query;
		
		public Query1 (final DomainInstance domainInstance, final DomainQuery<A> query) {
			this.domainInstance = domainInstance;
			this.query = query;
		}
		
		@Override
		public <T extends Entity> Query2<A, T> get (final Class<T> cls, final String id) {
			return query (new GetEntity<> (cls, id));
		}
		
		@Override
		public <T extends Entity> Query2<A, Page<T>> list (final Class<T> cls) {
			return query (new ListEntity<> (cls, 0));
		}
		
		@Override
		public <T extends Entity> Query2<A, Page<T>> list (final Class<T> cls, long page) {
			return query (new ListEntity<> (cls, page));
		}
		
		@Override
		public <T extends Identifiable> Query2<A, Boolean> put (final T value) {
			return query (new PutEntity<> (value));
		}
		
		@Override
		public <T extends Identifiable> Query2<A, Boolean> delete (final Class<? extends T> cls, final String id) {
			return query (new DeleteEntity<> (cls, id));
		}
		
		@Override
		public <T> Query2<A, T> query (final DomainQuery<T> domainQuery) {
			return new Query2<A, T> (this, domainQuery);
		}
		
		public <R> Promise<R> execute (final Function<A, R> callback) {
			final Promise<Object> promise =
					wrap (
						ask (
							domainInstance.domainActor, 
							query, 
							domainInstance.timeout
						)
					);

			return promise
				.map (new Function<Object, R> () {
					@Override
					public R apply (final Object a) throws Throwable {
						@SuppressWarnings("unchecked")
						final A value = (A)a;
						return callback.apply (value);
					}
				});
		}
		
		public <R> Promise<R> execute (final Function<A, R> callback, final Function<Throwable, R> errorCallback) {
			return execute (callback).recover (new Function<Throwable, R> () {
					@Override
					public R apply (final Throwable a) throws Throwable {
						return errorCallback.apply (a);
					}
				});
		}
	}
	
	public final static class Query2<A, B> implements Query, Queryable {

		protected final Query1<A> baseQuery;
		protected final DomainQuery<B> query;
		
		public Query2 (final Query1<A> baseQuery, final DomainQuery<B> query) {
			this.baseQuery = baseQuery;
			this.query = query;
		}
		
		@Override
		public <T extends Entity> Query3<A, B, T> get (final Class<T> cls, final String id) {
			return query (new GetEntity<> (cls, id));
		}
		
		@Override
		public <T extends Entity> Query3<A, B, Page<T>> list (final Class<T> cls) {
			return query (new ListEntity<> (cls, 0));
		}
		
		@Override
		public <T extends Entity> Query3<A, B, Page<T>> list (final Class<T> cls, long page) {
			return query (new ListEntity<> (cls, page));
		}
		
		@Override
		public <T extends Identifiable> Query3<A, B, Boolean> put (final T value) {
			return query (new PutEntity<> (value));
		}
		
		@Override
		public <T extends Identifiable> Query3<A, B, Boolean> delete (final Class<? extends T> cls, final String id) {
			return query (new DeleteEntity<> (cls, id));
		}
		
		@Override
		public <T> Query3<A, B, T> query (final DomainQuery<T> domainQuery) {
			return new Query3<> (this, domainQuery);
		}
		
		public <R> Promise<R> execute (final Function2<A, B, R> callback) {
			final List<Promise<Object>> promises = new ArrayList<> ();
			promises.add (
					wrap (
						ask (
							baseQuery.domainInstance.domainActor, 
							baseQuery.query, 
							baseQuery.domainInstance.timeout
						)
					)
				);
			promises.add (
					wrap (
						ask (
							baseQuery.domainInstance.domainActor, 
							query, 
							baseQuery.domainInstance.timeout
						)
					)
				);

			return sequence (promises)
				.map (new Function<List<Object>, R> () {
					@Override
					public R apply (final List<Object> list) throws Throwable {
						@SuppressWarnings("unchecked")
						final A a = (A)list.get (0);
						
						@SuppressWarnings("unchecked")
						final B b = (B)list.get (0);
						
						return callback.apply (a, b);
					}
				});
		}
		
		public <R> Promise<R> execute (final Function2<A, B, R> callback, final Function<Throwable, R> errorCallback) {
			return execute (callback).recover (new Function<Throwable, R> () {
					@Override
					public R apply (final Throwable a) throws Throwable {
						return errorCallback.apply (a);
					}
				});
		}
	}
	
	public final static class Query3<A, B, C> implements Query, Queryable {
		
		protected final Query2<A, B> baseQuery;
		protected final DomainQuery<C> query;
		
		public Query3 (final Query2<A, B> baseQuery, final DomainQuery<C> query) {
			this.baseQuery = baseQuery;
			this.query = query;
		}
		
		@Override
		public <T extends Entity> Query4<A, B, C, T> get (final Class<T> cls, final String id) {
			return query (new GetEntity<> (cls, id));
		}
		
		@Override
		public <T extends Entity> Query4<A, B, C, Page<T>> list (final Class<T> cls) {
			return query (new ListEntity<> (cls, 0));
		}
		
		@Override
		public <T extends Entity> Query4<A, B, C, Page<T>> list (final Class<T> cls, long page) {
			return query (new ListEntity<> (cls, page));
		}
		
		@Override
		public <T extends Identifiable> Query4<A, B, C, Boolean> put (final T value) {
			return query (new PutEntity<> (value));
		}
		
		@Override
		public <T extends Identifiable> Query4<A, B, C, Boolean> delete (final Class<? extends T> cls, final String id) {
			return query (new DeleteEntity<> (cls, id));
		}
		
		@Override
		public <T> Query4<A, B, C, T> query (final DomainQuery<T> domainQuery) {
			return new Query4<> (this, domainQuery);
		}
		
		public <R> Promise<R> execute (final Function3<A, B, C, R> callback) {
			final List<Promise<Object>> promises = new ArrayList<> ();
			promises.add (
					wrap (
						ask (
							baseQuery.baseQuery.domainInstance.domainActor, 
							baseQuery.baseQuery.query, 
							baseQuery.baseQuery.domainInstance.timeout
						)
					)
				);
			promises.add (
					wrap (
						ask (
							baseQuery.baseQuery.domainInstance.domainActor, 
							baseQuery.query, 
							baseQuery.baseQuery.domainInstance.timeout
						)
					)
				);
			promises.add (
					wrap (
						ask (
							baseQuery.baseQuery.domainInstance.domainActor, 
							query, 
							baseQuery.baseQuery.domainInstance.timeout
						)
					)
				);
			
			return sequence (promises)
				.map (new Function<List<Object>, R> () {
					@Override
					public R apply (final List<Object> list) throws Throwable {
						@SuppressWarnings("unchecked")
						final A a = (A)list.get (0);
						
						@SuppressWarnings("unchecked")
						final B b = (B)list.get (0);
						
						@SuppressWarnings("unchecked")
						final C c = (C)list.get (0);
						
						return callback.apply (a, b, c);
					}
				});
		}
		
		public <R> Promise<R> execute (final Function3<A, B, C, R> callback, final Function<Throwable, R> errorCallback) {
			return execute (callback).recover (new Function<Throwable, R> () {
					@Override
					public R apply (final Throwable a) throws Throwable {
						return errorCallback.apply (a);
					}
				});
		}
	}
	
	public final static class Query4<A, B, C, D> implements Query {
		protected final Query3<A, B, C> baseQuery;
		protected final DomainQuery<D> query;
		
		public Query4 (final Query3<A, B, C> baseQuery, final DomainQuery<D> query) {
			this.baseQuery = baseQuery;
			this.query = query;
		}
		
		public <R> Promise<R> execute (final Function3<A, B, C, R> callback) {
			final List<Promise<Object>> promises = new ArrayList<> ();
			promises.add (
					wrap (
						ask (
							baseQuery.baseQuery.baseQuery.domainInstance.domainActor, 
							baseQuery.baseQuery.baseQuery.query, 
							baseQuery.baseQuery.baseQuery.domainInstance.timeout
						)
					)
				);
			promises.add (
					wrap (
						ask (
							baseQuery.baseQuery.baseQuery.domainInstance.domainActor, 
							baseQuery.baseQuery.query, 
							baseQuery.baseQuery.baseQuery.domainInstance.timeout
						)
					)
				);
			promises.add (
					wrap (
						ask (
							baseQuery.baseQuery.baseQuery.domainInstance.domainActor, 
							baseQuery.query, 
							baseQuery.baseQuery.baseQuery.domainInstance.timeout
						)
					)
				);
			promises.add (
					wrap (
						ask (
							baseQuery.baseQuery.baseQuery.domainInstance.domainActor, 
							query, 
							baseQuery.baseQuery.baseQuery.domainInstance.timeout
						)
					)
				);
			
			return sequence (promises)
				.map (new Function<List<Object>, R> () {
					@Override
					public R apply (final List<Object> list) throws Throwable {
						@SuppressWarnings("unchecked")
						final A a = (A)list.get (0);
						
						@SuppressWarnings("unchecked")
						final B b = (B)list.get (0);
						
						@SuppressWarnings("unchecked")
						final C c = (C)list.get (0);
						
						return callback.apply (a, b, c);
					}
				});
		}
		
		public <R> Promise<R> execute (final Function3<A, B, C, R> callback, final Function<Throwable, R> errorCallback) {
			
			return execute (callback).recover (new Function<Throwable, R> () {
					@Override
					public R apply (final Throwable a) throws Throwable {
						return errorCallback.apply (a);
					}
				});
		}
	}
	
    public static interface Function4<A, B, C, D, R> {
        public R apply (A a, B b, C c, D d) throws Throwable;
    }
}
