package controllers;

import static models.Domain.from;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

import models.Domain;
import models.Domain.Function;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.Constant;
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
import views.html.categories.form;
import actions.DefaultAuthenticator;
import akka.actor.ActorSelection;

@Security.Authenticated (DefaultAuthenticator.class)
public class Categories extends Controller {
	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");
	private final static String ID="#CREATE_CONSTANTS#";
	
	
	private static Promise<Result> renderCreateForm (final Form<CategoryForm> constantsForm) {
		// No need to go to the database, because the form contains all information needed
		 return Promise.promise(new F.Function0<Result>() {
             @Override
             public Result apply() throws Throwable {
                 return ok (form.render (constantsForm.get().getCategories()));
             }
         });
	}
	
	public static Promise<Result> submitUpdate () {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		final Form<CategoryForm> form = Form.form (CategoryForm.class).bindFromRequest ();
//		
//		if (form.hasErrors ()) {
//			return renderCreateForm (form);
//		}
//		
//		final CategoryForm constantsForm = form.get();
//		final PutCategories putCategories;
//		
//		Logger.debug ("submit Constants: " + form.field("organization").value());
//		
//		
//		return from (database)
//			.query(putCategories)
//			.executeFlat (new Function<Response<?>, Promise<Result>> () {
//				@Override
//				public Promise<Result> apply (final Response<?> response) throws Throwable {
//					if (CrudOperation.CREATE.equals (response.getOperation())) {
//						Logger.debug ("Created constants");
//						flash ("success", Domain.message("web.application.page.constants.name") + " " + constantsForm.getOrganization() + " is " + Domain.message("web.application.added").toLowerCase());
//					}else{
//						Logger.debug ("Updated constants");
//						flash ("success", Domain.message("web.application.page.constants.name") + " " + constantsForm.getOrganization() + " is " + Domain.message("web.application.updated").toLowerCase());
//					}
//					return Promise.pure (redirect (routes.Dashboard.index()));
//				}
//		});
		return null;
	}
	
	public static Promise<Result> edit () {
		Logger.debug ("edit Categories");
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from (database)
			.list (Category.class)
			.execute (new Function<Page<Category>, Result> () {

				@Override
				public Result apply (final Page<Category> cat) throws Throwable {
					final CategoryForm catForm = new CategoryForm (cat);
					
					Logger.debug ("Edit categorie Form: " + catForm);						

					return ok (form.render (catForm.getCategories()));
				}
			});
	}
	
	
	public static class FormCategory {
		private String row;
		private String identification;
		private String name;
		
		public FormCategory(String row, String identification, String name) {
			super();
			this.row = row;
			this.identification = identification;
			this.name = name;
		}		

		public String getRow() {
			return row;
		}
		public void setRow(String row) {
			this.row = row;
		}
		public String getIdentification() {
			return identification;
		}
		public void setIdentification(String identification) {
			this.identification = identification;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
	}
	
	public static class CategoryForm {
		
		@Constraints.Required
		private List<FormCategory> categories;
		
		public CategoryForm(){
			super();
		}
		
		public CategoryForm(Page<Category> categoryList){
			if(!categoryList.values().isEmpty()) {
				categories = new ArrayList<FormCategory>();
				int i = 0;
				for (Category cat : categoryList.values()){
					
					categories.add( new FormCategory("" + i, cat.id(), cat.name() ));
					i++;
					
				}
			} else {
			}
		}

		public List<FormCategory> getCategories() {
			return categories;
		}

		public void setCategories(List<FormCategory> categories) {
			this.categories = categories;
		}

	}
}