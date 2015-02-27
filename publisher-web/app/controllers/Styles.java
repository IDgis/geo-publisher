package controllers;

import static models.Domain.from;

import java.io.IOException;
import java.io.StringReader;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import models.Domain;
import models.Domain.Function;
import models.Domain.Function2;
import nl.idgis.publisher.domain.query.ListStyles;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.web.Layer;
import nl.idgis.publisher.domain.web.LayerGroup;
import nl.idgis.publisher.domain.web.Style;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;

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
import actions.DefaultAuthenticator;
import akka.actor.ActorSelection;
import controllers.Styles.SimpleErrorHandler;
import controllers.Styles.StyleForm;
import views.html.styles.form;
import views.html.styles.list;


@Security.Authenticated (DefaultAuthenticator.class)
public class Styles extends Controller {
	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");
	private final static String ID="#CREATE_STYLE#";
	
	 
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
		return from (database)
			.list (Style.class)
			.executeFlat (new Function<Page<Style>, Promise<Result>> () {
	
				@Override
				public Promise<Result> apply (final Page<Style> styles) throws Throwable {
					final Form<StyleForm> form = Form.form (StyleForm.class).bindFromRequest ();
					Logger.debug ("submit Style: " + form.field("name").value());
					
					// validation start
					if (form.field("name").value().length() == 1 ) {
						form.reject("name", Domain.message("web.application.page.styles.form.field.name.validation.error", "1"));
					}
					if (form.field("id").value().equals(ID)){
						for (Style style : styles.values()) {
							if (form.field("name").value().equals(style.name())){
								form.reject("name", Domain.message("web.application.page.styles.form.field.name.exists",  style.name()));
							}
						}
					}
					boolean validXml = isValidXml(form.field("definition").value());
					if (!validXml){ 
						form.reject("definition", Domain.message("web.application.page.styles.form.field.definition.validation.error", form.field("format").value()));
					}
					if (form.hasErrors ()) {
						return renderCreateForm (form);
					}
					// validation end
					
					final StyleForm styleForm = form.get ();
					final Style style = new Style(styleForm.id, styleForm.name, styleForm.definition, styleForm.styleType);
					
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
								return Promise.pure (redirect (routes.Styles.list (null, 1)));
							}
					});
				}
			});
	}
	

	private static boolean isValidXml(String xmlContent) {
		boolean isValid = true;
//        try {
//            SAXParserFactory factory = SAXParserFactory.newInstance();
//            factory.setValidating(true);
//            factory.setNamespaceAware(true);
//
//            SAXParser parser = factory.newSAXParser();
//            parser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
//
//            XMLReader reader = parser.getXMLReader();
//            SimpleErrorHandler errorHandler = new SimpleErrorHandler();
//            reader.setErrorHandler(errorHandler);
//            Logger.debug ("START VALIDATING ....");
//            reader.parse(new InputSource(new StringReader(xmlContent)));
              // parse does not return 		
//            Logger.debug ("DONE VALIDATING .... " + isValid);
//        } catch (ParserConfigurationException | SAXException | IOException e) {
//            e.printStackTrace();
//            isValid = false;
//        }
		return (xmlContent.indexOf("xml") >= 0);
	}

	public static class SimpleErrorHandler implements ErrorHandler {
		
		public SimpleErrorHandler (){
			super();
		}
		
	    public void warning(SAXParseException e) {
	        System.out.println(e.getMessage());
	    }
	
	    public void error(SAXParseException e) {
	        System.out.println(e.getMessage());
	    }
	
	    public void fatalError(SAXParseException e) {
	        System.out.println(e.getMessage());
	    }
	}
	
	public static Promise<Result> list (final String query, final long page) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);

		Logger.debug ("list Styles ");
		
		return from (database)
			.query (new ListStyles (page, query))
			.execute (new Function<Page<Style>, Result> () {
				@Override
				public Result apply (final Page<Style> styles) throws Throwable {
					return ok (list.render (styles, query));
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
				return redirect (routes.Styles.list (null, 1));
			}
		});
		
		return Promise.pure (redirect (routes.Styles.list (null, 1)));
	}
	
	public static class StyleForm {
		@Constraints.Required
		private String id;
		@Constraints.Required
		@Constraints.MinLength (value=1)
		private String name;
		@Constraints.Required
		private String definition;
		private String styleType;
		
		
		public StyleForm (){
			super();
			this.id = ID;
		}
		
		public StyleForm (final Style style){
			this.id = style.id();
			this.name = style.name();
			this.definition = style.definition();
			this.styleType = style.styleType();
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
		public String getDefinition() {
			return definition;
		}
		public void setDefinition(String definition) {
			this.definition = definition;
		}
		public String getStyleType() {
			return styleType;
		}
		public void setStyleType(String styleType) {
			this.styleType = styleType;
		}
		
	}

	
}
