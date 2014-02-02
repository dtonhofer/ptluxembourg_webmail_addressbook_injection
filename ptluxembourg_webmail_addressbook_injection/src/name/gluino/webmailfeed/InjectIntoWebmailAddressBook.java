package name.gluino.webmailfeed;

import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.example.BasicChecks.*;

import com.mplify.logging.LogFacilities;
import com.mplify.logstarter.LogbackStarter;
import com.mplify.tools.Sleep;

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
    private final static Logger LOGGER_goToWebmailLoginPage = LoggerFactory.getLogger(CLASS + ".goToWebmailLoginPage");
    private final static Logger LOGGER_createAddressbook = LoggerFactory.getLogger(CLASS + ".createAddressbook");
    private final static Logger LOGGER_logoutFromWebmail = LoggerFactory.getLogger(CLASS + ".logoutFromWebmail");
    private final static Logger LOGGER_openContactsMenu = LoggerFactory.getLogger(CLASS + ".openContactsMenu");
    private final static Logger LOGGER_makeSureFolderDoesNotExist = LoggerFactory.getLogger(CLASS + ".makeSureFolderDoesNotExist");
    private final static Logger LOGGER_addNewFolder = LoggerFactory.getLogger(CLASS + ".addNewFolder");
    private final static Logger LOGGER_makeSureFolderDoesExist = LoggerFactory.getLogger(CLASS + ".makeSureFolderDoesExist");
    private final static Logger LOGGER_enterFolder = LoggerFactory.getLogger(CLASS + ".enterFolder");
    private final static Logger LOGGER_enterRelevantContacts = LoggerFactory.getLogger(CLASS + ".enterRelevantContacts");

    private final PropertiesReader config; // immutable configuration
    private final Set<ClubMember> allMembers; // immutable set of all the club members
    private final WebDriver driver;

    private final int DOG_SLOW_SECONDS = 10;
    private final int MEDIUM_SLOW_SECONDS = 10;

    /**
     * We also have a simple facility to store several problems in a string buffer
     */

    private boolean acceptNextAlert = true;
    private StringBuffer verificationErrors = new StringBuffer();

    /**
     * Constructor
     */

    public InjectIntoWebmailAddressBook(PropertiesReader config, Set<ClubMember> allMembers) {
        checkNotNull(config, "config");
        checkNotNull(allMembers, "allMembers");
        this.config = config;
        this.allMembers = Collections.unmodifiableSet(allMembers);
        this.driver = new FirefoxDriver();
        this.driver.manage().timeouts().implicitlyWait(DOG_SLOW_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Cleaning up after a "test" function has been run
     */

    public void tearDown() {
        driver.quit();
        String verificationErrorString = verificationErrors.toString();
        if (!"".equals(verificationErrorString)) {
            instaFail(verificationErrorString);
        }
    }

    /**
     * Helper
     */

    private void goToWebmailLoginPage() {
        Logger logger = LOGGER_goToWebmailLoginPage;
        logger.info("Opening login page at " + config.getBaseURL());
        driver.manage().timeouts().implicitlyWait(MEDIUM_SLOW_SECONDS, TimeUnit.SECONDS);
        driver.get(config.getBaseURL() + "/webmail");
        //
        // On return, check that the login page is up
        //
        String text = driver.findElement(By.cssSelector("BODY")).getText();
        logger.info("Text in BODY: '" + LogFacilities.mangleString(text) + "'");
        checkTrue(text.contains("Webmail Login"), "Did not find 'Webmail Login'");
        checkTrue(text.contains("POST Luxembourg"), "Did not find 'POST Luxembourg'");
        logger.info("Apparently the login page is visible");
    }

    /**
     * Helper
     */

    private void loginToWebmail() {
        Logger logger = LOGGER_loginToWebmail;
        logger.info("Logging in");
        driver.findElement(By.name("user")).clear();
        driver.findElement(By.name("user")).sendKeys(config.getUsername());
        driver.findElement(By.name("password")).clear();
        driver.findElement(By.name("password")).sendKeys(config.getPassword());
        new Select(driver.findElement(By.name("lang"))).selectByVisibleText("English");
        driver.findElement(By.cssSelector("button[type=\"submit\"]")).click();
        Sleep.sleepTwoSeconds();
        //
        // On return, we should be logged in
        //
        driver.get(config.getBaseURL() + "/webmail/overview");
        String title = driver.getTitle();
        logger.info("Title of page: '" + LogFacilities.mangleString(title) + "'");
        // checkTrue(title.matches(".*?P&T\u00A0Luxembourg\u00A0Webmail.*?"), "Looked for P&T Luxembourg Webmail string");
        String text = driver.findElement(By.cssSelector("BODY")).getText();
        logger.info("Text in BODY: '" + LogFacilities.mangleString(text) + "'");
        checkTrue(text.contains(config.getUsername()), "Looked for '" + config.getUsername() + "'");
        checkTrue(text.contains("Logout"), "Looked for 'Logout'");
        //
        // The icon for "contacts" should be there
        //
        checkTrue(isElementPresent(By.cssSelector("span.icon_manager.navigation_addr")), "The 'Contacts' icon is there");
        logger.info("Apparently we are logged in");
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
    

    private void waitForExportImportInAddressBookMenu() {
      for (int second = 0;; second++) {
          checkFalse(second >= 60, "timeout");
          try { if (isElementPresent(By.id("text_action_export"))) break; } catch (Exception e) { 
              // NOP          
              
          }
          Sleep.sleepHalfASecond();
      }

      for (int second = 0;; second++) {
          checkFalse(second >= 60, "timeout");
          try { if (isElementPresent(By.id("text_action_import_addrimport"))) break; } catch (Exception e) {
              // NOP
          }
          Sleep.sleepHalfASecond();
      }

    }

    /**
     * Helper
     */

    private void openContactsMenu() {
        Logger logger = LOGGER_openContactsMenu;
        logger.info("Opening contacts menu");
        driver.findElement(By.cssSelector("span.icon_manager.navigation_addr")).click();
        Sleep.sleepTwoSeconds();
        //
        // On return, the web page should be in "address book mode".
        // Finding out that this is so is kind difficult.
        //
        checkTrue(driver.findElement(By.cssSelector("BODY")).getText().matches("^[\\s\\S]*Contacts[\\s\\S]*$"), "Looked for 'Contacts'");
        waitForExportImportInAddressBookMenu();
        checkTrue(isElementPresent(By.id("icon_action_add_folder")), "Add folder icon should be there");        
        //
        // Click at least twice then click once more if "Contacts" is not yet there
        //
        driver.findElement(By.xpath("(//a[contains(text(),'" + config.getUsername() + "')])[2]")).click();
        Sleep.sleepTwoSeconds();
        driver.findElement(By.xpath("(//a[contains(text(),'" + config.getUsername() + "')])[2]")).click();
        Sleep.sleepTwoSeconds();
        if (!isElementPresent(By.xpath("//a[contains(text(),'Contacts')]"))) {
            driver.findElement(By.xpath("(//a[contains(text(),'" + config.getUsername() + "')])[2]")).click();
            Sleep.sleepTwoSeconds();
        }
        //
        // Contacts must now be there
        //
        checkTrue(isElementPresent(By.xpath("//a[contains(text(),'Contacts')]")), "Was looking for 'Contacts'");
        logger.info("Apparently 'Contacts' are now visible");

    }

    /**
     * Helper
     */

    private void logoutFromWebmail() {
        Logger logger = LOGGER_logoutFromWebmail;
        logger.info("Logging out");
        driver.findElement(By.linkText("Logout")).click();
        Sleep.sleepTwoSeconds();
        String text = driver.findElement(By.cssSelector("BODY")).getText();
        logger.info("Text in BODY: '" + LogFacilities.mangleString(text) + "'");
        checkTrue(text.contains("Webmail Login"), "Did not find 'Webmail Login'");
        //checkTrue(text.contains("Entreprise des P&T"), "Did not find 'Entreprise des P&T'");
        logger.info("Apparently logged out");
    }

    /**
     * Helper
     */

    private void makeSureFolderDoesNotExist(String foldername) {
        Logger logger = LOGGER_makeSureFolderDoesNotExist;
        logger.info("Making sure there is no folder '" + foldername + "'");
        // for some reason this tkes a LONG time
        checkFalse(isElementPresent(By.xpath("//a[contains(text(),'" + foldername + "')]")), "Was looking for '" + foldername + "'");
        logger.info("Apparently there is no folder '" + foldername + "'");
    }

    /**
     * Helper
     */

    private void makeSureFolderDoesExist(String foldername) {
        Logger logger = LOGGER_makeSureFolderDoesExist;
        logger.info("Making sure there is a folder '" + foldername + "'");
        checkTrue(isElementPresent(By.xpath("//a[contains(text(),'" + foldername + "')]")), "Was looking for '" + foldername + "'");
        logger.info("Apparently there is a folder '" + foldername + "'");
    }

    /**
     * Helper
     */

    private void addNewFolder(String foldername) {
        Logger logger = LOGGER_addNewFolder;
        driver.findElement(By.id("icon_action_add_folder")).click();
        Sleep.sleepTwoSeconds();
        checkTrue(isElementPresent(By.id("_button_cancel")), "Looked for CANCEL button");
        checkTrue(isElementPresent(By.id("_button_save")), "Looked for SAVE button");
        checkTrue(isElementPresent(By.id("parent")), "Looked for 'parent folder' entry field");
        checkTrue(isElementPresent(By.id("name")), "Looked for 'name' entry field");
        driver.findElement(By.id("name")).clear();
        driver.findElement(By.id("name")).sendKeys(foldername);
        driver.findElement(By.id("_button_save")).click();
        Sleep.sleepTwoSeconds();
        logger.info("Apparently folder '" + foldername + "' has been added");
    }

    /**
     * Helper
     */

    private void enterFolder(String foldername) {
        Logger logger = LOGGER_enterFolder;
        waitForExportImportInAddressBookMenu();
        logger.info("Trying to enter '" + foldername + "'");
        driver.findElement(By.cssSelector("span.icon_manager.navigation_mail")).click();
        Sleep.sleepTwoSeconds();
        driver.findElement(By.cssSelector("span.icon_manager.navigation_addr")).click();
        Sleep.sleepTwoSeconds();
        //
        // Click at least twice then click once more if "Contacts" is not yet there
        //
        driver.findElement(By.xpath("(//a[contains(text(),'" + config.getUsername() + "')])[2]")).click();
        Sleep.sleepTwoSeconds();
        driver.findElement(By.xpath("(//a[contains(text(),'" + config.getUsername() + "')])[2]")).click();
        Sleep.sleepTwoSeconds();
        if (!isElementPresent(By.xpath("//a[contains(text(),'" + foldername + "')]"))) {
            driver.findElement(By.xpath("(//a[contains(text(),'" + config.getUsername() + "')])[2]")).click();
            Sleep.sleepTwoSeconds();
        }
        driver.findElement(By.xpath("//a[contains(text(),'" + foldername + "')]")).click();
        Sleep.sleepTwoSeconds();
        
        checkTrue(isElementPresent(By.id("icon_action_add")), "Looked for 'icon_action_add' action");
        checkTrue(driver.findElement(By.cssSelector("BODY")).getText().contains(foldername), "Looked for '" + foldername + "'");
        checkTrue(driver.findElement(By.cssSelector("BODY")).getText().contains("No items"), "Looked for 'No items'");
        logger.info("Apparently we are ready to enter contacts");
    }

    /**
     * Helper
     */

    private void enterRelevantContacts(Acceptor acceptor) {
        Logger logger = LOGGER_enterRelevantContacts;
        for (ClubMember m : allMembers) {
            if (acceptor.accept(m)) {
                int count = 0;
                for (String addr : m.emailAddressList) {
                    logger.info("Trying to add " + m);
                    driver.findElement(By.id("icon_action_add")).click();
                    Sleep.sleepTwoSeconds();
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
                    Sleep.sleepTwoSeconds();
                    logger.info("Looks like this succeeded!");
                }
            }
        }
    }

    /**
     * Helper
     */

    private void createAddressbook(String foldername, Acceptor acceptor) {
        @SuppressWarnings("unused")
        Logger logger = LOGGER_createAddressbook;
        //
        goToWebmailLoginPage();
        //
        loginToWebmail();
        //
        openContactsMenu();
        //
        makeSureFolderDoesNotExist(foldername);
        //
        addNewFolder(foldername);
        //
        openContactsMenu();
        //
        makeSureFolderDoesExist(foldername);
        //
        enterFolder(foldername);
        //
        enterRelevantContacts(acceptor);
        //
        logoutFromWebmail();
    }

    /**
     * Helper
     */

    @SuppressWarnings("unused")
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
        createAddressbook("### Subset: Comité ###", acceptor);
    }

    /**
     * Proper action: Adding all the black belters. The "Acceptor" returns true if a
     * given "ClubMember" is detected as being a "black belter"
     */

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
        createAddressbook("### Subset: Ceintures Noires ###", acceptor);
    }

    /**
     * Proper action: Adding all the children. The "Acceptor" returns true if a
     * given "ClubMember" is detected as being a "child"
     */

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
        createAddressbook("### Subset: Enfants ###", acceptor);
    }

    /**
     * Proper action: Adding all the adults. The "Acceptor" returns true if a
     * given "ClubMember" is detected as being an "adult"
     */

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
        createAddressbook("### Subset: Adultes ###", acceptor);
    }

    /**
     * Proper action: Adding everybody
     */

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
        createAddressbook("### Tous les Membres du Club ###", acceptor);
    }

    /**
     * Main
     */

    private enum Action {
        addSubsetCommittee, addSubsetCeinturesNoires, addSubsetEnfants, addSubsetAdultes, addSubsetTousLesMembres
    }

    private static SortedSet<Action> actionSet = new TreeSet<Action>();
    
    static {
        actionSet.add(Action.addSubsetCommittee);
        actionSet.add(Action.addSubsetCeinturesNoires);
        actionSet.add(Action.addSubsetEnfants);
        actionSet.add(Action.addSubsetAdultes);
        actionSet.add(Action.addSubsetTousLesMembres);
    }
    
    @SuppressWarnings("unchecked")
    public static void main(String[] argv) {
        Logger logger = LoggerFactory.getLogger(CLASS + ".main");
        //
        // Fire up logging through "SLF4J" over "Logback"
        // This looks like a zero-effect statement but it's actually an initialization
        //
        {
            @SuppressWarnings("unused")
            Object foo = new LogbackStarter(InjectIntoWebmailAddressBook.class);
        }
        //
        // The resource "config.xml" in the package in which "Hook" resides is read and a directly
        // usable structure is returned.
        //
        PropertiesReader config = new PropertiesReader(name.gluino.webmailfeed.data.Hook.class, "config.xml");
        //
        // Hoover up all the member information and put it into a Set<Member>
        //
        Set<ClubMember> allMembers;
        try {
            allMembers = (new MemberListSlurper(config.getFqInputResource(), config.getCommitteEmails())).members;
        } catch (Exception exe) {
            throw new IllegalStateException(exe);
        }
        //
        // Run interaction
        //
        for (Action ax : actionSet) {

            InjectIntoWebmailAddressBook inj = null;
            try {
                inj = new InjectIntoWebmailAddressBook(config, allMembers);
                switch (ax) {
                case addSubsetCommittee:
                    inj.addSubsetCommittee();
                    break;
                case addSubsetCeinturesNoires:
                    inj.addSubsetCeinturesNoires();
                    break;
                case addSubsetEnfants:
                    inj.addSubsetEnfants();
                    break;
                case addSubsetAdultes:
                    inj.addSubsetAdultes();
                    break;
                case addSubsetTousLesMembres:
                    inj.addSubsetTousLesMembres();
                    break;
                default:
                    // NOP
                }
            } catch (Exception exe) {
                logger.error("While running", exe);
                System.exit(1);
            } finally {
                if (inj != null) {
                    inj.tearDown();
                }
            }
        }
    }
}
