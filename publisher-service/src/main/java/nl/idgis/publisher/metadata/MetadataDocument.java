package nl.idgis.publisher.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import scala.concurrent.Future;
import scala.concurrent.Promise;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import nl.idgis.publisher.metadata.messages.GetAlternateTitle;
import nl.idgis.publisher.metadata.messages.GetRevisionDate;
import nl.idgis.publisher.metadata.messages.GetTitle;
import nl.idgis.publisher.metadata.messages.MetadataFailure;
import nl.idgis.publisher.metadata.messages.MetadataQuery;
import nl.idgis.publisher.metadata.messages.NotValid;
import nl.idgis.publisher.xml.messages.Close;
import nl.idgis.publisher.xml.messages.GetString;
import nl.idgis.publisher.xml.messages.NotFound;
import nl.idgis.publisher.xml.messages.Query;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class MetadataDocument extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private static class QueryMapper<T> {
		
		private final Query<T> query;
		private final Mapper<T, ? extends Object> mapper;
		
		public QueryMapper(Query<T> query) {
			this(query, null);
		}
		
		public QueryMapper(Query<T> query, Mapper<T, ? extends Object> mapper) {
			this.query = query;
			this.mapper = mapper;
		}

		public Query<T> getQuery() {
			return query;
		}

		public Mapper<T, ? extends Object> getMapper() {
			return mapper;
		}
	}
	
	private final ActorRef xmlDocument;	
	private final Map<Class<? extends MetadataQuery>, List<QueryMapper<?>>> queries;

	public MetadataDocument(ActorRef xmlDocument) {
		this.xmlDocument = xmlDocument;
		
		BiMap<String, String> namespaces = HashBiMap.create();
		namespaces.put("gmd", "http://www.isotc211.org/2005/gmd");
		namespaces.put("gco", "http://www.isotc211.org/2005/gco");
		
		queries = new HashMap<>();
		queries.put(GetTitle.class, 
				Collections.<QueryMapper<?>>singletonList(
					new QueryMapper<>(
						new GetString(namespaces, 
							"/gmd:MD_Metadata" +
							"/gmd:identificationInfo" +
							"/gmd:MD_DataIdentification" +
							"/gmd:citation" +
							"/gmd:CI_Citation" +
							"/gmd:title" +
							"/gco:CharacterString"))));
		
		queries.put(GetAlternateTitle.class,
				Collections.<QueryMapper<?>>singletonList(
					new QueryMapper<>(
						new GetString(namespaces, 
							"/gmd:MD_Metadata" +
							"/gmd:identificationInfo" +
							"/gmd:MD_DataIdentification" +
							"/gmd:citation" +
							"/gmd:CI_Citation" +
							"/gmd:alternateTitle" +
							"/gco:CharacterString"))));
		
		SimpleDateFormatMapper dateTimeMapper = 
			new SimpleDateFormatMapper(
				"yyyy-MM-dd'T'HH:mm:ss",
				"yyyyMMdd'T'HH:mm:ss",
				"yyyyMMdd'T'HHmmss");
		
		SimpleDateFormatMapper dateMapper =
			new SimpleDateFormatMapper(
				"yyyy-MM-dd",
				"yyyyMMdd");
		
		queries.put(GetRevisionDate.class, 
				Arrays.<QueryMapper<?>>asList(
					new QueryMapper<>(
						new GetString(namespaces,
							getDatePath("revision") +
							"/gco:DateTime"), dateTimeMapper),
								
					new QueryMapper<>(
							new GetString(namespaces,
								getDatePath("revision") +
								"/gco:Date"), dateMapper),
								
					new QueryMapper<>(
						new GetString(namespaces,
							getDatePath("creation") +
							"/gco:DateTime"), dateTimeMapper),
								
					new QueryMapper<>(
							new GetString(namespaces,
								getDatePath("creation") +
								"/gco:Date"), dateMapper)								
						
					));
	}
	
	private String getDatePath(String codeListValue) {
		return 
				"/gmd:MD_Metadata" +
				"/gmd:identificationInfo" +
				"/gmd:MD_DataIdentification" +
				"/gmd:citation" +
				"/gmd:CI_Citation" +
				"/gmd:date" +
				"/gmd:CI_Date" +
					"[gmd:dateType" +
					"/gmd:CI_DateTypeCode" +
					"/@codeListValue" +
						"='" + codeListValue + "']" +
				"/gmd:date";
	}
	
	public static Props props(ActorRef xmlDocument) {
		return Props.create(MetadataDocument.class, xmlDocument);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Close) {
			handleClose((Close)msg);
		} else if(msg instanceof MetadataQuery) {
			handleMetadataQuery((MetadataQuery)msg);
		} else {
			unhandled(msg);
		}
	}
	
	private <T> Future<Object> dispatchQuery(final QueryMapper<T> queryMapper) {
		final Promise<Object> promise = Futures.promise();
		
		final Query<?> query = queryMapper.getQuery();
		Patterns.ask(xmlDocument, query, 15000)
			.onSuccess(new OnSuccess<Object>() {
				
				@Override
				@SuppressWarnings("unchecked")
				public void onSuccess(Object msg) throws Throwable {
					Mapper<T, ? extends Object> mapper = queryMapper.getMapper();
					
					if(msg instanceof NotFound || mapper == null) {
						promise.success(msg);
					} else {
						Object mapped = mapper.apply((T)msg);
						if(mapped == null) {
							promise.success(new NotValid<>(query.getExpression(), msg));
						} else {
							promise.success(mapped);
						}
					}
				}
				
			}, getContext().dispatcher());
		
		return promise.future();
	}

	private void handleMetadataQuery(MetadataQuery msg) {
		Class<? extends MetadataQuery> clazz = msg.getClass();
		if(queries.containsKey(clazz)) {
			log.debug("query dispatched");			
			
			List<Future<Object>> queryResults = new ArrayList<>();
			List<QueryMapper<?>> queryMappers = queries.get(clazz);
			for(QueryMapper<?> queryMapper : queryMappers) {
				queryResults.add(dispatchQuery(queryMapper));
			}
			
			final ActorRef sender = getSender();
			Futures.sequence(queryResults, getContext().dispatcher())
				.onSuccess(new OnSuccess<Iterable<Object>>() {

					@Override
					public void onSuccess(Iterable<Object> msgs) throws Throwable {
						ArrayList<NotFound> notFound = new ArrayList<>();
						ArrayList<NotValid<?>> notValid = new ArrayList<>();
						
						Object result = null;
						for(Object msg : msgs) {
							if(msg instanceof NotFound) {
								notFound.add((NotFound) msg);
							} else if(msg instanceof NotValid) {
								notValid.add((NotValid<?>) msg);
							} else if(result == null) {
								result = msg;
							} else {
								log.warning("additional (ignored) query result: " + result);
							}
						}
						
						if(result == null) {
							MetadataFailure failure = new MetadataFailure(notValid, notFound);							
							log.debug("metadata parsing failed: " + failure);
							
							sender.tell(failure, getSelf());
						} else {
							sender.tell(result, getSelf());
						}
					}
				}, getContext().dispatcher());
		} else {
			log.error("unknown metadata query: " + msg);
		}
	}

	private void handleClose(Close msg) {
		log.debug("closing metadata document");
		
		final ActorRef sender = getSender();
		Patterns.ask(xmlDocument, msg, 15000)
			.onSuccess(new OnSuccess<Object>() {

				@Override
				public void onSuccess(Object msg) throws Throwable {
					log.debug("xml document closed");
					
					sender.tell(msg, getSelf());
					getContext().stop(getSelf());
				}
				
			}, getContext().dispatcher());
	}

}
