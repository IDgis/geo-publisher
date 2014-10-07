package test.selenium.ide;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.inMemoryDatabase;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;
import models.Domain;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import play.Play;
import play.libs.F.Callback;
import play.test.TestBrowser;

import com.typesafe.config.ConfigFactory;

import org.openqa.selenium.*;
import static org.fluentlenium.core.filter.FilterConstructor.*;

public class TestLogin {

	private static String configUsername = ConfigFactory.load().getString("publisher.admin.username");
	private static String configPassword = ConfigFactory.load().getString("publisher.admin.password");
        
     private String baseUrl;

  @Before
  public void setUp() throws Exception {
    baseUrl = "http://localhost:3333";
  }
	@BeforeClass
    public static void before() {
    	
    	System.out.println("=========================================================");
    	System.out.println("                -------------------------");
    	System.out.println("                    SELENIUM TEST");
    	System.out.println("                -------------------------");
    	
    }

    @AfterClass
    public static void after() {
    	System.out.println("=========================================================");
    }


     
    @Test
  public void testLogin() throws Exception {
        running(testServer(3333, fakeApplication(inMemoryDatabase())), HTMLUNIT, new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) {
                WebDriver  driver=browser.getDriver();
              // selenium IDE generated code here:
    driver.get(baseUrl + "/user/login?r=%2F");
    driver.findElement(By.name("password")).clear();
    driver.findElement(By.name("password")).sendKeys("kdfsdlkfd");
    driver.findElement(By.xpath("//button[@type='submit']")).click();
    // ERROR: Caught exception [unknown command []]
            }
        });
    }

  @After
  public void tearDown() throws Exception {
    
  }

}
