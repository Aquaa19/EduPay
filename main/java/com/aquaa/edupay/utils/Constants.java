package com.aquaa.edupay.utils;

import android.graphics.Color;

public class Constants {
    // Semester Colors (as defined in colors.xml)
    public static final int SEMESTER_COLOR_1 = Color.parseColor("#008080"); // Teal
    public static final int SEMESTER_COLOR_2 = Color.parseColor("#DAA520"); // Changed to darker Goldenrod
    public static final int SEMESTER_COLOR_3 = Color.parseColor("#FF7F50"); // Coral
    public static final int SEMESTER_COLOR_4 = Color.parseColor("#6A5ACD"); // Slate Blue
    public static final int SEMESTER_COLOR_5 = Color.parseColor("#FF00FF"); // Magenta
    public static final int SEMESTER_COLOR_6 = Color.parseColor("#98FF98"); // Mint Green

    public static int getSemesterColor(int semester) {
        switch (semester) {
            case 1: return SEMESTER_COLOR_1;
            case 2: return SEMESTER_COLOR_2;
            case 3: return SEMESTER_COLOR_3;
            case 4: return SEMESTER_COLOR_4;
            case 5: return SEMESTER_COLOR_5;
            case 6: return SEMESTER_COLOR_6;
            default: return Color.GRAY; // Default color for invalid semester
        }
    }

    public static final String STATUS_PAID = "paid";
    public static final String STATUS_UNPAID = "unpaid";
    public static final String STATUS_ALL = "all"; // For filter options (e.g., all payments, all student statuses)

    // NEW: Student Statuses
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_GRADUATED = "graduated";
}
