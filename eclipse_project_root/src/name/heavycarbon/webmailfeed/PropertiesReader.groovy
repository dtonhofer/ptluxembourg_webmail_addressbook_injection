package name.heavycarbon.webmailfeed

import static name.heavycarbon.checks.BasicChecks.*

import name.heavycarbon.groovyutils.ResourceHelp
import name.heavycarbon.utils.ResourceInfo
import name.heavycarbon.utils.MailAddressAcceptor

/* 34567890123456789012345678901234567890123456789012345678901234567890123456789
 *******************************************************************************
 * A class that read an XML resource and presents the data it found 
 *
 * Config file looks like this:
 * 
 * <config>
 *    <credentials>
 *        <username>XXXXXXXXXX</username>
 *        <password>YYYYYYYYYY</password>
 *        <email>ZZZZZZZZZZZ</email>
 *    </credentials>
 *    <base_url>
 *        https://webmail.pt.lu/
 *    </base_url>
 *   <emails_of_comittee_members>
 *        <email>AAAAAAAAAAAAAAAAAAAAAA</email>
 *        <email>BBBBBBBBBBBBBBBBBBBBBB</email>
 *        <email>CCCCCCCCCCCCCCCCCCCCCC</email>
 *    </emails_of_comittee_members>
 *    <input_resource>
 *        membres_actuels.txt
 *    </input_resource>
 * </config>
 * 
 * 2013.04.XX - Created
 * 2013.06.14 - Cleanup
 ******************************************************************************/

class PropertiesReader {

    final String username // not null: username to logs on to webmail
    final String password // not null: password to logs on to webmail
    final String userEmail // not null: user's email; we are looking for this in the web page
    final String baseURL // not null: the base URL of all the pages
    final String fqInputResource // not null: name of the resourcing holding the membership list
    final Set committeEmails // not null, immutable: list of commitee member's e-mail addresses

    /**
     * Constructor reads a resource containing XML and extracts the data using Groovy's XmlSlurper
     */

    PropertiesReader(Class hookClass, String unqualifiedResourceName) {
        String xmltxt = ResourceHelp.slurpText(new ResourceInfo(hookClass, unqualifiedResourceName, "UTF-8"))
        def config = new XmlSlurper().parseText(xmltxt)
        username = config.credentials.username.text().trim()
        password = config.credentials.password.text().trim()
        userEmail = config.credentials.email.text().trim()
        baseURL = config.base_url.text().trim()
        def emails = config.emails_of_committee_members.email
        Set<String> mutableSet = new HashSet();
        emails.each {
            String email = it.text().trim()
            checkTrue(MailAddressAcceptor.acceptAddress(email),"Email address '{}' is not acceptable",email)
            boolean added = mutableSet.add(email)
            checkTrue(added, "Email address '%s' seen twice",email)
        }
        committeEmails = Collections.unmodifiableSet(mutableSet)
        fqInputResource = ResourceHelp.fullyQualifyResourceName(hookClass, config.input_resource.text())
        checkNotNullAndNotOnlyWhitespace(username,"username is unset")
        checkNotNullAndNotOnlyWhitespace(password,"password is unset")
        checkNotNullAndNotOnlyWhitespace(userEmail,"user email is unset")
        checkNotNullAndNotOnlyWhitespace(baseURL,"baseURL is unset")
        checkFalse(committeEmails.isEmpty(),"No committee emails found at all")
        checkNotNullAndNotOnlyWhitespace(fqInputResource,"fqInputResource is unset")
    }
}
