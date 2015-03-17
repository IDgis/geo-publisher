package controllers;

import static models.Domain.from;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import models.Domain;
import models.Domain.Function;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.web.TiledLayer;
import play.Logger;
import play.Play;
import play.data.Form;
import play.data.validation.Constraints;
import play.libs.Akka;
import play.libs.F;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.tiledlayers.form;
import views.html.tiledlayers.list;
import actions.DefaultAuthenticator;
import akka.actor.ActorSelection;

@Security.Authenticated (DefaultAuthenticator.class)
public class Tiledlayers extends Controller {

	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");

	private static Promise<Result> renderCreateForm (final Form<TiledLayerForm> tiledlayerForm) {
		 return Promise.promise(new F.Function0<Result>() {
             @Override
             public Result apply() throws Throwable {
                 return ok (form.render (tiledlayerForm, true));
             }
         });
	}
	
	public static Promise<Result> submitCreateUpdate () {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		final Form<TiledLayerForm> form = Form.form (TiledLayerForm.class).bindFromRequest ();
		Logger.debug ("submit TiledLayer: " + form.field("name").value());
		
		// validation
		if (form.hasErrors ()) {
			return renderCreateForm (form);
		}
		
		final TiledLayerForm tiledlayerForm = form.get ();
		final TiledLayer tiledlayer = new TiledLayer(tiledlayerForm.id, "",
				tiledlayerForm.metaWidth, tiledlayerForm.metaHeight, 
				tiledlayerForm.expireCache, tiledlayerForm.expireClients,
				tiledlayerForm.gutter, tiledlayerForm.getMimeFormats()
				);
		
		return from (database)
			.put(tiledlayer)
			.executeFlat (new Function<Response<?>, Promise<Result>> () {
				@Override
				public Promise<Result> apply (final Response<?> response) throws Throwable {
					if (CrudOperation.CREATE.equals (response.getOperation())) {
						Logger.debug ("Created tiledlayer " + tiledlayer);
						flash ("success", Domain.message("web.application.page.tiledlayers.name") + " " + Domain.message("web.application.added").toLowerCase());
					}else{
						Logger.debug ("Updated tiledlayer " + tiledlayer);
						flash ("success", Domain.message("web.application.page.tiledlayers.name") + " " + Domain.message("web.application.updated").toLowerCase());
					}
					return Promise.pure (redirect (routes.Tiledlayers.list ()));
				}
			});
	}
	
	public static Promise<Result> list () {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);

		Logger.debug ("list TiledLayers ");
		
		return from (database)
			.list (TiledLayer.class)
			.execute (new Function<Page<TiledLayer>, Result> () {
				@Override
				public Result apply (final Page<TiledLayer> tiledlayers) throws Throwable {
					return ok (list.render (tiledlayers));
				}
			});
	}

	public static Promise<Result> create () {
		Logger.debug ("create TiledLayer");
		final Form<TiledLayerForm> tiledlayerForm = Form.form (TiledLayerForm.class).fill (new TiledLayerForm ());
		
		return renderCreateForm (tiledlayerForm);
	}
	
	public static Promise<Result> edit (final String tiledlayerId) {
		Logger.debug ("edit TiledLayer: " + tiledlayerId);
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from (database)
			.get (TiledLayer.class, tiledlayerId)
			.execute (new Function<TiledLayer, Result> () {

				@Override
				public Result apply (final TiledLayer tiledlayer) throws Throwable {
					final Form<TiledLayerForm> tiledlayerForm = Form
							.form (TiledLayerForm.class)
							.fill (new TiledLayerForm (tiledlayer, tiledlayerId));
					
					Logger.debug ("Edit tiledlayerForm: " + tiledlayerForm);						

					return ok (form.render (tiledlayerForm, false));
				}
			});
	}

	public static Promise<Result> delete(final String tiledlayerId){
		Logger.debug ("delete TiledLayer " + tiledlayerId);
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		from(database).delete(TiledLayer.class, tiledlayerId)
		.execute(new Function<Response<?>, Result>() {
			
			@Override
			public Result apply(Response<?> a) throws Throwable {
				return redirect (routes.Tiledlayers.list ());
			}
		});
		
		return Promise.pure (redirect (routes.Tiledlayers.list ()));
	}
	
	
	public static class TiledLayerForm {
		
		private static final Integer META_WIDTH_DEFAULT = 4;

		private static final Integer META_HEIGTH_DEFAULT = 4;

		private static final Integer EXPIRE_CACHE_DEFAULT = 0;

		private static final Integer EXPIER_CLIENTS_DEFAULT = 0;

		private static final Integer GUTTER_DEFAULT = 0;

		private static final String GIF  = "image/gif";
		private static final String JPG  = "image/jpeg";
		private static final String PNG8 = "image/png; mode=8bit";
		private static final String PNG  = "image/png";
		
		private String id;
		private TiledLayer tiledLayer;
		private Boolean png  = false;
		private Boolean png8 = false;
		private Boolean jpg  = false;
		private Boolean gif  = false;
		private Integer metaWidth = META_WIDTH_DEFAULT;
		private Integer metaHeight = META_HEIGTH_DEFAULT;
		private Integer expireCache = EXPIRE_CACHE_DEFAULT;
		private Integer expireClients = EXPIER_CLIENTS_DEFAULT;
		private Integer gutter = GUTTER_DEFAULT;

		
		public TiledLayerForm(){
			super();
			this.id = UUID.randomUUID().toString();
		}

		public TiledLayerForm(final TiledLayer tl, final String tiledLayerId){
			this();
			this.id = tiledLayerId;
			this.metaWidth = tl.metaWidth();
			this.metaHeight = tl.metaHeight();
			this.expireCache = tl.expireCache();
			this.expireClients = tl.expireClients();
			this.gutter = tl.gutter();
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public TiledLayer getTiledLayer() {
			return new TiledLayer(getId(), "", getMetaWidth(), getMetaHeight(), getExpireCache(), getExpireClients(), getGutter(), getMimeFormats() );
		}

		public void setTiledLayer(TiledLayer tiledLayer) {
			this.tiledLayer = tiledLayer;
			if (tiledLayer == null){
				this.metaWidth = META_WIDTH_DEFAULT;
				this.metaHeight = META_HEIGTH_DEFAULT;
				this.expireCache = EXPIRE_CACHE_DEFAULT;
				this.expireClients = EXPIER_CLIENTS_DEFAULT;
				this.gutter = GUTTER_DEFAULT;
				setMimeFormats(null);
			} else {
				this.metaWidth = tiledLayer.metaWidth() == null ? META_WIDTH_DEFAULT : tiledLayer.metaWidth();
				this.metaHeight = tiledLayer.metaHeight() == null ? META_HEIGTH_DEFAULT : tiledLayer.metaHeight();
				this.expireCache = tiledLayer.expireCache() == null ? EXPIRE_CACHE_DEFAULT : tiledLayer.expireCache();
				this.expireClients = tiledLayer.expireClients() == null ? EXPIER_CLIENTS_DEFAULT : tiledLayer.expireClients();
				this.gutter = tiledLayer.gutter() == null ? GUTTER_DEFAULT : tiledLayer.gutter();
				setMimeFormats(tiledLayer.mimeformats());
			}
		}


		public List<String> getMimeFormats() {
			List<String> mimeFormats = new ArrayList<String>();
			if (getPng()) mimeFormats.add(PNG);
			if (getPng8()) mimeFormats.add(PNG8);
			if (getJpg()) mimeFormats.add(JPG);
			if (getGif()) mimeFormats.add(GIF);
			return mimeFormats;
		}

		public void setMimeFormats(List<String> mimeFormats) {
			setPng(false);
			setPng8(false);
			setJpg(false);
			setGif(false);
			if (mimeFormats==null) return;
			for (String string : mimeFormats) {
				if (string.equals(PNG)) setPng(true);
				if (string.equals(PNG8)) setPng8(true);
				if (string.equals(JPG)) setJpg(true);
				if (string.equals(GIF)) setGif(true);
			}
		}

		public Boolean getPng() {
			return png;
		}

		public void setPng(Boolean png) {
			this.png = png;
		}

		public Boolean getPng8() {
			return png8;
		}

		public void setPng8(Boolean png8) {
			this.png8 = png8;
		}

		public Boolean getJpg() {
			return jpg;
		}

		public void setJpg(Boolean jpg) {
			this.jpg = jpg;
		}

		public Boolean getGif() {
			return gif;
		}

		public void setGif(Boolean gif) {
			this.gif = gif;
		}

		public Integer getMetaWidth() {
			return metaWidth;
		}

		public void setMetaWidth(Integer metaWidth) {
			this.metaWidth = metaWidth;
		}

		public Integer getMetaHeight() {
			return metaHeight;
		}

		public void setMetaHeight(Integer metaHeight) {
			this.metaHeight = metaHeight;
		}

		public Integer getExpireCache() {
			return expireCache;
		}

		public void setExpireCache(Integer expireCache) {
			this.expireCache = expireCache;
		}

		public Integer getExpireClients() {
			return expireClients;
		}

		public void setExpireClients(Integer expireClients) {
			this.expireClients = expireClients;
		}

		public Integer getGutter() {
			return gutter;
		}

		public void setGutter(Integer gutter) {
			this.gutter = gutter;
		}

		@Override
		public String toString() {
			return "TiledLayerForm [id=" + id + ", tiledLayer="
					+ tiledLayer + ", png=" + png + ", png8=" + png8 + ", jpg=" + jpg + ", gif=" + gif + ", metaWidth="
					+ metaWidth + ", metaHeight=" + metaHeight + ", expireCache=" + expireCache + ", expireClients="
					+ expireClients + ", gutter=" + gutter + "]";
		}

		
	}
}