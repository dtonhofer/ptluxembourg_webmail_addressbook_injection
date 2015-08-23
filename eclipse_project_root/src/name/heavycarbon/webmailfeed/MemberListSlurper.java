package name.heavycarbon.webmailfeed;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.example.BasicChecks.*;

import com.example.resources.ResourceHelp_Java;
import com.mplify.logstarter.LogbackStarter;
import com.mplify.tools.MailAddressAcceptor;

import name.heavycarbon.webmailfeed.data.Hook;

/* 34567890123456789012345678901234567890123456789012345678901234567890123456789
 * *****************************************************************************
 * Copyright (c) 2013, David Tonhofer
 *
 * Distributed under: "The MIT License" (http://opensource.org/licenses/MIT)
 *******************************************************************************
 * A class that read the CSV file which contains the memberlist
 * 
 * 2013.04.XX - Created
 * 2013.06.14 - Cleanup
 ******************************************************************************/

class MemberListSlurper {

    private final static String CLASS = MemberListSlurper.class.getName();
    private final static Logger LOGGER_readMembers = LoggerFactory.getLogger(CLASS + ".readMembers");
    private final static Logger LOGGER_makeLevel = LoggerFactory.getLogger(CLASS + ".makeLevel");

    private final static Map<String, Level> LEVEL_MAPPING;

    static {
        //
        // Codes for the KYU and DAN levels in the input
        //
        {
            Map<String, Level> map = new HashMap();
            map.put("6k", Level.KYU_6);
            map.put("5k", Level.KYU_5);
            map.put("4k", Level.KYU_4);
            map.put("3k", Level.KYU_3);
            map.put("2k", Level.KYU_2);
            map.put("1k", Level.KYU_1);
            map.put("1d", Level.DAN_1);
            map.put("2d", Level.DAN_2);
            map.put("3d", Level.DAN_3);
            map.put("4d", Level.DAN_4);
            map.put("5d", Level.DAN_5);
            map.put("6d", Level.DAN_6);
            LEVEL_MAPPING = Collections.unmodifiableMap(map);
        }
    }

    /**
     * The accessible result: Set of Member, immutable
     */

    public final Set<ClubMember> members;

    /**
     * Constructor reads the passed resource; in order to flag a member of the
     * club as "member of the committee", the list of committee e-mails is
     * passed in too.
     */

    public MemberListSlurper(String fqInputResource, Set<String> committeeEmails) throws IOException {
        members = Collections.unmodifiableSet(readMembers(fqInputResource, committeeEmails));
    }

    /**
     * Do it
     */

    private static Set<ClubMember> readMembers(String fqInputResource, Set<String> committeeEmails) throws IOException {
        Logger logger = LOGGER_readMembers;
        checkNotNull(committeeEmails);
        Set<ClubMember> res = new HashSet();
        String data = ResourceHelp_Java.slurpResource(fqInputResource, "UTF-8");
        LineNumberReader lnr = new LineNumberReader(new StringReader(data));
        String current;
        String separator = ",";
        // FAMILYNAME , FirstName , EMAIL , Birthday as "DAY/MONTH/YEAR" , Level
        // email may be missing or there may be several e-mails, separated by
        // space
        // birth date may be missing
        // level indicator may be missing
        String rawPattern = "";
        rawPattern += "(.+?)"; // FAMILYNAME
        rawPattern += separator;
        rawPattern +=  "(.+?)"; // FirstName
        rawPattern += separator;
        rawPattern += "(.*?)"; // EMAIL
        rawPattern += separator;
        rawPattern += "(.*?)"; // Birthday
        rawPattern += separator;
        rawPattern += "(.*?)"; // Level
        rawPattern += (separator + "?");        
        Pattern pat = Pattern.compile(rawPattern);
        while ((current = lnr.readLine()) != null) {
            Matcher m = pat.matcher(current);
            if (m.matches()) {
                //
                // Names
                //
                String firstName = m.group(2).trim();
                String lastName = m.group(1).trim();
                //
                // Email; break off if none there
                //
                String emailAddressOverall = m.group(3).trim();
                if (emailAddressOverall.isEmpty()) {
                    logger.info("No e-mail address in line '" + current + "' -- skipping this");
                    continue;
                }
                boolean nope = false;
                List<String> emailAddressList = new LinkedList();
                {
                    String[] emailAddressArray = emailAddressOverall.split("\\s+");
                    for (String candidate : emailAddressArray) {
                        String cx = candidate.trim();
                        if (cx.isEmpty()) {
                            continue;
                        } else {
                            if (!MailAddressAcceptor.acceptAddress(cx)) {
                                logger.error("Could not understand email '" + cx + "' in line '" + current + "'");
                                nope = true;
                            } else {
                                emailAddressList.add(cx);
                            }
                        }
                    }
                }
                if (nope || emailAddressList.isEmpty()) {
                    logger.error("No usable e-mail address in line '" + current + "'");
                    continue;
                }
                //
                // Birthday, if it exists
                //
                DateTime birthday = null;
                {
                    String rawBirthDay = makeNullIfEmpty(m.group(4));
                    if (rawBirthDay != null) {                        
                        birthday = makeBirthday(rawBirthDay);
                        if (birthday == null) {
                            logger.error("Could not understand birthday '" + rawBirthDay + "' in line '" + current + "'");
                            System.exit(1);
                        }
                    }
                }
                //
                // Level
                //
                Level level = null;
                {
                    String rawLevel = makeNullIfEmpty(m.group(5));
                    if (rawLevel != null) {
                        level = makeLevel(rawLevel);
                        if (level == null) {
                            logger.error("Could not understand level '" + rawLevel + "' in line '" + current + "'");
                            System.exit(1);
                        }
                    }
                }
                //
                // Member of the committee?
                //
                boolean isComite = false;
                for (String e : emailAddressList) {
                    isComite = isComite || (committeeEmails.contains(e));
                }
                //
                // All the data has been collected; construct instance; this
                // will check the values
                //
                ClubMember membre = new ClubMember(firstName, lastName, emailAddressList, birthday, level, isComite);
                res.add(membre);
                logger.info(membre.toString());
            } else {
                logger.error("Could not match line '" + current + "'");
            }
        }
        return res;
    }

    /**
     * Helper
     */

    private static DateTime makeBirthday(String str) {
        Pattern pat = Pattern.compile("(\\d\\d)(/|\\.)(\\d\\d)(/|\\.)(\\d\\d)");
        Matcher m = pat.matcher(str);
        if (m.matches()) {
            String dayStr = m.group(1);
            String monthStr = m.group(3);
            String yearStr = m.group(5);
            int day = Integer.parseInt(dayStr);
            int month = Integer.parseInt(monthStr);
            int year = Integer.parseInt(yearStr);
            // Y2K problem, again!
            if (year > 30) {
                year = year + 1900;
            } else {
                year = year + 2000;
            }
            return new DateTime(year, month, day, 12, 0, 0, 0);
        } else {
            return null;
        }
    }

    /**
     * Helper
     */

    private static Level makeLevel(String str) {
        Logger logger = LOGGER_makeLevel;
        String levelStr = str.toLowerCase().replaceAll("\\s", "");
        logger.info("Transformed level string is '" + levelStr + "'");
        return LEVEL_MAPPING.get(levelStr);
    }

    /**
     * Helper
     */
    
    private static String makeNullIfEmpty(String x) {
        if (x == null) {
            return x;
        }
        else {
            x = x.trim();
            if (x.isEmpty()) {
                return null;
            }
            else {
                return x;
            }
        }
    }
    
    /**
     * Test via short "main"
     */

    @SuppressWarnings("unused")
    public static void main(String[] argv) {
        new LogbackStarter(InjectIntoWebmailAddressBook.class);
        try {
            Set<String> committeeEmails = new HashSet();
            String fqResourceName = ResourceHelp_Java.fullyQualifyResourceName(Hook.class, "Membres_actuels.csv");
            Set<ClubMember> allMembers = (new MemberListSlurper(fqResourceName, committeeEmails)).members;
        } catch (Exception exe) {
            throw new IllegalStateException(exe);
        }
    }
    
    
}
