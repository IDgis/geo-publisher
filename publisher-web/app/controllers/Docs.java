package controllers;

import static org.pegdown.FastEncoder.encode;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

import org.parboiled.common.StringUtils;
import org.pegdown.LinkRenderer;
import org.pegdown.PegDownProcessor;
import org.pegdown.ast.AutoLinkNode;
import org.pegdown.ast.ExpImageNode;
import org.pegdown.ast.ExpLinkNode;
import org.pegdown.ast.RefImageNode;
import org.pegdown.ast.RefLinkNode;
import org.pegdown.ast.WikiLinkNode;

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
		
		final String fullPath = path + "/" + lang.language () + "/" + file;
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
		
		return ok (processor.markdownToHtml (buffer.toString (), new DefaultLinkRenderer (lang, file))).as ("text/html");
	}

	public static Action<AnyContent> at (final String path, final String file) {
		return controllers.Assets.at (path, getLang ().language () + "/" + file, false);
	}
	
	/**
	 * Link renderer that turns all links into absolute links.
	 */
	private static class DefaultLinkRenderer extends LinkRenderer {
		private final Lang lang;
		private final String currentPath;
		
		public DefaultLinkRenderer (final Lang lang, final String currentPath) {
			this.lang = lang;
			this.currentPath = currentPath;
		}
		
		private final String route (final String href) {
			if (href.endsWith (".md")) {
				return routes.Docs.markdown (href).url ();
			} else {
				return routes.Docs.at (href).url ();
			}
		}
		
		private String modifyHref (final String href) {
			// Don't modify hrefs that specify a protocol:
			if (href.indexOf ("://") >= 0) {
				return href;
			}
			
			// Absolute URL's are routed through the markdown controller:
			if (href.startsWith ("/")) {
				return route (href.substring (1));
			}
			
			// Relative URL's use the current path:
			if (currentPath.endsWith ("/")) {
				return route (currentPath + href);
			}
			
			int offset = currentPath.lastIndexOf ('/');
			if (offset >= 0) {
				return route (currentPath.substring (0, offset + 1) + href);
			}
			
			return route (href);
		}
		
		private Rendering modifyRendering (final Rendering rendering) {
			final Rendering newRendering = new Rendering (modifyHref (rendering.href), rendering.text);
			
			for (final Attribute attribute: rendering.attributes) {
				newRendering.withAttribute (attribute);
			}
			
			newRendering.withAttribute ("data-doc-path", newRendering.href);
			
			return newRendering;
		}
		
		@Override
	    public Rendering render (final AutoLinkNode node) {
			return modifyRendering (super.render (node));
	    }

		@Override
	    public Rendering render (final ExpLinkNode node, final String text) {
			return modifyRendering (super.render (node, text));
	    }

		@Override
	    public Rendering render (final ExpImageNode node, final String text) {
			return modifyRendering (super.render (node, text));
	    }

		@Override
	    public Rendering render (final RefLinkNode node, final String url, final String title, final String text) {
			return modifyRendering (super.render (node, url, title, text));
	    }

		@Override
	    public Rendering render (final RefImageNode node, final String url, final String title, final String alt) {
			return modifyRendering (super.render (node, url, title, alt));
	    }

		@Override
	    public Rendering render (final WikiLinkNode node) {
			return modifyRendering (super.render (node));
	    }
	}
}