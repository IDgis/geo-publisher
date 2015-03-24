package controllers;

import static models.Domain.from;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Arrays;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import models.Domain;
import models.Domain.Function;
import nl.idgis.publisher.domain.query.ListStyles;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.web.Style;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import play.Logger;
import play.Play;
import play.data.Form;
import play.data.validation.Constraints;
import play.libs.Akka;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import play.mvc.Security;
import actions.DefaultAuthenticator;
import akka.actor.ActorSelection;

import com.fasterxml.jackson.databind.node.ObjectNode;

import controllers.Styles.StyleForm;
import views.html.styles.form;
import views.html.styles.list;
import views.html.styles.stylePagerHeader;
import views.html.styles.stylePagerBody;
import views.html.styles.stylePagerFooter;
import views.html.styles.uploadFileForm;


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
	
	@BodyParser.Of (value = BodyParser.MultipartFormData.class, maxLength = 1024 * 1024)
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
					if (form.field("id").value().equals(ID)){
						for (Style style : styles.values()) {
							if (form.field("name").value().trim().equals(style.name())){
								form.reject("name", Domain.message("web.application.page.styles.form.field.name.exists",  style.name()));
							}
						}
					}
					
					if (form.field ("definition").value () == null) {
						form.reject ("definition", "web.application.page.styles.form.field.definition.validation.tooLarge");
					} else {
						String xmlError = isValidXml(form.field("definition").value());
						if (xmlError != null){ 
							form.reject("definition", Domain.message("web.application.page.styles.form.field.definition.validation.error", form.field("format").value()));
							form.reject ("definition", xmlError);
						}
					}
					
					if (form.hasErrors ()) {
						return renderCreateForm (form);
					}
					// validation end
					
					final StyleForm styleForm = form.get ();
					final Style style = new Style(styleForm.id, styleForm.name.trim (), styleForm.definition, styleForm.styleType, styleForm.inUse);
					
					return from (database)
						.put(style)
						.executeFlat (new Function<Response<?>, Promise<Result>> () {
							@Override
							public Promise<Result> apply (final Response<?> response) throws Throwable {
								if (CrudOperation.CREATE.equals (response.getOperation())) {
									Logger.debug ("Created style " + style);
									flash ("success", Domain.message("web.application.page.styles.name") + " " + styleForm.getName () + " " + Domain.message("web.application.added").toLowerCase());
								}else{
									Logger.debug ("Updated style " + style);
									flash ("success", Domain.message("web.application.page.styles.name") + " " + styleForm.getName () + " " + Domain.message("web.application.updated").toLowerCase());
								}
								return Promise.pure (redirect (routes.Styles.list (null, 1)));
							}
					});
				}
			});
	}
	

	private static String isValidXml(String xmlContent) {
		try {
			final XMLStreamReader reader = XMLInputFactory.newInstance ().createXMLStreamReader (new StringReader (xmlContent));
			final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
			final Schema schema = schemaFactory.newSchema (new StreamSource (Play.application().resourceAsStream ("StyledLayerDescriptor.xsd")));
			final Validator validator = schema.newValidator ();
			
			validator.validate (new StAXSource (reader));
		} catch (IOException e) {
			return e.getLocalizedMessage ();
		} catch (SAXParseException e) {
			return e.getLineNumber () + ":" + e.getColumnNumber() + ": " + e.getMessage (); 
		} catch (SAXException e) {
			return e.getLocalizedMessage ();
		} catch (XMLStreamException e) {
			if (e.getLocation () != null) {
				return e.getLocation ().getLineNumber () + ":" + e.getLocation ().getColumnNumber () + ": " + e.getLocalizedMessage ();
			} else {
				return e.getLocalizedMessage ();
			}
		}
		
		return null;
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
	
	public static Promise<Result> listStylesJson (final long page, final String query) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from (database)
			.query (new ListStyles (page, query))
			.execute (new Function<Page<Style>, Result> () {
				@Override
				public Result apply (final Page<Style> styles) throws Throwable {
					final ObjectNode result = Json.newObject ();
					
					result.put ("header", stylePagerHeader.render (styles, query).toString ());
					result.put ("body", stylePagerBody.render (styles).toString ());
					result.put ("footer", stylePagerFooter.render (styles).toString ());
					
					return ok (result);
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
		
		return from(database).delete(Style.class, styleId)
		.execute(new Function<Response<?>, Result>() {
			
			@Override
			public Result apply(Response<?> a) throws Throwable {
				return redirect (routes.Styles.list (null, 1));
			}
		});
	}

	public static Result uploadFileForm () {
		return ok (uploadFileForm.render (null));
	}
	
	public static Result handleFileUploadForm () {
		final MultipartFormData body = request ().body ().asMultipartFormData ();
		final FilePart uploadFile = body.getFile ("file");
		
		if (uploadFile == null) {
			return ok (uploadFileForm.render (null));
		}
		
		final String content = handleFileUpload (uploadFile.getFile ());
		
		return ok (uploadFileForm.render (content));
	}
		
	@BodyParser.Of (value = BodyParser.Raw.class)
	public static Result handleFileUploadRaw () {
		final String content = request ().body ().asRaw () != null 
				? handleFileUpload (request ().body ().asRaw ().asFile ()) 
				: null;
				
		final ObjectNode result = Json.newObject ();
		if (content == null) {
			result.put ("valid", false);
		} else {
			result.put ("valid", true);
			result.put ("textContent", content);
		}
		
		return ok (result);
	}
	
	private static String handleFileUpload (final File file) {
		if (file == null) {
			return null;
		}
		
		try (final Reader reader = new InputStreamReader (new FileInputStream (file), Charset.forName ("UTF-8"))) {
			final char[] buffer = new char[512];
			final StringBuilder builder = new StringBuilder ();
			int n;
			int controlCount = 0;
			
			while ((n = reader.read (buffer)) >= 0) {
				// Quick and dirty method for detecting whether this is a plain text file: 
				// if the file contains any control characters (character code < 8) it will be classified as binary. 
				for (int i = 0; i < n; ++ i) {
					if (buffer[i] >= 0 && buffer[i] < 8) {
						++ controlCount;
					}
				}
				if (n > 0) {
					builder.append (Arrays.copyOf (buffer, n));
				}
			}

			return controlCount == 0 ? builder.toString () : null;
		} catch (IOException e) {
			return null;
		}
	}
	
	public static class StyleForm {
		@Constraints.Required
		private String id;
		@Constraints.Required (message = "web.application.page.styles.form.field.name.validation.required")
		@Constraints.MinLength (value = 3, message = "web.application.page.styles.form.field.name.validation.length")
		@Constraints.Pattern (value = "^[a-zA-Z0-9\\-\\_]+$", message = "web.application.page.styles.form.field.name.validation.error")
		private String name;
		@Constraints.Required (message = "web.application.page.styles.form.field.definition.validation.required")
		private String definition;
		private String styleType;
		private Boolean inUse;
		
		
		public StyleForm (){
			super();
			this.id = ID;
		}
		
		public StyleForm (final Style style){
			this.id = style.id();
			this.name = style.name();
			this.definition = style.definition();
			this.styleType = style.styleType().name();
			this.inUse = style.inUse();
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
		public Boolean getInUse() {
			return inUse;
		}
		public void setInUse(Boolean inUse) {
			this.inUse = inUse;
		}
		
	}

	
}
