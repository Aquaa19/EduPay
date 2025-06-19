package com.aquaa.edupay.utils;

import android.content.Context;
import android.util.Log;

import com.aquaa.edupay.database.DatabaseHelper;
import com.aquaa.edupay.models.Student;
import com.aquaa.edupay.utils.DateUtils;

import java.util.Calendar;
import java.util.Locale;

public class PromotionManager {

    private DatabaseHelper dbHelper;

    public PromotionManager(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    /**
     * Promotes a student to the next semester.
     * If the student is already in Semester 6, their status will be changed to "graduated".
     *
     * @param studentId The ID of the student to promote (or graduate if in Sem 6).
     * @param promotionMonth The month of promotion (0-11).
     * @param promotionYear The year of promotion.
     * @return True if promotion/graduation was successful, false otherwise.
     */
    public boolean promoteStudent(int studentId, int promotionMonth, int promotionYear) {
        Student student = dbHelper.getStudentById(studentId);
        if (student == null) {
            Log.e("PromotionManager", "Student not found with ID: " + studentId);
            return false;
        }

        int currentSemester = student.getCurrentSemester();

        // If student is in Semester 6, change their status to "graduated"
        if (currentSemester == 6) {
            student.setStatus("graduated"); // Set status to graduated
            int rowsAffected = dbHelper.updateStudent(student); // Update the student's record
            if (rowsAffected > 0) {
                Log.d("PromotionManager", "Student " + student.getName() + " (ID: " + studentId + ") completed Semester 6 and was marked as graduated.");
                return true; // Indicate success (graduation is the "promotion" for Sem 6)
            } else {
                Log.e("PromotionManager", "Failed to update student " + student.getName() + " (ID: " + studentId + ") to 'graduated' status after Semester 6 completion.");
                return false;
            }
        }

        // For semesters 1 through 5, promote to the next semester
        int nextSemester = currentSemester + 1;

        // Ensure next semester is within valid range (1-6) before proceeding with update
        if (nextSemester <= 6) {
            student.setCurrentSemester(nextSemester);

            // Construct the promotion date string for the 1st of the selected month and year
            String promotionDateStr = promotionYear + "-" +
                    String.format(Locale.getDefault(), "%02d", promotionMonth + 1) +
                    "-01";

            String formattedPromotionDate = DateUtils.formatDate(DateUtils.parseDateString(promotionDateStr));

            student.addPromotionEntry(nextSemester, formattedPromotionDate);

            int rowsAffected = dbHelper.updateStudent(student);
            if (rowsAffected > 0) {
                Log.d("PromotionManager", "Student " + student.getName() + " promoted to Semester " + nextSemester + " on " + formattedPromotionDate);
                return true;
            } else {
                Log.e("PromotionManager", "Failed to update student record for promotion.");
                return false;
            }
        } else {
            // This condition should ideally not be hit if currentSemester == 6 is handled above
            Log.e("PromotionManager", "Attempted to promote student " + student.getName() + " to an invalid semester: " + nextSemester);
            return false;
        }
    }
}
