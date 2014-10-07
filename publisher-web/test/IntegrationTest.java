import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.inMemoryDatabase;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;
import models.Domain;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import play.libs.F.Callback;
import play.test.TestBrowser;

import com.typesafe.config.ConfigFactory;

public class IntegrationTest {

	private static String configUsername = ConfigFactory.load().getString("publisher.admin.username");
	private static String configPassword = ConfigFactory.load().getString("publisher.admin.password");

	@BeforeClass
    public static void before() {
    	
    	System.out.println("=========================================================");
    	System.out.println("                -------------------------");
    	System.out.println("                    INTEGRATION TEST");
    	System.out.println("                -------------------------");
    	
    }

    @AfterClass
    public static void after() {
    	System.out.println("=========================================================");
    }


    /**
     * add your integration test here
     * in this example we just check if the welcome page is being shown
     */
    
    
    @Test
    public void testLogin() {
        running(testServer(3333, fakeApplication(inMemoryDatabase())), HTMLUNIT, new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) {
                browser.goTo("http://localhost:3333");
                assertThat(browser.url()).startsWith("http://localhost:3333/user/login");
                assertThat(browser.pageSource()).contains(Domain.message("web.application.title"));
                assertThat(browser.pageSource()).contains(Domain.message("web.application.login"));
            }
        });
    }

    @Test
    public void testPreformLoginSucceed() {

        running(testServer(3333, fakeApplication(inMemoryDatabase())), HTMLUNIT, new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) {
                browser.goTo("http://localhost:3333");
                assertThat(browser.url()).startsWith("http://localhost:3333/user/login");
                assertThat(browser.pageSource()).contains(Domain.message("web.application.login"));
                
                browser.find("input[name=username]").text(configUsername);
                browser.find("input[name=password]").text(configPassword);
                browser.find("button[type=submit]").click();
                /*
                 * The application is started 
                 */
                assertThat(browser.pageSource()).doesNotContain(Domain.message("web.application.login.FAILED"));
                // This will only work when there is a service  
//                assertThat(browser.pageSource()).contains(Domain.message("web.application.layout.sidebar.dashboard"));
            }
        });
    }

    @Test
    public void testPreformLoginFail() {
        running(testServer(3333, fakeApplication(inMemoryDatabase())), HTMLUNIT, new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) {
                browser.goTo("http://localhost:3333");
                assertThat(browser.url()).startsWith("http://localhost:3333/user/login");
                assertThat(browser.pageSource()).contains(Domain.message("web.application.login"));
                
                browser.find("input[name=username]").text(configUsername);
                browser.find("input[name=password]").text(configPassword + "XXX");
                browser.find("button[type=submit]").click();

                assertThat(browser.pageSource()).contains(Domain.message("web.application.login.FAILED"));
            }
        });
    }

    
    
    
}
