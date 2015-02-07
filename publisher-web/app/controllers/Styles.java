package controllers;

import static models.Domain.from;
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
import views.html.styles.form;
import views.html.styles.list;
import actions.DefaultAuthenticator;
import akka.actor.ActorSelection;
import models.Domain;
import models.Domain.Function;
import nl.idgis.publisher.domain.query.PutEntity;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.PutStyle;
import nl.idgis.publisher.domain.web.Style;

@Security.Authenticated (DefaultAuthenticator.class)
public class Styles extends Controller {
	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");

	// CRUD 
	private static Promise<Result> renderCreateForm (final Form<StyleForm> styleForm) {
		// No need to go to the database, because the form contains all information needed
		 return Promise.promise(new F.Function0<Result>() {
             @Override
             public Result apply() throws Throwable {
                 return ok (form.render (styleForm, true));
             }
         });
	}
	
	public static Promise<Result> submitCreateUpdate () {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		final Form<StyleForm> form = Form.form (StyleForm.class).bindFromRequest ();
		Logger.debug ("submit Style: " + form.field("name").value());
		
		// validation
		if (form.field("name").value().length() == 1 ) 
			form.reject("name", Domain.message("web.application.page.styles.form.field.name.validation.error", "1"));
		if (!isValidXml(form.field("definition").value())) 
			form.reject("definition", Domain.message("web.application.page.styles.form.field.definition.validation.error", form.field("format").value()));
		
		if (form.hasErrors ()) {
			return renderCreateForm (form);
		}
		
		final StyleForm styleForm = form.get ();
		final Style style = new Style(styleForm.id, styleForm.name, styleForm.format, styleForm.version,styleForm.definition);
		
		return from (database)
			.put(style)
			.executeFlat (new Function<Response<?>, Promise<Result>> () {
				@Override
				public Promise<Result> apply (final Response<?> response) throws Throwable {
					if (CrudOperation.CREATE.equals (response.getOperation())) {
						Logger.debug ("Created style " + style);
						flash ("success", Domain.message("web.application.page.styles.name") + " " + styleForm.getName () + " is " + Domain.message("web.application.added").toLowerCase());
					}else{
						Logger.debug ("Updated style " + style);
						flash ("success", Domain.message("web.application.page.styles.name") + " " + styleForm.getName () + " is " + Domain.message("web.application.updated").toLowerCase());
					}
					return Promise.pure (redirect (routes.Styles.list ()));
				}
			});
	}
	

	private static boolean isValidXml(String value) {
		//TODO validate against SLD or SE xsd
		return value.toLowerCase().indexOf("xml") > 0;
	}

	public static Promise<Result> list () {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);

		Logger.debug ("list Styles ");
		
		return from (database)
			.list (Style.class)
			.execute (new Function<Page<Style>, Result> () {
				@Override
				public Result apply (final Page<Style> styles) throws Throwable {
					return ok (list.render (styles));
				}
			});
	}

	public static Promise<Result> create () {
		Logger.debug ("create Style");
		final Form<StyleForm> styleForm = Form.form (StyleForm.class).fill (new StyleForm ());
		
		return renderCreateForm (styleForm);
	}
	
	public static Promise<Result> edit (final String styleId) {
		Logger.debug ("edit Style: " + styleId);
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from (database)
			.get (Style.class, styleId)
			.execute (new Function<Style, Result> () {

				@Override
				public Result apply (final Style style) throws Throwable {
					final Form<StyleForm> styleForm = Form
							.form (StyleForm.class)
							.fill (new StyleForm (style));
					
					Logger.debug ("Edit styleForm: " + styleForm);						

					return ok (form.render (styleForm, false));
				}
			});
	}

	public static Promise<Result> delete(final String styleId){
		Logger.debug ("delete Style " + styleId);
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		from(database).delete(Style.class, styleId)
		.execute(new Function<Response<?>, Result>() {
			
			@Override
			public Result apply(Response<?> a) throws Throwable {
				System.out.println("apply delete: " + a);
				return null;
			}
		});
		
		return list();
	}
	
	public static class StyleForm {
		@Constraints.Required
		private String id;
		@Constraints.Required
		@Constraints.MinLength (value=1)
		private String name;
		@Constraints.Required
		private String format;
		@Constraints.Required
		private String version;
		@Constraints.Required
		private String definition;
		
		
		public StyleForm (){
			super();
			this.id = "id";
			this.format = "SLD";
			this.version = "1.0.0";

		}
		
		public StyleForm (final Style style){
			this.id = style.id();
			this.name = style.name();
			this.format = style.format();
			this.version = style.version();
			this.definition = style.definition();
		}
		
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getFormat() {
			return format;
		}
		public void setFormat(String format) {
			this.format = format;
		}
		public String getVersion() {
			return version;
		}
		public void setVersion(String version) {
			this.version = version;
		}
		public String getDefinition() {
			return definition;
		}
		public void setDefinition(String definition) {
			this.definition = definition;
		}
		
	}

	
}
