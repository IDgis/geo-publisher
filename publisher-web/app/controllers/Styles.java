package controllers;

import static models.Domain.from;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
import models.Domain.Function2;
import nl.idgis.publisher.domain.query.GetStyleParentLayers;
import nl.idgis.publisher.domain.query.ListStyles;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.web.Layer;
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
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import play.mvc.Security;
import views.html.styles.form;
import views.html.styles.list;
import views.html.styles.stylePagerBody;
import views.html.styles.stylePagerFooter;
import views.html.styles.stylePagerHeader;
import views.html.styles.uploadFileForm;
import actions.DefaultAuthenticator;
import akka.actor.ActorSelection;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Security.Authenticated (DefaultAuthenticator.class)
public class Styles extends Controller {
	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");
	private final static String ID="#CREATE_STYLE#";
	
	private final static Promise<Schema> schemaPromise = Promise.promise (() -> {
		final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
		return schemaFactory.newSchema (new StreamSource (Play.application().resourceAsStream ("StyledLayerDescriptor.xsd")));
	});
	
	private static Promise<Result> renderCreateForm (final Form<StyleForm> styleForm) {
		// No need to go to the database, because the form contains all information needed
		 return Promise.promise(new F.Function0<Result>() {
             @Override
             public Result apply() throws Throwable {
                 return ok (form.render (styleForm, null, true, Optional.empty (), Optional.empty ()));
             }
         });
	}
	
	private static String join (final List<String> strings) {
		final StringBuilder builder = new StringBuilder ();
		
		for (final String s: strings) {
			builder.append (s);
		}
		
		return builder.toString ();
	}
	
	@BodyParser.Of (value = BodyParser.FormUrlEncoded.class, maxLength = 2 * 1024 * 1024)
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
					
					if (form.hasErrors ()) {
						return Promise.pure ((Result) ok (views.html.styles.form.render (form, null, form.field ("id").equals (ID), Optional.empty (), Optional.empty ())));
					}

					final StyleForm styleForm = form.get ();
					final String sldContent = join (styleForm.getDefinition ());
					final Optional<Integer> errorLine;
					final Optional<String> errorMessage;
					
					final XmlError xmlError = isValidXml (sldContent);
					if (xmlError != null){ 
						form.reject("definition", Domain.message("web.application.page.styles.form.field.definition.validation.error", form.field("format").value()));
						form.reject ("definition", xmlError.message);
						errorLine = xmlError.line == null ? Optional.empty () : Optional.of (xmlError.line);
						errorMessage = Optional.of (xmlError.message);
					} else {
						errorLine = Optional.empty ();
						errorMessage = Optional.empty ();
					}
					
					if (form.hasErrors ()) {
						return Promise.pure ((Result) ok (views.html.styles.form.render (form, null, form.field ("id").equals (ID), errorLine, errorMessage)));
					}
					// validation end
					
					final Style style = new Style(styleForm.id, styleForm.name.trim (), sldContent, styleForm.styleType, styleForm.inUse);
					
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
	

	private static XmlError isValidXml(String xmlContent) {
		try {
			final XMLStreamReader reader = XMLInputFactory.newInstance ().createXMLStreamReader (new StringReader (xmlContent));
			final Validator validator = schemaPromise.get (30000).newValidator ();
			
			validator.validate (new StAXSource (reader));
		} catch (IOException e) {
			return new XmlError (e.getLocalizedMessage (), null);
		} catch (SAXParseException e) {
			return new XmlError (e.getLineNumber () + ":" + e.getColumnNumber() + ": " + e.getMessage (), e.getLineNumber ());
		} catch (SAXException e) {
			return new XmlError (e.getLocalizedMessage (), null);
		} catch (XMLStreamException e) {
			if (e.getLocation () != null) {
				return new XmlError (e.getLocation ().getLineNumber () + ":" + e.getLocation ().getColumnNumber () + ": " + e.getLocalizedMessage (), e.getLocation ().getLineNumber ());
			} else {
				return new XmlError (e.getLocalizedMessage (), null);
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
			.query(new GetStyleParentLayers(styleId))
			.execute (new Function2<Style, Page<Layer>, Result> () {

				@Override
				public Result apply (final Style style, final Page<Layer> layers) throws Throwable {
					final Form<StyleForm> styleForm = Form
							.form (StyleForm.class)
							.fill (new StyleForm (style));
					
					Logger.debug ("Edit styleForm: " + styleForm);						
					Logger.debug ("Style is in layers: " + layers.values().toString());						
					return ok (form.render (styleForm, layers, false, Optional.empty (), Optional.empty ()));
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
	
	@BodyParser.Of (value = BodyParser.MultipartFormData.class, maxLength = 2 * 1024 * 1024)
	public static Result handleFileUploadForm () {
		final MultipartFormData body = request ().body ().asMultipartFormData ();
		final FilePart uploadFile = body.getFile ("file");
		
		if (uploadFile == null) {
			return ok (uploadFileForm.render (null));
		}
		
		final String content = handleFileUpload (uploadFile.getFile ());
		
		return ok (uploadFileForm.render (content));
	}
		
	@BodyParser.Of (value = BodyParser.Raw.class, maxLength = 2 * 1024 * 1024)
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
	
	@BodyParser.Of (value = BodyParser.Raw.class, maxLength = 2 * 1024 * 1024)
	public static Result validateSld () {
		final String content = request ().body ().asRaw () != null
				? handleFileUpload (request ().body ().asRaw ().asFile ())
				: null;
				
		final ObjectNode result = Json.newObject ();
		if (content == null) {
			result.put ("valid", false);
			result.put ("message", Domain.message ("web.application.page.styles.form.field.definition.validation.cantValidate"));
		} else {
			final XmlError xmlError = isValidXml (content);
			
			if (xmlError != null) {
				result.put ("valid", false);
				result.put ("message", xmlError.message);
				if (xmlError.line != null) {
					result.put ("line", (int) xmlError.line);
				}
			} else {
				result.put ("valid", true);
				result.put ("message", Domain.message ("web.application.page.styles.form.field.definition.validation.valid"));
			}
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
		private List<String> definition = new ArrayList<> ();
		private String styleType;
		private Boolean inUse;
		
		
		public StyleForm (){
			super();
			this.id = ID;
		}
		
		public StyleForm (final Style style){
			this.id = style.id();
			this.name = style.name();
			this.definition = new ArrayList<> ();
			this.definition.add (style.definition ());
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
		public List<String> getDefinition() {
			return definition;
		}
		public void setDefinition(List<String> definition) {
			this.definition = definition == null ? new ArrayList<> () : new ArrayList<> (definition);
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

	public static class XmlError {
		public final String message;
		public final Integer line;

		public XmlError (final String message, final Integer line) {
			this.message = message;
			this.line = line;
		}
	}
	
}
