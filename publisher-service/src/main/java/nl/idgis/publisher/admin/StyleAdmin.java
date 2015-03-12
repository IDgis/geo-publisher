package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QLayerStyle.layerStyle;
import static nl.idgis.publisher.database.QStyle.style;

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
		return handleListStylesWithQuery (new ListStyles (null, null));
	}
	
	private CompletableFuture<Page<Style>> handleListStylesWithQuery (final ListStyles listStyles) {
		final AsyncSQLQuery baseQuery = db
			.query()
			.from(style)
			.leftJoin(layerStyle).on(style.id.eq(layerStyle.styleId)).distinct()
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
					.list (new QStyle (style.identification, style.name, style.name, style.definition, style.styleType, layerStyle.styleId.isNotNull()))
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
			.leftJoin(layerStyle).on(style.id.eq(layerStyle.styleId)).distinct()
			.where(style.identification.eq(styleId))
			.singleResult(new nl.idgis.publisher.domain.web.QStyle(style.identification, style.name, style.name, style.definition,style.styleType, layerStyle.styleId.isNotNull()));		
	}
	
	private CompletableFuture<Response<?>> handlePutStyle(Style theStyle) {
		String styleId = theStyle.id();
		String styleName = theStyle.name();
		String styleOldName = theStyle.oldName();
		log.debug ("handle update/create style: " + styleId);
		
		return db.transactional(tx ->
			// Check if there is another style with the same id
			tx.query().from(style)
				.where(style.identification.eq(styleId))
				.singleResult(style.identification)
				.thenCompose(sId -> {
				if (!sId.isPresent()){
					// INSERT, check name
					return tx.query().from(style)
						.where(style.name.eq(styleName))
						.singleResult(style.identification)
						.thenCompose(otherStyleExists -> {
						if (otherStyleExists.isPresent()){
							// Name exists
							log.debug("[insert] Style exists with name: " + styleName);
							return f.successful(new Response<String>(CrudOperation.CREATE, CrudResponse.NOK, Style.NAME_EXISTS));
						}else{
							log.debug("Inserting new style with name: " + styleName);
							return tx.insert(style)
								.set(style.identification, UUID.randomUUID().toString())
								.set(style.name, styleName)
								.set(style.definition, theStyle.definition())
								.set(style.styleType, theStyle.styleType().name())
								.execute()
								.thenApply(l -> new Response<String>(CrudOperation.CREATE, CrudResponse.OK, styleName));
						}
					});
				} else {
					if (styleName.equals(styleOldName)){
						// UPDATE, name not changed
						log.debug("Updating style with name: " + styleName);
						return tx.update(style)
							.set(style.definition, theStyle.definition())
							.set(style.styleType, theStyle.styleType().name())
							.where(style.identification.eq(styleId))
							.execute()
							.thenApply(l -> new Response<String>(CrudOperation.UPDATE, CrudResponse.OK, styleName));
					} else {
						// UPDATE but name changed also
						return tx.query().from(style)
							.where(style.name.eq(styleName))
							.singleResult(style.identification)
							.thenCompose(otherStyleExists -> {
								if (otherStyleExists.isPresent()){
									// Name exists
									log.debug("[update] Style exists with name: " + styleName);
									return f.successful(new Response<String>(CrudOperation.UPDATE, CrudResponse.NOK, Style.NAME_EXISTS));
								}else{
									log.debug("Updating style with new name: " + styleName);
									return tx.update(style)
										.set(style.name, styleName) // set the new name	
										.set(style.definition, theStyle.definition())
										.set(style.styleType, theStyle.styleType().name())
										.where(style.identification.eq(styleId))
										.execute()
										.thenApply(l -> new Response<String>(CrudOperation.UPDATE, CrudResponse.OK, styleName));
								}
							});
					}
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
