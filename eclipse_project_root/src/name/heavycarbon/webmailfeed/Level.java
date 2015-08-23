package name.heavycarbon.webmailfeed;

/* 34567890123456789012345678901234567890123456789012345678901234567890123456789
 * *****************************************************************************
 * Dan and Kyu levels. Each member of the club is given one of these.
 * 
 * 2013.04.XX - Created
 * 2013.06.14 - Cleanup
 ******************************************************************************/

enum Level {

    KYU_6(false), KYU_5(false), KYU_4(false), KYU_3(false), KYU_2(false), KYU_1(false), DAN_1(true), DAN_2(true), DAN_3(true), DAN_4(true), DAN_5(true), DAN_6(true), DAN_7(true);

    boolean isDan;

    Level(boolean isDan) {
        this.isDan = isDan;
    }
}