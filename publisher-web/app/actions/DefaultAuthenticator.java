package actions;

import controllers.routes;
import play.mvc.Result;
import play.mvc.Http.Context;
import play.mvc.Security.Authenticator;

public class DefaultAuthenticator extends Authenticator {

	@Override
    public String getUsername (final Context ctx) {
        final String username = ctx.session ().get ("username");
        
        if (username != null && isAllowed (username)) {
        	return username;
        }
        
        return null;
    }
    
    protected boolean isAllowed (final String username) {
    	return true;
    }
    
    @Override
    public Result onUnauthorized (final Context ctx) {
    	if (ctx.session ().get ("username") == null) {
    		return redirect (routes.User.login (ctx.request ().uri ()));
    	} else {
    		return unauthorized (views.html.defaultpages.unauthorized.render ()); 
    	}
    }

}
