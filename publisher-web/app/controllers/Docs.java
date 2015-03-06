package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

import org.pegdown.PegDownProcessor;

import play.Logger;
import play.Play;
import play.api.i18n.Lang;
import play.api.mvc.Action;
import play.api.mvc.AnyContent;
import play.mvc.Controller;
import play.mvc.Result;

public class Docs extends Controller {

    private static Lang getLang(){
        Lang lang = null;
        if(play.mvc.Http.Context.current.get() != null) {
            lang = play.mvc.Http.Context.current().lang();
        } else {
            Locale defaultLocale = Locale.getDefault();
            lang = new Lang(defaultLocale.getLanguage(), defaultLocale.getCountry());
        }
        return lang;
    }
    
	public static Result markdown (final String path, final String file) {
		final Lang lang = getLang ();
		
		final String fullPath = path + "/" + lang.code () + "/" + file;
		final InputStream stream = Play.application().resourceAsStream (fullPath);
		if (stream == null) {
			Logger.debug ("Document not found: " + fullPath);
			return notFound ();
		}
		
		final StringBuffer buffer = new StringBuffer ();
		try (final InputStreamReader reader = new InputStreamReader (stream, Charset.forName ("UTF-8"))) {
			final char[] characters = new char[1024];
			int n;
			
			while ((n = reader.read (characters)) >= 0) {
				if (n == 0) {
					continue;
				} else if (n == characters.length) {
					buffer.append (characters);
				} else {
					buffer.append (Arrays.copyOf (characters, n));
				}
			}
		} catch (IOException e) {
			throw new RuntimeException (e);
		}
		
		final PegDownProcessor processor = new PegDownProcessor ();
		
		return ok (processor.markdownToHtml (buffer.toString ())).as ("text/html");
	}

	public static Action<AnyContent> at (final String path, final String file) {
		return controllers.Assets.at (path, file, false);
	}
}