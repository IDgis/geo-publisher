package controllers;

import play.data.Form;
import play.data.validation.Constraints;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.login;

import static play.data.Form.form;

public class User extends Controller {

	public static Result login (final String r) {
		final Form<Login> loginForm = form (Login.class).fill (new Login (r));
		
		return ok (login.render (loginForm));
	}
	
	public static Result authenticate () {
		final Form<Login> loginForm = form (Login.class).bindFromRequest ();
		
		if (loginForm.hasErrors ()) {
			return badRequest (login.render (loginForm));
		} else {
			session ().clear ();
			session ("username", loginForm.get ().username);
			
			if (loginForm.get ().getReturnUrl () != null) {
				return redirect (loginForm.get ().getReturnUrl ());
			} else {
				return redirect (routes.Dashboard.index ());
			}
		}
	}
	
	public static Result logout () {
		session ().clear ();
		return redirect (routes.Dashboard.index ());
	}
	
	public static class Login {
		@Constraints.Required
		private String username;
		
		private String password;
		
		private String returnUrl;
		
		public Login () {
		}
		
		public Login (final String returnUrl) {
			this.returnUrl = returnUrl;
		}

		public String getUsername () {
			return username;
		}

		public void setUsername (final String username) {
			this.username = username;
		}

		public String getPassword () {
			return password;
		}

		public void setPassword (final String password) {
			this.password = password;
		}

		public String getReturnUrl () {
			return returnUrl;
		}

		public void setReturnUrl (final String returnUrl) {
			this.returnUrl = returnUrl;
		}
		
		public String validate () {
			if (!"admin@idgis.nl".equals (username) || !"12admin34".equals (password)) {
				return "Ongeldige gebruikersnaam of wachtwoord";
			}
			
			return null;
		}
	}
}
