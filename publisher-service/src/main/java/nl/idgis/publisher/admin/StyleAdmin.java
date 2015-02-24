package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QStyle.style;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import nl.idgis.publisher.database.AsyncSQLQuery;
import nl.idgis.publisher.domain.query.ListStyles;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.QStyle;
import nl.idgis.publisher.domain.web.Style;
import akka.actor.ActorRef;
import akka.actor.Props;

public class StyleAdmin extends AbstractAdmin {
	
	public StyleAdmin(ActorRef database) {
		super(database); 
	}
	
	public static Props props(ActorRef database) {
		return Props.create(StyleAdmin.class, database);
	}

	@Override
	protected void preStartAdmin() {
		doList(Style.class, this::handleListStyles);
		doGet(Style.class, this::handleGetStyle);
		doPut(Style.class, this::handlePutStyle);
		doDelete(Style.class, this::handleDeleteStyle);
		doQuery (ListStyles.class, this::handleListStylesWithQuery);
	}

	private CompletableFuture<Page<Style>> handleListStyles () {
		log.debug ("handleListStyles");
		
		return 
			db.query().from(style)
			.list(new QStyle(style.identification,style.name,style.definition))
			.thenApply(this::toPage);
	}
	
	private CompletableFuture<Page<Style>> handleListStylesWithQuery (final ListStyles listStyles) {
		final AsyncSQLQuery baseQuery = db
			.query()
			.from(style)
			.orderBy (style.name.asc ());

		if (listStyles.getQuery () != null) {
			baseQuery.where (style.name.containsIgnoreCase (listStyles.getQuery ()));
		}
		
		final AsyncSQLQuery listQuery = baseQuery.clone ();
		
		singlePage (listQuery, listStyles.getPage ());
		
		return baseQuery
			.count ()
			.thenCompose ((count) -> {
				final Page.Builder<Style> builder = new Page.Builder<> ();
				
				addPageInfo (builder, listStyles.getPage (), count);
				
				return listQuery
					.list (new QStyle (style.identification, style.name, style.definition))
					.thenApply ((styles) -> {
						builder.addAll (styles.list ());
						return builder.build ();
					});
			});
	}

	
	private CompletableFuture<Optional<Style>> handleGetStyle (String styleId) {
		log.debug ("handleGetStyle: " + styleId);
		
		return 
			db.query().from(style)
			.where(style.identification.eq(styleId))
			.singleResult(new nl.idgis.publisher.domain.web.QStyle(style.identification, style.name, style.definition));		
	}
	
	private CompletableFuture<Response<?>> handlePutStyle(Style theStyle) {
		String styleId = theStyle.id();
		String styleName = theStyle.name();
		log.debug ("handle update/create style: " + styleId);
		
		return db.transactional(tx ->
			// Check if there is another style with the same id
			tx.query().from(style)
			.where(style.identification.eq(styleId))
			.singleResult(style.identification)
			.thenCompose(msg -> {
				if (!msg.isPresent()){
					// INSERT
					log.debug("Inserting new style with name: " + styleName);
					return tx.insert(style)
					.set(style.identification, UUID.randomUUID().toString())
					.set(style.name, styleName)
					.set(style.definition, theStyle.definition())
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.CREATE, CrudResponse.OK, styleName));
				} else {
					// UPDATE
					log.debug("Updating style with name: " + styleName);
					return tx.update(style)
					.set(style.definition, theStyle.definition())
					.where(style.identification.eq(styleId))
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.UPDATE, CrudResponse.OK, styleName));
				}
		}));
	}

	private CompletableFuture<Response<?>> handleDeleteStyle(String styleId) {
		log.debug ("handleDeleteStyle: " + styleId);
		return db.delete(style)
			.where(style.identification.eq(styleId))
			.execute()
			.thenApply(l -> new Response<String>(CrudOperation.DELETE, CrudResponse.OK, styleId));
	}
	
}
