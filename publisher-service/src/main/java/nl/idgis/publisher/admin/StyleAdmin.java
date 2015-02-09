package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QStyle.style;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import nl.idgis.publisher.domain.response.Page;
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
	
}
