package name.heavycarbon.webmailfeed;

import static name.heavycarbon.checks.BasicChecks.*;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import name.heavycarbon.utils.MailAddressAcceptor;

/* 34567890123456789012345678901234567890123456789012345678901234567890123456789
 * *****************************************************************************
 * Representation of a Club Member
 *  
 * 2013.04.XX - Created
 * 2013.06.14 - Cleanup
 ******************************************************************************/

class ClubMember {

    public final String firstName; // not null
    public final String lastName; // not null
    public final List<String> emailAddressList; // not null and not empty
    public final DateTime birthday; // may be null!
    public final Level level; // may be null
    public final boolean isComite;

    public ClubMember(String firstName, String lastName, List<String> emailAddressList, DateTime birthday, Level level, boolean isComite) {
        checkNotNull(firstName, "first name");
        checkNotNull(lastName, "last name");
        checkNotNull(emailAddressList, "email address list");
        checkFalse(emailAddressList.isEmpty(), "email address list is empty");
        // Check.notNull(birthday, "birthday");
        // Check.notNull(level, "level");
        for (String e : emailAddressList) {
            checkTrue(MailAddressAcceptor.acceptAddress(e), "The address '%s' was unacceptable", e);
        }
        this.firstName = firstName;
        this.lastName = lastName;
        this.emailAddressList = emailAddressList;
        this.birthday = birthday;
        this.level = level;
        this.isComite = isComite;
    }

    // getAge may return null if unset!
    
    public Integer getAge() {
        if (birthday != null) {
            Interval iv = new Interval(birthday, new DateTime());
            Period p = iv.toPeriod();
            return p.getYears();
        } else {
            return null;
        }
    }

    public boolean isCommittee() {
        return isComite;
    }

    public boolean isCeintureNoire() {        
        return level != null && level.isDan;
    }
    
    // We separate "adultes" and "enfants" (they pay different fees) at 14:
    
    public boolean isAdulte() {
        return getAge()!=null && 14 <= getAge();
    }

    // We separate "adultes" and "enfants" (they pay different fees) at 14:

    public boolean isEnfant() {
        return getAge()!=null && !isAdulte();
    }

    // "enfants ou adoslecent" is set to be below the age of 19:
    
    public boolean isEnfantOuAdolescent() {
        return getAge()!=null && getAge() < 19;
    }

    @Override
    public String toString() {
        DateTimeFormatter fmttr = DateTimeFormat.forPattern("YYYY-MMMM-dd");
        StringBuilder buf = new StringBuilder();
        buf.append("First Name : " + firstName + "\n");
        buf.append("Last Name  : " + lastName + "\n");
        for (String e : emailAddressList) {
            buf.append("Email      : " + e + "\n");
        }
        if (birthday != null) {
            buf.append("Birthday   : " + fmttr.print(birthday) + "\n");
            buf.append("Age        : " + getAge() + "\n");
        } else {
            buf.append("Birthday   : ?\n");
        }
        if (level != null) {
            buf.append("Level      : " + level + "\n");
        } else {
            buf.append("Level      : ?\n");
        }
        return buf.toString();
    }
}