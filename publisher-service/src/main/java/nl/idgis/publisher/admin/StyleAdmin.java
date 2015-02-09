package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QStyle.style;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
		addList(Style.class, this::handleListStyles);
		addGet(Style.class, this::handleGetStyle);
		addPut(Style.class, this::handlePutStyle);
	}

	private CompletableFuture<Page<Style>> handleListStyles () {
		log.debug ("handleListStyles");
		
		return 
			db.query().from(style)
			.list(new QStyle(style.identification,style.name,style.format, style.version, style.definition))
			.thenApply(this::toPage);
	}

	
	private CompletableFuture<Optional<Style>> handleGetStyle (String styleId) {
		log.debug ("handleGetStyle");
		
		return 
			db.query().from(style)
			.where(style.identification.eq(styleId))
			.singleResult(new nl.idgis.publisher.domain.web.QStyle(style.identification,style.name,style.format, style.version, style.definition));		
	}
	
	private CompletableFuture<Response<?>> handlePutStyle(Style theStyle) {

		String styleName = theStyle.name();
		log.debug ("handle update/create style: " + styleName);
		
		
		// Check if there is another style with the same name
		
		return db.transactional(tx ->
			tx.query().from(style)
			.where(style.name.eq(styleName))
			.singleResult(style.name)
			.thenCompose(msg -> {
				if (!msg.isPresent()){
					// there is no other style with the same name
					// INSERT
					log.debug("Inserting new style with name: " + styleName);
					return tx.insert(style)
					.set(style.identification, UUID.randomUUID().toString())
					.set(style.name, styleName)
					.set(style.version, theStyle.version())
					.set(style.format, theStyle.format())
					.set(style.definition, theStyle.definition())
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.CREATE, CrudResponse.OK, styleName));
					
				} else {
					// UPDATE
					log.debug("Updating style with name: " + styleName);
					
					return tx.update(style)
					.set(style.version, theStyle.version())
					.set(style.format, theStyle.format())
					.set(style.definition, theStyle.definition())
					.where(style.name.eq(styleName))
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.UPDATE, CrudResponse.OK, styleName));
				}
		}));
	}

}
