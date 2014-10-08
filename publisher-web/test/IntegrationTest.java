import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.inMemoryDatabase;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.util.concurrent.TimeUnit;

import models.Domain;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import play.libs.Akka;
import play.libs.F.Callback;
import play.test.TestBrowser;
import scala.concurrent.Await;
import scala.concurrent.Future;
import akka.actor.ActorIdentity;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Identify;
import akka.pattern.AskableActorSelection;
import akka.util.Timeout;

import com.typesafe.config.ConfigFactory;

/**
 * Contains user interface tests by starting the application and a browser. <br>
 * The browser can mimic user actions.
 * @author Rob
 *
 */
public class IntegrationTest {

	private static String configUsername = ConfigFactory.load().getString("publisher.admin.username");
	private static String configPassword = ConfigFactory.load().getString("publisher.admin.password");
	private final static String databaseRef = ConfigFactory.load().getString("publisher.database.actorRef");

	private ActorSelection database = null;
	
	private String baseUrl;
	private int basePort;

	private static WebDriver driver;
	
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
		driver.quit();
    }

    
	@Before
	public void setUp() throws Exception {
		basePort = 3333;
		baseUrl = "http://localhost:"+basePort;
		driver = new HtmlUnitDriver();
		/* 
		 * The firefox driver starts FireFox but does not replay the TestBrowser actions
		 */
//		driver = new FirefoxDriver();
	}

    @After
    public void teardown() {
		driver.quit();
    }

    /**
     * Check if the service actor is available.
     * @param sel actor
     * @return true if available
     */
    private boolean actorAvailable(ActorSelection sel){
    	ActorRef ref  = null;
        Timeout t = new Timeout(5, TimeUnit.SECONDS);
        AskableActorSelection asker = new AskableActorSelection(sel);
        Future<Object> fut = asker.ask(new Identify(1), t);
        ActorIdentity ident;
		try {
			ident = (ActorIdentity)Await.result(fut, t.duration());
			ref = ident.getRef();
		} catch (Exception e) {
			e.printStackTrace();
		}
        System.out.println(">>>>>>> actor avail: " + (ref != null));
		return ref != null;
    }
    
    /**
     * Perform a login by filling in username and password and clicking the login button.
     * @param browser
     * @param user
     * @param password
     */
    private void performLoginActions(TestBrowser browser, String user, String password) {
		browser.goTo(baseUrl);
        assertThat(browser.url()).startsWith(baseUrl + "/user/login");
        
        // 'user' fills in fields and clicks button:        
        browser.find("input[name=username]").text(user);
        browser.find("input[name=password]").text(password);
        browser.find("button[type=submit]").click();
	}

    @Test
    public void testLoginPage() {
        running(testServer(basePort, fakeApplication(inMemoryDatabase())), HTMLUNIT, new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) {
                browser.goTo(baseUrl);
                assertThat(browser.url()).startsWith(baseUrl + "/user/login");
                assertThat(browser.pageSource()).contains(Domain.message("web.application.title"));
                assertThat(browser.pageSource()).contains(Domain.message("web.application.login"));
            }
        });
    }

    @Test
    public void testPerformLoginSuccess() {

        running(testServer(basePort, fakeApplication(inMemoryDatabase())), driver, new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) {
            	
                performLoginActions(browser, configUsername, configPassword);
                
                /*
                 * The application is started 
                 */
                assertThat(browser.pageSource()).doesNotContain(Domain.message("web.application.login"));
                assertThat(browser.pageSource()).doesNotContain(Domain.message("web.application.login.failed"));
                
                //check if service available
                database = Akka.system().actorSelection (databaseRef);		
                if (!actorAvailable(database)){
	                // When there is no service, the program will not start  
	                assertThat(browser.pageSource()).contains(Domain.message("web.application.notavailable"));
                }else{
	                // When there is a service, the userinterface will show and can be tested further  
	                assertThat(browser.pageSource()).contains(Domain.message("web.application.layout.sidebar.dashboard"));
	                assertThat(browser.pageSource()).contains(Domain.message("web.application.layout.sidebar.datasets"));
	                assertThat(browser.pageSource()).contains(Domain.message("web.application.layout.sidebar.sourcedatasets"));
	                assertThat(browser.pageSource()).contains(Domain.message("web.application.layout.sidebar.log"));
                }
            }

        });
    }

    @Test
    public void testPerformLoginFail() {
        running(testServer(basePort, fakeApplication(inMemoryDatabase())), driver, new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) {
            	
            	performLoginActions(browser, configUsername, configPassword + "XXXX");

            	// check that the login failed message appears
                assertThat(browser.pageSource()).contains(Domain.message("web.application.login.failed"));
                
                // check that the application does not start
                assertThat(browser.pageSource()).doesNotContain(Domain.message("web.application.notavailable"));
                assertThat(browser.pageSource()).doesNotContain(Domain.message("web.application.layout.sidebar.dashboard"));
            }
        });
    }

	@Ignore ("This will fail because the webdriver does not update pagesource")
	@Test
	public void testPerformLoginFailIgnored() {
		running(testServer(basePort, fakeApplication(inMemoryDatabase())), driver, new Callback<TestBrowser>() {
			public void invoke(TestBrowser browser) {
				WebDriver driver = browser.getDriver();
				driver.get(baseUrl + "/user/login?r=%2F");
				driver.findElement(By.name("username")).clear();
				driver.findElement(By.name("username")).sendKeys(configUsername);
				driver.findElement(By.name("password")).clear();
				driver.findElement(By.name("password")).sendKeys(configPassword + "XXX");
				driver.findElement(By.xpath("//button[@type='submit']")).click();
				   
				/*
				 * This will fail because the page source contains the old login page,
				 * not the resulting page after clicking the button.
				 */
				assertThat(driver.getPageSource()).contains(Domain.message("web.application.login.failed"));
			}
		});
	}

    
    
    
}
