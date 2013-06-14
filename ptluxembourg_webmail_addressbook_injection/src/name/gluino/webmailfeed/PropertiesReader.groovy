package name.gluino.webmailfeed

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.XmlSupport;

import com.mplify.checkers._check;
import com.mplify.msgserver.addressing.AddressAcceptor;
import com.mplify.resources.ResourceHelpers;
import com.mplify.xml.JDomHelper;

/* 34567890123456789012345678901234567890123456789012345678901234567890123456789
 * *****************************************************************************
 * Copyright (c) 2013, David Tonhofer
 *
 * Distributed under: "The MIT License" (http://opensource.org/licenses/MIT)
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
        String xmltxt = ResourceHelpers.slurpResource(hookClass, unqualifiedResourceName, "UTF-8")
        def config = new XmlSlurper().parseText(xmltxt)
        username = config.credentials.username.text()
        password = config.credentials.password.text()
        userEmail = config.credentials.email.text()
        baseURL = config.base_url.text()
        def emails = config.emails_of_comittee_members.email
        Set<String> mutableSet = new HashSet();
        emails.each {
            String email = it.text().trim()
            _check.isTrue(AddressAcceptor.acceptAddress(email),"Email address '%s' is not acceptable",email)
            boolean added = mutableSet.add(email)
            _check.isTrue(added, "Email address '%s' seen twice",email)
        }
        committeEmails = Collections.unmodifiableSet(mutableSet)
        fqInputResource = ResourceHelpers.fullyQualifyResourceName(hookClass, config.input_resource.text())
        _check.notNullAndNotOnlyWhitespace(username,"username is unset")
        _check.notNullAndNotOnlyWhitespace(password,"password is unset")
        _check.notNullAndNotOnlyWhitespace(userEmail,"user email is unset")
        _check.notNullAndNotOnlyWhitespace(baseURL,"baseURL is unset")
        _check.isFalse(committeEmails.isEmpty(),"No committee emails found at all")
        _check.notNullAndNotOnlyWhitespace(fqInputResource,"fqInputResource is unset")
    }
}
