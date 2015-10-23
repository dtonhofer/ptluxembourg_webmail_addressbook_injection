package name.heavycarbon.webmailfeed;

import static name.heavycarbon.checks.BasicChecks.checkFalse;
import static name.heavycarbon.checks.BasicChecks.checkNotNull;
import static name.heavycarbon.checks.BasicChecks.checkTrue;
import static name.heavycarbon.checks.BasicChecks.instaFail;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.heavycarbon.logging.LogFacilities;
import name.heavycarbon.logstarter.LogbackStarter;
import name.heavycarbon.utils.Sleep;
import name.heavycarbon.webmailfeed.Actions.Action;

/* 34567890123456789012345678901234567890123456789012345678901234567890123456789
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
 * ALl the ops seem to follow this scheme:
 * 
 * VERIFY->ESTABLISH->DO->VERIFY->ESTABLISH
 * 
 * with VERIFY or ESTABLISH sometimes short-circuited
 * 
 * Actual behaviour is still a bit arbitrary.
 * 
 * JavaDoc at:
 * 
 * https://selenium.googlecode.com/git-history/master/docs/api/java/index.html?org/openqa/selenium/WebDriver.html
 * 
 * 2013.04.XX - Created
 * 2013.06.14 - Cleanup
 * 2015.09.13 - Added "enfants & adolescents"
 ******************************************************************************/

public class InjectIntoWebmailAddressBook {

    private final static String CLASS = InjectIntoWebmailAddressBook.class.getName();
    private final static Logger LOGGER_loginToWebmail = LoggerFactory.getLogger(CLASS + ".loginToWebmail");
    private final static Logger LOGGER_goToWebmailLoginPage = LoggerFactory.getLogger(CLASS + ".goToWebmailLoginPage");
    private final static Logger LOGGER_createAddressbook = LoggerFactory.getLogger(CLASS + ".createAddressbook");
    private final static Logger LOGGER_logoutFromWebmail = LoggerFactory.getLogger(CLASS + ".logoutFromWebmail");
    private final static Logger LOGGER_openContactsMenuFromTheOverviewPage = LoggerFactory.getLogger(CLASS + ".openContactsMenuFromTheOverviewPage");
    private final static Logger LOGGER_makeSureFolderDoesNotExist = LoggerFactory.getLogger(CLASS + ".makeSureFolderDoesNotExist");
    private final static Logger LOGGER_addNewFolder = LoggerFactory.getLogger(CLASS + ".addNewFolder");
    private final static Logger LOGGER_makeSureFolderDoesExist = LoggerFactory.getLogger(CLASS + ".makeSureFolderDoesExist");
    private final static Logger LOGGER_enterFolder = LoggerFactory.getLogger(CLASS + ".enterFolder");
    private final static Logger LOGGER_enterRelevantContacts = LoggerFactory.getLogger(CLASS + ".enterRelevantContacts");
    private final static Logger LOGGER_verifyThatThisIsTheWebmailLoginPage = LoggerFactory.getLogger(CLASS + ".verifyThatThisIsTheWebmailLoginPage");
    private final static Logger LOGGER_verifyThatWeAreInInAddressBookMenu = LoggerFactory.getLogger(CLASS + ".verifyThatWeAreInInAddressBookMenu");
    private final static Logger LOGGER_makeSureTheListOfContactsFoldersIsVisible = LoggerFactory.getLogger(CLASS + ".makeSureTheListOfContactsFoldersIsVisible");
    private final static Logger LOGGER_verifyThatThisIsTheOverviewPage = LoggerFactory.getLogger(CLASS + ".verifyThatThisIsTheOverviewPage");
    private final static Logger LOGGER_openListOfContactFolders = LoggerFactory.getLogger(CLASS + ".openListOfContactFolders");
    private final static Logger LOGGER_sleepabit = LoggerFactory.getLogger(CLASS + ".sleepabit");

    private final PropertiesReader config; // immutable configuration
    private final Set<ClubMember> allMembers; // immutable set of all the club
                                              // members

    private final WebDriver driver;
    private final String testTag;

    /**
     * We also have a simple facility to store several problems in a string
     * buffer
     */

    private boolean acceptNextAlert = true;
    private StringBuffer verificationErrors = new StringBuffer();

    /**
     * Constructor
     */

    public InjectIntoWebmailAddressBook(PropertiesReader config, Set<ClubMember> allMembers, int secondsToImplicitylWait, String testTag) {
        checkNotNull(config, "config");
        checkNotNull(allMembers, "allMembers");
        checkNotNull(testTag, "testTag");
        this.config = config;
        this.allMembers = Collections.unmodifiableSet(allMembers);
        this.testTag = testTag;
        this.driver = new FirefoxDriver();
        this.driver.manage().timeouts().implicitlyWait(secondsToImplicitylWait, TimeUnit.SECONDS);
    }

    /**
     * Cleaning up, tear down connection, once all has been done
     */

    public void tearDown() {
        driver.quit();
        String verificationErrorString = verificationErrors.toString();
        if (!"".equals(verificationErrorString)) {
            instaFail(verificationErrorString);
        }
    }

    private static void sleepabit() {
        Logger logger = LOGGER_sleepabit;
        final int time_ms = 1000; // 500ms too fast and so is 1000ms from time
                                  // to time
        logger.info("...waiting " + time_ms + " ms");
        Sleep.sleepFor(time_ms);
    }

    private boolean isElementPresent(By by) {
        try {
            driver.findElement(by);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private void verifyThatWeAreInInAddressBookMenu() {
        Logger logger = LOGGER_verifyThatWeAreInInAddressBookMenu;
        logger.info(">>> Verifying whether we are really in address book menu");
        checkTrue(driver.findElement(By.cssSelector("BODY")).getText().matches("^[\\s\\S]*Contacts[\\s\\S]*$"), "Looked for 'Contacts' but didn't find it");
        logger.info("Polling contents until all expected items are there");
        for (int second = 0;; second++) {
            checkFalse(second >= 60, "timeout waiting for address book menu 'export' to appear");
            try {
                if (isElementPresent(By.id("text_action_export")))
                    break;
            } catch (Exception e) {
                // NOP

            }
            Sleep.sleepHalfASecond();
        }
        for (int second = 0;; second++) {
            checkFalse(second >= 60, "timeout waiting for address book menu 'address import' to appear");
            try {
                if (isElementPresent(By.id("text_action_import_addrimport")))
                    break;
            } catch (Exception e) {
                // NOP
            }
            Sleep.sleepHalfASecond();
        }
        checkTrue(isElementPresent(By.id("icon_action_add_folder")), "Add folder icon should be there but didn't find it");
        logger.info("<<< Apparently we are really in address book menu");

    }

    private void openListOfContactFolders(String someString) {
        Logger logger = LOGGER_openListOfContactFolders;
        logger.info(">>> Aggressively clicking on the 'username' '" + config.getUsername() + "' to make the list of contact folders appear");
        logger.info("...clicking once");
        driver.findElement(By.xpath("(//a[contains(text(),'" + config.getUsername() + "')])[2]")).click();
        logger.info("...waiting a bit");
        sleepabit();
        logger.info("...clicking again");
        driver.findElement(By.xpath("(//a[contains(text(),'" + config.getUsername() + "')])[2]")).click();
        logger.info("...waiting a bit");
        sleepabit();
        logger.info("Is the list of contacts there?");
        if (!isElementPresent(By.xpath("//a[contains(text(),'" + someString + "')]"))) {
            logger.info("...no -- clicking a third time");
            driver.findElement(By.xpath("(//a[contains(text(),'" + config.getUsername() + "')])[2]")).click();
            logger.info("...waiting a bit");
            sleepabit();
        }
        logger.info("<<< The list of contact folders should now be there, in particular folder '" + someString + "'");
    }

    private void makeSureTheListOfContactsFoldersIsVisible() {
        Logger logger = LOGGER_makeSureTheListOfContactsFoldersIsVisible;
        logger.info(">>> Verifying whether the list of contact folders is visible");
        checkTrue(isElementPresent(By.xpath("//a[contains(text(),'Contacts')]")), "Was looking for 'Contacts'");
        logger.info("<<< Apparently the list of contact folders is now visible");
    }

    private void goToWebmailLoginPage() {
        Logger logger = LOGGER_goToWebmailLoginPage;
        logger.info(">>> Opening webmail login page at " + config.getBaseURL());
        driver.get(config.getBaseURL() + "/webmail");
        logger.info("<<< Webmail login page apparently opened");
    }

    private void verifyThatThisIsTheWebmailLoginPage() {
        Logger logger = LOGGER_verifyThatThisIsTheWebmailLoginPage;
        logger.info(">>> Verifying whether this is really the webmail login page");
        String text = driver.findElement(By.cssSelector("BODY")).getText();
        logger.info("Text in BODY: '" + LogFacilities.mangleString(text) + "'");
        // Unbelievably, the next test fails from time to time...
        checkTrue(text.contains("Webmail Login"), "Did not find 'Webmail Login'");
        checkTrue(text.contains("POST Luxembourg"), "Did not find 'POST Luxembourg'");
        logger.info("<<< Apparently this is really the webmail login page");
    }

    private void verifyThatThisIsTheOverviewPage() {
        Logger logger = LOGGER_verifyThatThisIsTheOverviewPage;
        logger.info(">>> Verifying whether this is really the overview page");
        String title = driver.getTitle();
        logger.info("Title of page: '" + LogFacilities.mangleString(title) + "'");
        String text = driver.findElement(By.cssSelector("BODY")).getText();
        logger.info("Text in BODY: '" + LogFacilities.mangleString(text) + "'");
        checkTrue(text.contains(config.getUsername()), "Looked for '" + config.getUsername() + "' but didn't find it");
        checkTrue(text.contains("Logout"), "Looked for 'Logout' but didn't find it");
        checkTrue(isElementPresent(By.cssSelector("span.icon_manager.navigation_addr")), "The 'Contacts' icon is not there");
        logger.info("<<< Apparently this is really the overview page");
    }

    private void loginToWebmail() {
        Logger logger = LOGGER_loginToWebmail;
        {
            logger.info(">>> Logging in to Webmail");
            driver.findElement(By.name("user")).clear();
            driver.findElement(By.name("user")).sendKeys(config.getUsername());
            driver.findElement(By.name("password")).clear();
            driver.findElement(By.name("password")).sendKeys(config.getPassword());
            new Select(driver.findElement(By.name("lang"))).selectByVisibleText("English");
            logger.info("Clicking 'submit'");
            driver.findElement(By.cssSelector("button[type=\"submit\"]")).click();
            logger.info("...waiting a bit");
            sleepabit();
            logger.info("<<< We should be logged in now");
        }
        {
            logger.info(">>> Making sure by reloading the overview page");
            driver.get(config.getBaseURL() + "/webmail/overview");
            logger.info("<<< Overview page should be loaded now");
        }
        {
        }
    }

    private void logoutFromWebmail() {
        Logger logger = LOGGER_logoutFromWebmail;
        {
            logger.info(">>> Logging out by clicking on 'Logout'");
            driver.findElement(By.linkText("Logout")).click();
            sleepabit();
            logger.info("<<< We should be logged out now");
        }
        verifyThatThisIsTheWebmailLoginPage();
    }

    /**
     * Helper
     */

    private void openContactsMenuFromTheOverviewPage() {
        Logger logger = LOGGER_openContactsMenuFromTheOverviewPage;
        logger.info(">>> Opening contacts menu");
        logger.info("Clicking on icon");
        driver.findElement(By.cssSelector("span.icon_manager.navigation_addr")).click();
        sleepabit();
        logger.info("<<< Contacts menu should now be open");
    }

    private void makeSureFolderDoesNotExist(String foldername) {
        Logger logger = LOGGER_makeSureFolderDoesNotExist;
        logger.info(">>> Making sure there is no contacts folder called '" + foldername + "'");
        // for some reason this takes a LONG time
        checkFalse(isElementPresent(By.xpath("//a[contains(text(),'" + foldername + "')]")), "Was looking for for the absence of '" + foldername + "' ... but found it");
        logger.info("<<< Apparently there is no contacts folder called '" + foldername + "'");
    }

    private void addNewFolder(String foldername) {
        Logger logger = LOGGER_addNewFolder;
        logger.info(">>> Creating a new contacts folder called '" + foldername + "'");
        logger.info("Clicking on 'add folder' icon");
        driver.findElement(By.id("icon_action_add_folder")).click();
        sleepabit();
        logger.info(">>> Verifying whether we are in the 'add folder' page");
        checkTrue(isElementPresent(By.id("_button_cancel")), "Looked for CANCEL button but didn't find it");
        checkTrue(isElementPresent(By.id("_button_save")), "Looked for SAVE  button but didn't find it");
        checkTrue(isElementPresent(By.id("parent")), "Looked for 'parent folder' entry field but didn't find it");
        checkTrue(isElementPresent(By.id("name")), "Looked for 'name' entry field but didn't find it");
        logger.info("<<< Apparently we are in the 'add folder' page");
        logger.info("Filling fields to create new folder");
        driver.findElement(By.id("name")).clear();
        driver.findElement(By.id("name")).sendKeys(foldername);
        logger.info("Clicking on 'save'");
        driver.findElement(By.id("_button_save")).click();
        sleepabit();
        logger.info("<<< Apparently folder '" + foldername + "' has been added");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private String makeSureFolderDoesExist(String foldername) {
        Logger logger = LOGGER_makeSureFolderDoesExist;
        logger.info(">>> Making sure there is indeed a contacts folder called '" + foldername + "' now");
        // the display only shows ~30 chars, then continues with an ellipsis!
        // this means we have to limit our search a bit
        List<String> poss = new LinkedList();
        poss.add(foldername);
        int i = Math.min(foldername.length(), 32 - "...".length());
        poss.add(foldername.substring(0, i) + "...");
        String foundStr = null;
        for (String x : poss) {
            logger.info("Checking '" + x + "'");
            // The test below is exceedingly slow!
            if (isElementPresent(By.xpath("//a[contains(text(),'" + x + "')]"))) {
                foundStr = x;
                break;
            }
        }
        checkTrue(foundStr != null, "Was looking for '" + foldername + "' or an abbreviation thereof and didn't find it");
        logger.info("<<< Apparently there is a contacts folder called '" + foundStr + "' now");
        return foundStr;
    }

    private void enterFolder(String foldername) {
        Logger logger = LOGGER_enterFolder;
        logger.info(">>> Entering '" + foldername + "'");
        logger.info("Clicking on navigation mail");
        driver.findElement(By.cssSelector("span.icon_manager.navigation_mail")).click();
        sleepabit();
        logger.info("Clicking on navigation address");
        driver.findElement(By.cssSelector("span.icon_manager.navigation_addr")).click();
        sleepabit();
        openListOfContactFolders(foldername);
        logger.info("Entering folder '" + foldername + "'");
        driver.findElement(By.xpath("//a[contains(text(),'" + foldername + "')]")).click();
        sleepabit();
        checkTrue(isElementPresent(By.id("icon_action_add")), "Looked for 'icon_action_add' action");
        checkTrue(driver.findElement(By.cssSelector("BODY")).getText().contains(foldername), "Looked for '" + foldername + "'");
        // This no longer works. If there are no items, a blank page is
        // displayed!
        // checkTrue(driver.findElement(By.cssSelector("BODY")).getText().contains("No
        // items"), "Looked for 'No items'");
        logger.info("<<< Apparently we are in folder '" + foldername + "' and ready to enter contacts");
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
                    sleepabit();
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
                    sleepabit();
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
        {
            goToWebmailLoginPage();
            verifyThatThisIsTheWebmailLoginPage();
        }
        {
            loginToWebmail();
            verifyThatThisIsTheOverviewPage();
        }
        {
            openContactsMenuFromTheOverviewPage(); // this may fail and the next
                                                   // test will fail too ;
                                                   // re-engineer this
            verifyThatWeAreInInAddressBookMenu();
            makeSureTheListOfContactsFoldersIsVisible();
        }
        {
            openListOfContactFolders("Contacts");
            makeSureTheListOfContactsFoldersIsVisible();
        }
        {
            // If folder exists, this fails and operation will stop
            makeSureFolderDoesNotExist(foldername);
        }
        {
            addNewFolder(foldername);
            openContactsMenuFromTheOverviewPage();
            String foundStr = makeSureFolderDoesExist(foldername);
            enterFolder(foundStr);
        }
        {
            enterRelevantContacts(acceptor);
        }
        {
            logoutFromWebmail();
        }
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
     * Proper action: Adding all committee member. The "Acceptor" returns true
     * if a given "ClubMember" is detected as being a "committee member"
     */

    private final Acceptor acceptorCommittee = new Acceptor() {
        public boolean accept(ClubMember x) {
            assert x != null;
            return x.isCommittee();
        }

        public String getCompany(ClubMember x) {
            assert x != null;
            return "Comité ACL";
        }
    };

    private final Acceptor acceptorCeintureNoires = new Acceptor() {
        public boolean accept(ClubMember x) {
            assert x != null;
            return x.isCeintureNoire();
        }

        public String getCompany(ClubMember x) {
            assert x != null;
            return "Ceinture Noire: " + ((x.level == null) ? "?" : x.level);
        }
    };

    private final Acceptor acceptorEnfants = new Acceptor() {
        public boolean accept(ClubMember x) {
            assert x != null;
            return x.isEnfant();
        }

        public String getCompany(ClubMember x) {
            assert x != null;
            return "Enfant âgé de: " + x.getAge() + " ans";
        }
    };

    private final Acceptor acceptorAdultes = new Acceptor() {
        public boolean accept(ClubMember x) {
            assert x != null;
            return x.isAdulte();
        }

        public String getCompany(ClubMember x) {
            assert x != null;
            return "Adulte âgé de: " + x.getAge() + " ans";
        }
    };

    private final Acceptor acceptorEnfantsEtAdolescents = new Acceptor() {
        public boolean accept(ClubMember x) {
            assert x != null;
            return x.getAge() < 19;
        }

        public String getCompany(ClubMember x) {
            assert x != null;
            return "Membre âgé de: " + x.getAge() + " ans";
        }
    };

    private final Acceptor acceptorTousLesMembres = new Acceptor() {
        public boolean accept(ClubMember x) {
            return true;
        }

        public String getCompany(ClubMember x) {
            assert x != null;
            String res = "Membre âgé de: " + x.getAge() + " ans";
            if (acceptorCeintureNoires.accept(x)) {
                res += ", ceinture noire";
            }
            if (acceptorAdultes.accept(x)) {
                res += ", adulte";
            }
            if (acceptorEnfants.accept(x)) {
                res += ", enfant";
            }
            return res;
        }
    };

    /**
     * Proper action: Adding all the black belters. The "Acceptor" returns true
     * if a given "ClubMember" is detected as being a "black belter"
     */

    public void addSubsetCeinturesNoires() {
        createAddressbook(testTag + "### Subset: Ceintures Noires ###", acceptorCeintureNoires);
    }

    /**
     * Proper action: Adding all the children. The "Acceptor" returns true if a
     * given "ClubMember" is detected as being a "child"
     */

    public void addSubsetEnfants() {
        createAddressbook(testTag + "### Subset: Enfants ###", acceptorEnfants);
    }

    /**
     * Proper action: Adding all the adults. The "Acceptor" returns true if a
     * given "ClubMember" is detected as being an "adult"
     */

    public void addSubsetAdultes() {
        createAddressbook(testTag + "### Subset: Adultes ###", acceptorAdultes);
    }

    /**
     * Proper action: Adding all all the people < 19. The "Acceptor" returns
     * true if a given "ClubMember" is detected as being an "enfant/adolescent"
     */

    public void addSubsetEnfantsEtAdolescents() {
        createAddressbook(testTag + "### Subset: Enfants et Adolescents ###", acceptorEnfantsEtAdolescents);
    }

    /**
     * Proper action: Adding everybody
     */

    public void addSubsetTousLesMembres() {
        createAddressbook(testTag + "### Tous les Membres du Club ###", acceptorTousLesMembres);
    }

    /**
     * Proper action: Adding committee members
     */

    public void addSubsetCommittee() {
        createAddressbook(testTag + "### Subset: Comité ###", acceptorCommittee);
    }

    /**
     * ===== MAIN =====
     */

    @SuppressWarnings("unchecked")
    public static void main(String[] argv) {
        Logger logger = LoggerFactory.getLogger(CLASS + ".main");
        //
        // Fire up logging through "SLF4J" on top of "Logback"
        // This looks like a zero-effect statement but it's actually an
        // initialization
        //
        new LogbackStarter(InjectIntoWebmailAddressBook.class);
        //
        // The resource "config.xml" in the package in which "Hook" resides is
        // read and a directly
        // usable structure is returned.
        //
        PropertiesReader config = new PropertiesReader(name.heavycarbon.webmailfeed.data.Hook.class, "config.xml");
        //
        // Hoover up all the member information and put it into a Set<Member>
        //
        Set<ClubMember> allMembers;
        try {
            allMembers = (new MemberListSlurper(config.getFqInputResource(), config.getCommitteEmails())).members;
        } catch (Exception exe) {
            throw new IllegalStateException("While reading list of members", exe);
        }
        //
        // Run interaction
        //
        // POST can be very slow, thus:
        final int secondsToImplicityWait = 10;
        //
        // Set this to empty in case of "production"
        //
        final String testTag = "TEST";
        // final String testTag = "";
        //
        //
        for (Action ax : Actions.ACTION_SET) {
            InjectIntoWebmailAddressBook inj = null;
            try {
                inj = new InjectIntoWebmailAddressBook(config, allMembers, secondsToImplicityWait, testTag);
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
                case addSubsetEnfantsEtAdolescents:
                    inj.addSubsetEnfantsEtAdolescents();
                    break;
                default:
                    instaFail("Unknown action " + ax);
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
        logger.info("SUCCESS");
    }
}
