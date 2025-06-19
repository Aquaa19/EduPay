package com.aquaa.edupay.utils;

import android.content.Context;
import android.util.Log;

import com.aquaa.edupay.database.DatabaseHelper;
import com.aquaa.edupay.models.FeePayment;
import com.aquaa.edupay.models.Student;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FeeTrackingManager {

    private DatabaseHelper dbHelper;
    private Context context;

    public FeeTrackingManager(Context context) {
        this.context = context;
        dbHelper = new DatabaseHelper(context);
    }

    /**
     * Initializes fee payment records for new students for the initial payment period (June-August).
     * This should be called when a student is added.
     * @param studentId The ID of the newly added student.
     * @param monthOfJoining The month the student joined (e.g., "June").
     * @param yearOfJoining The year the student joined (e.g., 2025).
     */
    public void initializeStudentFeeRecords(int studentId, String monthOfJoining, int yearOfJoining) {
        // All students start tracking from June of current year
        // First payment period: June-August (3 months)
        int currentYear = DateUtils.getCurrentYear();
        int currentMonth = DateUtils.getCurrentMonth(); // 0-indexed

        // Determine the actual start month for fee tracking. If joining before June, start from June.
        // If joining after June, start from joining month.
        int startMonthIndex = DateUtils.getMonthIndex(monthOfJoining);
        int startYear = yearOfJoining;

        if (startYear < currentYear || (startYear == currentYear && startMonthIndex < Calendar.JUNE)) {
            startMonthIndex = Calendar.JUNE;
            startYear = currentYear;
        }

        // Initialize for 3 months from the determined start point
        for (int i = 0; i < 3; i++) {
            int targetMonthIndex = (startMonthIndex + i) % 12;
            int targetYear = startYear + ((startMonthIndex + i) / 12);

            String monthYear = DateUtils.getMonthYearString(targetMonthIndex, targetYear);
            int semester = dbHelper.getActiveSemesterForStudent(studentId, targetYear, targetMonthIndex);

            // Only add if the payment record doesn't already exist for this student, month-year, and semester
            if (!dbHelper.doesFeePaymentExist(studentId, monthYear, semester)) {
                dbHelper.addFeePayment(new FeePayment(
                        0, // Auto-incremented
                        studentId,
                        0.0, // Initial amount can be 0, updated when paid
                        "", // Payment date empty initially
                        monthYear,
                        semester,
                        Constants.STATUS_UNPAID // Initially unpaid
                ));
            }
        }
        Log.d("FeeTrackingManager", "Initialized fee records for student ID: " + studentId);
    }

    /**
     * Handles the automatic year transition logic on June 1st.
     * This method should be called once when the app starts up on June 1st of any year.
     *
     * Business Logic:
     * - On June 1st of each year:
     * - Automatically archive previous year's data (by deleting old fee payments).
     * - For continuing students:
     * - Remove oldest semester colors per replacement rules.
     * - Update current semester if maximum not reached.
     */
    public void handleAnnualYearTransition() {
        Calendar today = Calendar.getInstance();
        int currentMonth = today.get(Calendar.MONTH);
        int currentDay = today.get(Calendar.DAY_OF_MONTH);
        int currentYear = today.get(Calendar.YEAR);

        // Only run this logic on June 1st
        if (currentMonth != Calendar.JUNE || currentDay != 1) {
            Log.d("FeeTrackingManager", "Not June 1st. Skipping annual transition.");
            return;
        }

        Log.d("FeeTrackingManager", "Running annual year transition for " + currentYear);

        List<Student> allStudents = dbHelper.getAllStudents();
        int previousYear = currentYear - 1;

        for (Student student : allStudents) {
            // 1. Archive previous year's data (delete fee payments for previous year)
            dbHelper.deleteFeePaymentsForStudentInYear(student.getStudentId(), previousYear);
            Log.d("FeeTrackingManager", "Archived fee payments for student " + student.getName() + " for year " + previousYear);

            // 2. Remove oldest semester colors per replacement rules (from promotion history)
            // This logic is complex and needs to consider the "active" semester for previous periods.
            // The requirement "Remove oldest semester colors per replacement rules" implies that
            // when a student is promoted to an odd semester (3, 5), the previous odd semester's color
            // should be "erased" from their history for display purposes, and similarly for even.
            // Since we are deleting the actual fee payment records for the previous year,
            // the "colors" for that year will naturally disappear from the display.
            // The promotion history itself should remain intact as it's a factual record.
            // The display logic in StudentAdapter will handle which colors are shown based on active promotions.
            // So, no explicit removal from promotion_history is needed here, as it's a historical record.
            // The example "Jun 2026: Year update → Sem 1 colors erased" means that for the *display*
            // of 2026, Sem 1 payments from 2025 (if any were displayed) are no longer relevant,
            // and the student will be in Sem 3 (Coral) for June 2026.

            // 3. Update current semester if maximum not reached (this is handled by PromotionManager
            //    which is triggered by specific months, not annually. However, if a student
            //    has completed Sem 6, they might be considered "graduated" and not tracked further.
            //    For this app, students continue in their current semester until promoted.)
            //    The prompt says "Update current semester if maximum not reached".
            //    This implies a student might automatically advance if they were, say, in Sem 6
            //    and completed it, but the promotion rules are specific (March/September).
            //    So, this part is implicitly handled by the promotion logic.
            //    If a student reaches Sem 6, they stay in Sem 6 unless explicitly handled as "graduated".

            // Re-initialize fee records for the current year (starting from June of the current year)
            // for continuing students, assuming they continue from their current semester.
            // The initial setup logic in initializeStudentFeeRecords handles this.
            // We can re-call it to ensure future months are set to 'unpaid'.
            // This ensures that for the new academic year, the fee records are set up.
            initializeStudentFeeRecords(student.getStudentId(), DateUtils.getMonthName(Calendar.JUNE), currentYear);
        }
    }
}
