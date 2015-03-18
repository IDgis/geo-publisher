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
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.Constant;
import nl.idgis.publisher.domain.query.PutCategories;
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
	
	private static Promise<Result> renderCreateForm (final Form<CategoryForm> categoriesForm) {
		// No need to go to the database, because the form contains all information needed
		 return Promise.promise(new F.Function0<Result>() {
             @Override
             public Result apply() throws Throwable {
                 return ok (form.render (categoriesForm.get().getCategories()));
             }
         });
	}
	
	public static Promise<Result> submitUpdate () {
		Logger.debug ("submit update Categories");
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		final Form<CategoryForm> form = Form.form (CategoryForm.class).bindFromRequest ();
		
		if (form.hasErrors ()) {
			return renderCreateForm (form);
		}

		Logger.debug("id  : "+ form.get().getIdentification());
		Logger.debug("name: "+ form.get().getName());
		
		final CategoryForm constantsForm = form.get();
		List<String> id   =  form.get().getIdentification();
		List<String> name =  form.get().getName();
		List<Category> categories = new ArrayList<Category>();
		for (int i = 0; i < id.size(); i++){
			categories.add(new Category(id.get(i), name.get(i)));
		}
		final PutCategories putCategories = new PutCategories(categories);
			
		return from (database)
			.query(putCategories)
			.executeFlat (new Function<Response<?>, Promise<Result>> () {
				@Override
				public Promise<Result> apply (final Response<?> response) throws Throwable {
					if (CrudResponse.OK.equals (response.getOperationResponse())) {
						Logger.debug ("Updated categories OK");
						flash ("success", Domain.message("web.application.updating") + " " +  Domain.message("web.application.layout.sidebar.categories") + " " +  Domain.message("web.application.succeeded"));
					}else{
						Logger.debug ("Updated categories NOK");
						flash ("danger", Domain.message("web.application.updating") + " " + Domain.message("web.application.layout.sidebar.categories") + " " +  Domain.message("web.application.failed"));
					}
					return Promise.pure (redirect (routes.Dashboard.index()));
				}
		});
	}
	
	public static Promise<Result> edit () {
		Logger.debug ("edit Categories");
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from (database)
			.list (Category.class)
			.execute (new Function<Page<Category>, Result> () {

				@Override
				public Result apply (final Page<Category> catList) throws Throwable {
					final CategoryForm catForm = new CategoryForm (catList);
					
					Logger.debug ("Edit categorie Form: " + catForm);

					return ok (form.render (catForm.getCategories()));
				}
			});
	}
	
	
	public static class CategoryForm {
		
		private List<String> identification;
		private List<String> name;
		private Page<Category> categories;
		
		public CategoryForm(){
			super();
		}
		
		public CategoryForm(Page<Category> categoryList){
			this.categories = categoryList;		
		}

		public Page<Category> getCategories() {
			return categories;
		}

		public void setCategories(Page<Category> categories) {
			this.categories = categories;
		}

		public List<String> getIdentification() {
			return identification;
		}

		public void setIdentification(List<String> identification) {
			this.identification = identification;
		}

		public List<String> getName() {
			return name;
		}

		public void setName(List<String> name) {
			this.name = name;
		}

	}
}