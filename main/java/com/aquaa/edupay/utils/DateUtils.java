package com.aquaa.edupay.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat MONTH_YEAR_FORMAT = new SimpleDateFormat("MMM-yyyy", Locale.getDefault());
    private static final SimpleDateFormat MONTH_FULL_FORMAT = new SimpleDateFormat("MMMM", Locale.getDefault());

    /**
     * Gets the current date in YYYY-MM-DD format.
     * @return Current date string.
     */
    public static String getCurrentDate() {
        return DATE_FORMAT.format(new Date());
    }

    /**
     * Gets the current month in 0-indexed format (0 for January, 11 for December).
     * @return Current month index.
     */
    public static int getCurrentMonth() {
        return Calendar.getInstance().get(Calendar.MONTH);
    }

    /**
     * Gets the current year.
     * @return Current year.
     */
    public static int getCurrentYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    /**
     * Gets the month name from a 0-indexed month.
     * @param monthIndex 0-11
     * @return Full month name (e.g., "January").
     */
    public static String getMonthName(int monthIndex) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, monthIndex);
        return MONTH_FULL_FORMAT.format(cal.getTime());
    }

    /**
     * Gets the month index from a month name.
     * @param monthName Full month name (e.g., "January").
     * @return 0-indexed month (0 for January), or -1 if not found.
     */
    public static int getMonthIndex(String monthName) {
        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        for (int i = 0; i < months.length; i++) {
            if (months[i].equalsIgnoreCase(monthName)) {
                return i;
            }
        }
        return -1; // Not found
    }

    /**
     * Gets the month-year string (e.g., "Jun-2025") from month index and year.
     * @param monthIndex 0-11
     * @param year E.g., 2025
     * @return Formatted month-year string.
     */
    public static String getMonthYearString(int monthIndex, int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, monthIndex);
        return MONTH_YEAR_FORMAT.format(cal.getTime());
    }

    /**
     * Parses a month-year string (e.g., "Jun-2025") into a Calendar object.
     * @param monthYearString The string to parse.
     * @return Calendar object, or null if parsing fails.
     */
    public static Calendar parseMonthYearString(String monthYearString) {
        try {
            Date date = MONTH_YEAR_FORMAT.parse(monthYearString);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return cal;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Checks if a given month and year is in the future compared to the current date.
     * @param targetMonthIndex 0-11
     * @param targetYear
     * @return True if target date is in the future, false otherwise.
     */
    public static boolean isFutureMonth(int targetMonthIndex, int targetYear) {
        Calendar currentCal = Calendar.getInstance();
        int currentYear = currentCal.get(Calendar.YEAR);
        int currentMonth = currentCal.get(Calendar.MONTH);

        if (targetYear > currentYear) {
            return true;
        } else if (targetYear == currentYear) {
            return targetMonthIndex > currentMonth;
        }
        return false;
    }

    /**
     * Checks if a given date (month, year) is before another date.
     * @param month1 0-11
     * @param year1
     * @param month2 0-11
     * @param year2
     * @return True if (month1, year1) is before (month2, year2).
     */
    public static boolean isBefore(int month1, int year1, int month2, int year2) {
        if (year1 < year2) {
            return true;
        } else if (year1 == year2) {
            return month1 < month2;
        }
        return false;
    }

    /**
     * Checks if a given date (month, year) is equal to or after another date.
     * @param month1 0-11
     * @param year1
     * @param month2 0-11
     * @param year2
     * @return True if (month1, year1) is equal to or after (month2, year2).
     */
    public static boolean isSameOrAfter(int month1, int year1, int month2, int year2) {
        if (year1 > year2) {
            return true;
        } else if (year1 == year2) {
            return month1 >= month2;
        }
        return false;
    }

    /**
     * Parses a date string (YYYY-MM-DD) into a Calendar object.
     * @param dateString The date string.
     * @return Calendar object, or null if parsing fails.
     */
    public static Calendar parseDateString(String dateString) {
        try {
            Date date = DATE_FORMAT.parse(dateString);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return cal;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Formats a Calendar object into a YYYY-MM-DD date string.
     * @param calendar The Calendar object.
     * @return Formatted date string.
     */
    public static String formatDate(Calendar calendar) {
        return DATE_FORMAT.format(calendar.getTime());
    }
}
