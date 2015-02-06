package controllers;

import nl.idgis.publisher.domain.web.TiledLayer;
import play.Logger;
import play.data.validation.Constraints;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.tiledlayers.form;
import views.html.tiledlayers.list;
import actions.DefaultAuthenticator;

@Security.Authenticated (DefaultAuthenticator.class)
public class Tiledlayers extends Controller {

	public static Promise<Result> createForm () {
		Logger.debug ("create Tiled layer");
		
		return Promise.pure (ok (form.render ()));
	}
	
	public static Promise<Result> list () {
		Logger.debug ("list Tiled lagen ");
		
		return Promise.pure (ok (list.render ()));
	}
	
	public static class TiledlayerForm {
		
		@Constraints.Required
		private String id;
		@Constraints.Required
		@Constraints.MinLength (1)
		private String name;
		@Constraints.Required
		private Boolean enabled = false;
		private String mimeFormats;
		private String abstractText;
		@Constraints.Required
		private Integer metaWidth = 4;
		@Constraints.Required
		private Integer metaHeight = 4;
		@Constraints.Required
		private Integer expireCache = 0;
		@Constraints.Required
		private Integer expireClients = 0;
		@Constraints.Required
		private Integer gutter = 0;
		
		public TiledlayerForm(){
			super();
		}

		public TiledlayerForm(final TiledLayer tl){
			this.id = tl.id();
			this.name = tl.name();
			this.enabled = tl.enabled();
			this.mimeFormats = tl.mimeFormats();
			this.abstractText = tl.abstractText();
			this.metaWidth = tl.metaWidth();
			this.metaHeight = tl.metaHeight();
			this.expireCache = tl.expireCache();
			this.expireClients = tl.expireClients();
			this.gutter = tl.gutter();
		}
	}
}