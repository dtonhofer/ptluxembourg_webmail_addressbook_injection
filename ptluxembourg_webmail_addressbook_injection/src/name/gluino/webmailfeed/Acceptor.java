package name.gluino.webmailfeed;

/* 34567890123456789012345678901234567890123456789012345678901234567890123456789
 * *****************************************************************************
 * Copyright (c) 2013, David Tonhofer
 *
 * Distributed under: "The MIT License" (http://opensource.org/licenses/MIT)
 *******************************************************************************
 * Something which implements a boolean value on the set of Club Members,
 * Additionally, returns a string that can be written to the "Company field",
 * which we misuse for remarks & notices.
 *  
 * 2013.04.XX - Created
 * 2013.06.14 - Cleanup
 ******************************************************************************/

interface Acceptor {

    public boolean accept(ClubMember x);

    public String getCompany(ClubMember x);

}