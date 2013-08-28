P&T Luxembourg Webmail Addressbook Injection
=============================================

Not of general interest: Inject data into the P&amp;T Luxembourg webmail address book via its web
interface using Selenium WebDriver.

This code fulfills two needs:

   * Getting acquainted with Selenium WebDriver.
     (see in particular [Selenium WebDriver by Simon Stewart](http://aosabook.org/en/selenium.html) for an introduction)
   * Getting our Sports Club address list, currently in an Excel sheet, into the P&T Webmail (at one time
     I will have to write a Grails app, fer sure).

The P&T Luxembourg webmail interface unfortunately is a cow to drive via Selenium. The DOM objects have no ids and are
hard to locate via XPath or via text search, it is unsure in what state the application is at any moment (unless 
one looks of course) and random delays in page rebuilds and Ajax refreshes may happen. Maybe it's just me though. As a
consequence, this code is brittle: it works in the current instantiation but only with certain probability
and only if one adds judicious calls to `sleep()`

Found to be of extreme use in this endeavor:

   * The Firefox Web Developer tools
   * The Firefox [Firebug plugin](https://addons.mozilla.org/de/firefox/addon/firebug/).

![The Webmail interface that we want to control](images/WebmailSnapshot.png "The Webmail interface that we want to control")



