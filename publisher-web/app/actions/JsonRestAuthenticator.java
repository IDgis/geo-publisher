package actions;

import play.libs.Json;
import play.mvc.Http.Context;
import play.mvc.Result;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonRestAuthenticator extends DefaultAuthenticator {

	@Override
    public Result onUnauthorized (final Context ctx) {
		final ObjectNode result = Json.newObject ();
		
		result.put ("success", false);
		result.put ("status", "UNAUTHORIZED");
		
		return unauthorized (result);
	}
}
