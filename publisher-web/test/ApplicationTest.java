import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static play.data.Form.form;

import java.util.Collections;
import java.util.Map;

import models.Domain;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import play.data.Form;
import play.mvc.Http;
import play.test.FakeApplication;
import play.test.Helpers;
import play.twirl.api.Content;
import controllers.User.Login;


/**
*
* Simple (JUnit) tests that can call all parts of a play app.
* If you are interested in mocking a whole application, see the wiki for more details.
*
*/
public class ApplicationTest {
    
	public static FakeApplication app;
    private final Http.Request request = mock(Http.Request.class);

    @BeforeClass
    public static void startApp() {
    	
    	System.out.println("=========================================================");
    	System.out.println("                -------------------------");
    	System.out.println("                    APPLICATION TEST");
    	System.out.println("                -------------------------");
    	
        app = Helpers.fakeApplication();
        Helpers.start(app);
    }

    
    @AfterClass
    public static void endApp() {
    	System.out.println("=========================================================");
    	Helpers.stop(app);
    }

    
    /**
     * Make a Html context
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        Map<String, String> flashData = Collections.emptyMap();
        Map<String, Object> argData = Collections.emptyMap();
        Long id = 2L;
        play.api.mvc.RequestHeader header = mock(play.api.mvc.RequestHeader.class);
        Http.Context context = new Http.Context(id, header, request , flashData, flashData, argData);
        Http.Context.current.set(context);
    }


    @Test
    public void renderLogin() {
    	final Form<Login> loginForm = form (Login.class).fill (new Login (null));
        Content html = views.html.login.render(loginForm);
        assertThat(Helpers.contentType(html)).isEqualTo("text/html");
        assertThat(Helpers.contentAsString(html)).contains(Domain.message("web.application.title"));   
        assertThat(Helpers.contentAsString(html)).contains(Domain.message("web.application.login"));

    }

}
