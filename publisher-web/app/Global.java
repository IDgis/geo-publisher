import java.text.ParseException;
import java.util.Locale;

import nl.idgis.publisher.domain.web.Filter;
import play.Application;
import play.GlobalSettings;
import play.data.format.Formatters;
import play.libs.Json;


public class Global extends GlobalSettings {

	@Override
	public void onStart (final Application application) {
		Formatters.register (Filter.class, new Formatters.SimpleFormatter<Filter> () {
			@Override
			public Filter parse (final String text, final Locale locale) throws ParseException {
				try {
					return Json.fromJson (Json.parse (text), Filter.class);
				} catch (Exception e) {
					throw new ParseException (e.getMessage (), 0);
				}
			}

			@Override
			public String print (final Filter filter, final Locale locale) {
				return Json.stringify (Json.toJson (filter));
			}
		});
	}
}
