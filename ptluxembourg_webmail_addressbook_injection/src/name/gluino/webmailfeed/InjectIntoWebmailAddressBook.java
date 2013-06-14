package name.gluino.webmailfeed;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Set;
import java.util.concurrent.TimeUnit;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mplify.logging.LogFacilities;
import com.mplify.logstarter.LogbackStarter;
import com.mplify.utils.Sleep;

/* 34567890123456789012345678901234567890123456789012345678901234567890123456789
 * *****************************************************************************
 * Copyright (c) 2013, David Tonhofer
 *
 * Distributed under: "The MIT License" (http://opensource.org/licenses/MIT)
 *******************************************************************************
 * ** This is where it's at! **
 * 
 * Fill the address book of the P&T Luxembourg Webmail application
 * (https://webmail.pt.lu/) via its not-entirely-well-navigable web interface
 * with the data coming from a club membership list (a CSV document; TODO: 
 * extract the data directly from an Excel sheet)
 
 * This is set of methods configured as a JUnit TestCase so that one can
 * execute this via Eclipse JUnit runner or something.
 * 
 * 2013.04.XX - Created
 * 2013.06.14 - Cleanup
 ******************************************************************************/

public class InjectIntoWebmailAddressBook {

    private final static String CLASS = InjectIntoWebmailAddressBook.class.getName();
    private final static Logger LOGGER_loginToWebmail = LoggerFactory.getLogger(CLASS + ".loginToWebmail");
    private final static Logger LOGGER_gotoWebmailPage = LoggerFactory.getLogger(CLASS + ".gotoWebmailPage");
    private final static Logger LOGGER_createAddressbook = LoggerFactory.getLogger(CLASS + ".createAddressbook");
    private final static Logger LOGGER_logoutFromWebmail = LoggerFactory.getLogger(CLASS + ".logoutFromWebmail");

    private final static PropertiesReader config; // immutable configuration
    private final static Set<ClubMember> allMembers; // immutable set of all the club members

    /**
     * Static constructor hoovers up configuration data from "config.xml" and also configures 
     * some immutable maps.
     */

    static {
        //
        // Fire up logging through "SLF4J" over "Logback"
        // This looks like a zero-effect statement but it's actually an initialization
        //
        {
            Object foo = new LogbackStarter(InjectIntoWebmailAddressBook.class);
        }
        //
        // The resource "config.xml" in the package in which "Hook" resides is read and a directly
        // usable structure is returned.
        //
        config = new PropertiesReader(name.gluino.webmailfeed.data.Hook.class, "config.xml");
        //
        // Hoover up all the member information and put it into a Set<Member>
        //
        try {
            allMembers = (new MemberListSlurper(config.getFqInputResource(), config.getCommitteEmails())).members;
        } catch (Exception exe) {
            throw new IllegalStateException(exe);
        }
    }

    /**
     * Of of the "tests" (i.e. runs of website interaction) uses a new "WebDriver".
     * We have a Firefox installation with the Selenium plugin, so it will be the "FirefoxDriver()"
     */

    private WebDriver driver;

    /**
     * We also have a simple facility to store several problems in a string buffer
     */

    private boolean acceptNextAlert = true;
    private StringBuffer verificationErrors = new StringBuffer();

    /**
     * Set up before each test (this is called on a new instance of AddContact)
     */

    @Before
    public void setUp() {
        driver = new FirefoxDriver();
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    }

    /**
     * Cleaning up after a "test" function has been run
     */

    @After
    public void tearDown() {
        driver.quit();
        String verificationErrorString = verificationErrors.toString();
        if (!"".equals(verificationErrorString)) {
            fail(verificationErrorString);
        }
    }

    /**
     * Helper
     */

    private void gotoWebmailPage(String baseUrl) {
        Logger logger = LOGGER_gotoWebmailPage;
        logger.info("Now going to: " + baseUrl);
        driver.get(baseUrl);
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
        // check that the page is up
        String text = driver.findElement(By.cssSelector("BODY")).getText();
        logger.info("Text in BODY: '" + LogFacilities.mangleString(text) + "'");
        assertTrue(text.contains("Webmail Login"));
        assertTrue(text.contains("Entreprise des P&T"));
    }

    /**
     * Helper
     */

    private void loginToWebmail(String username, String password) {
        Logger logger = LOGGER_loginToWebmail;
        driver.findElement(By.name("user")).clear();
        driver.findElement(By.name("user")).sendKeys(username);
        driver.findElement(By.name("password")).clear();
        driver.findElement(By.name("password")).sendKeys(password);
        new Select(driver.findElement(By.name("lang"))).selectByVisibleText("English");
        driver.findElement(By.cssSelector("button[type=\"submit\"]")).click();
        // We should be logged in now
        String title = driver.getTitle();
        logger.info("Title of page: '" + LogFacilities.mangleString(title) + "'");
        assertTrue(title.matches(".*?P&T\u00A0Luxembourg\u00A0Webmail.*?"));
        String text = driver.findElement(By.cssSelector("BODY")).getText();
        logger.info("Text in BODY: '" + LogFacilities.mangleString(text) + "'");
        assertTrue(text.contains("Logged in as: " + username + " | Logout |"));
    }

    /**
     * Helper
     */

    private void logoutFromWebmail() {
        Logger logger = LOGGER_logoutFromWebmail;
        driver.findElement(By.linkText("Logout")).click();
        Sleep.sleepTwoSeconds();
        String text = driver.findElement(By.cssSelector("BODY")).getText();
        logger.info("Text in BODY: '" + LogFacilities.mangleString(text) + "'");
        assertTrue(text.contains("Webmail Login"));
        assertTrue(text.contains("Entreprise des P&T"));
    }

    /**
     * Helper
     */

    private void createAddressbook(String foldername, Acceptor acceptor) {
        Logger logger = LOGGER_createAddressbook;
        gotoWebmailPage(config.getBaseURL());
        loginToWebmail(config.getUsername(), config.getPassword());
        driver.findElement(By.cssSelector("span.icon_manager.navigation_addr")).click();
        Sleep.sleepTwoSeconds();
        driver.findElement(By.id("icon_action_add_folder")).click();
        Sleep.sleepTwoSeconds();
        driver.findElement(By.id("name")).clear();
        driver.findElement(By.id("name")).sendKeys(foldername);
        new Select(driver.findElement(By.id("parent"))).selectByVisibleText(config.getUserEmail());
        driver.findElement(By.id("_button_save")).click();
        Sleep.sleepTwoSeconds();
        for (ClubMember m : allMembers) {
            if (acceptor.accept(m)) {
                int count = 0;
                for (String addr : m.emailAddressList) {
                    driver.findElement(By.id("icon_action_add")).click();
                    driver.findElement(By.id("firstname")).clear();
                    driver.findElement(By.id("firstname")).sendKeys(m.firstName);
                    driver.findElement(By.id("lastname")).clear();
                    if (count == 0) {
                        driver.findElement(By.id("lastname")).sendKeys(m.lastName);
                    } else {
                        driver.findElement(By.id("lastname")).sendKeys(m.lastName + " (" + count + ")");
                    }
                    count++;
                    driver.findElement(By.id("email")).clear();
                    driver.findElement(By.id("email")).sendKeys(addr);
                    driver.findElement(By.id("company")).clear();
                    driver.findElement(By.id("company")).sendKeys(acceptor.getCompany(m));
                    driver.findElement(By.id("_button_save")).click();
                }
            }
        }
        logoutFromWebmail();
    }

    /**
     * Helper
     */

    private boolean isElementPresent(By by) {
        try {
            driver.findElement(by);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Helper
     */

    private String closeAlertAndGetItsText() {
        try {
            Alert alert = driver.switchTo().alert();
            if (acceptNextAlert) {
                alert.accept();
            } else {
                alert.dismiss();
            }
            return alert.getText();
        } finally {
            acceptNextAlert = true;
        }
    }

    /**
     * Proper action: Adding all committee member. The "Acceptor" returns true if a
     * given "ClubMember" is detected as being a "committee member"
     */

    @Test
    public void addSubsetCommittee() {
        Acceptor acceptor = new Acceptor() {
            public boolean accept(ClubMember x) {
                assert x != null;
                return x.isCommittee();
            }

            public String getCompany(ClubMember x) {
                assert x != null;
                return "Comité ACL";
            }
        };
        createAddressbook("*** Subset: Comité ***", acceptor);
    }

    /**
     * Proper action: Adding all the black belters. The "Acceptor" returns true if a
     * given "ClubMember" is detected as being a "black belter"
     */

    @Test
    public void addSubsetCeinturesNoires() {
        Acceptor acceptor = new Acceptor() {
            public boolean accept(ClubMember x) {
                assert x != null;
                return x.isCeintureNoire();
            }

            public String getCompany(ClubMember x) {
                assert x != null;               
                return "Ceinture Noire: " + ((x.level == null) ? "?" : x.level);
            }
        };
        createAddressbook("*** Subset: Ceintures Noires ***", acceptor);
    }

    /**
     * Proper action: Adding all the children. The "Acceptor" returns true if a
     * given "ClubMember" is detected as being a "child"
     */

    @Test
    public void addSubsetEnfants() {
        Acceptor acceptor = new Acceptor() {
            public boolean accept(ClubMember x) {
                assert x != null;
                return x.isEnfant();
            }

            public String getCompany(ClubMember x) {
                assert x != null;
                return "Enfant âgé de: " + x.getAge() + " ans";
            }
        };
        createAddressbook("*** Subset: Enfants ***", acceptor);
    }

    /**
     * Proper action: Adding all the adults. The "Acceptor" returns true if a
     * given "ClubMember" is detected as being an "adult"
     */

    @Test
    public void addSubsetAdultes() {
        Acceptor acceptor = new Acceptor() {
            public boolean accept(ClubMember x) {
                assert x != null;
                return x.isAdulte();
            }

            public String getCompany(ClubMember x) {
                assert x != null;
                return "Adulte âgé de: " + x.getAge() + " ans";
            }
        };
        createAddressbook("*** Subset: Adultes ***", acceptor);
    }

    /**
     * Proper action: Adding everybody
     */

    @Test
    public void addSubsetTousLesMembres() {
        Acceptor acceptor = new Acceptor() {
            public boolean accept(ClubMember x) {
                assert x != null;
                return true;
            }

            public String getCompany(ClubMember x) {
                assert x != null;
                return "Membre ACL";
            }
        };
        createAddressbook("*** Tous les Membres du Club ***", acceptor);
    }

}
