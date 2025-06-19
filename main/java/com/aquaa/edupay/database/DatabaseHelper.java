package com.aquaa.edupay.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.aquaa.edupay.models.FeePayment;
import com.aquaa.edupay.models.Student;
import com.aquaa.edupay.utils.DateUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "EduPay.db";
    // IMPORTANT: Increment DATABASE_VERSION when you make schema changes (like adding a column)
    private static final int DATABASE_VERSION = 2; // Incremented from 1 to 2

    // Students Table
    public static final String TABLE_STUDENTS = "students";
    public static final String COL_STUDENT_ID = "student_id";
    public static final String COL_STUDENT_NAME = "name";
    public static final String COL_STUDENT_GENDER = "gender";
    public static final String COL_STUDENT_MOBILE = "mobile_number";
    public static final String COL_STUDENT_GUARDIAN_MOBILE = "guardian_mobile_number";
    public static final String COL_STUDENT_CURRENT_SEMESTER = "current_semester";
    public static final String COL_STUDENT_PROMOTION_HISTORY = "promotion_history"; // JSON array
    public static final String COL_STUDENT_MONTH_OF_JOINING = "month_of_joining";
    public static final String COL_STUDENT_YEAR_OF_JOINING = "year_of_joining";
    public static final String COL_STUDENT_STATUS = "status"; // NEW COLUMN: "active" or "graduated"

    // Fee Payments Table
    public static final String TABLE_FEE_PAYMENTS = "fee_payments";
    public static final String COL_PAYMENT_ID = "payment_id";
    public static final String COL_PAYMENT_STUDENT_ID = "student_id"; // FK
    public static final String COL_PAYMENT_AMOUNT = "amount";
    public static final String COL_PAYMENT_DATE = "payment_date"; // YYYY-MM-DD
    public static final String COL_PAYMENT_MONTH_YEAR = "month_year"; // e.g., "Jun-2025"
    public static final String COL_PAYMENT_SEMESTER_WHEN_PAID = "semester_when_paid"; // 1-6
    public static final String COL_PAYMENT_STATUS = "status"; // "paid" / "unpaid"

    // SQL statement to create students table
    private static final String CREATE_TABLE_STUDENTS = "CREATE TABLE " + TABLE_STUDENTS + "("
            + COL_STUDENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COL_STUDENT_NAME + " TEXT NOT NULL,"
            + COL_STUDENT_GENDER + " TEXT,"
            + COL_STUDENT_MOBILE + " TEXT,"
            + COL_STUDENT_GUARDIAN_MOBILE + " TEXT,"
            + COL_STUDENT_CURRENT_SEMESTER + " INTEGER,"
            + COL_STUDENT_MONTH_OF_JOINING + " TEXT,"
            + COL_STUDENT_YEAR_OF_JOINING + " INTEGER,"
            + COL_STUDENT_PROMOTION_HISTORY + " TEXT," // Stored as JSON string
            + COL_STUDENT_STATUS + " TEXT DEFAULT 'active'" // NEW COLUMN: Default to 'active'
            + ")";

    // SQL statement to create fee payments table
    private static final String CREATE_TABLE_FEE_PAYMENTS = "CREATE TABLE " + TABLE_FEE_PAYMENTS + "("
            + COL_PAYMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COL_PAYMENT_STUDENT_ID + " INTEGER,"
            + COL_PAYMENT_AMOUNT + " REAL,"
            + COL_PAYMENT_DATE + " TEXT,"
            + COL_PAYMENT_MONTH_YEAR + " TEXT,"
            + COL_PAYMENT_SEMESTER_WHEN_PAID + " INTEGER,"
            + COL_PAYMENT_STATUS + " TEXT,"
            + "FOREIGN KEY(" + COL_PAYMENT_STUDENT_ID + ") REFERENCES " + TABLE_STUDENTS + "(" + COL_STUDENT_ID + ")"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_STUDENTS);
        db.execSQL(CREATE_TABLE_FEE_PAYMENTS);
        Log.d("DatabaseHelper", "Tables created successfully.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database schema upgrades here
        if (oldVersion < 2) {
            // Adding COL_STUDENT_STATUS in version 2
            // Add column with a default value to existing table
            db.execSQL("ALTER TABLE " + TABLE_STUDENTS + " ADD COLUMN " + COL_STUDENT_STATUS + " TEXT DEFAULT 'active'");
            Log.d("DatabaseHelper", "Added " + COL_STUDENT_STATUS + " column to " + TABLE_STUDENTS);

            // If there's existing data, you might want to explicitly set their status if it wasn't 'active' by default
            // For this case, 'DEFAULT 'active'' handles existing rows by setting their status to 'active'
            // if they were added before this column existed.
        }
        // If you have further versions, add more if (oldVersion < X) blocks
        // For simplicity, we are not dropping tables for this specific column addition.
        // If you were to add more complex changes that required data migration, you would
        // typically follow a different pattern or drop/recreate tables if data loss is acceptable.
    }


    /**
     * Adds a new student to the database.
     * @param student The Student object to add.
     * @return The ID of the newly inserted row, or -1 if an error occurred.
     */
    public long addStudent(Student student) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_STUDENT_NAME, student.getName());
        values.put(COL_STUDENT_GENDER, student.getGender());
        values.put(COL_STUDENT_MOBILE, student.getMobileNumber());
        values.put(COL_STUDENT_GUARDIAN_MOBILE, student.getGuardianMobileNumber());
        values.put(COL_STUDENT_CURRENT_SEMESTER, student.getCurrentSemester());
        values.put(COL_STUDENT_MONTH_OF_JOINING, student.getMonthOfJoining());
        values.put(COL_STUDENT_YEAR_OF_JOINING, student.getYearOfJoining());
        values.put(COL_STUDENT_PROMOTION_HISTORY, student.getPromotionHistoryJson());
        values.put(COL_STUDENT_STATUS, student.getStatus()); // NEW: Add status

        long studentId = db.insert(TABLE_STUDENTS, null, values);
        db.close();
        Log.d("DatabaseHelper", "Student added: " + student.getName() + " ID: " + studentId);
        return studentId;
    }

    /**
     * Retrieves a student by their ID.
     * @param studentId The ID of the student to retrieve.
     * @return The Student object, or null if not found.
     */
    public Student getStudentById(int studentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_STUDENTS, null, COL_STUDENT_ID + "=?",
                new String[]{String.valueOf(studentId)}, null, null, null);

        Student student = null;
        if (cursor != null && cursor.moveToFirst()) {
            student = new Student(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_STUDENT_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_STUDENT_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_STUDENT_GENDER)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_STUDENT_MOBILE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_STUDENT_GUARDIAN_MOBILE)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_STUDENT_CURRENT_SEMESTER)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_STUDENT_MONTH_OF_JOINING)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_STUDENT_YEAR_OF_JOINING)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_STUDENT_PROMOTION_HISTORY)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_STUDENT_STATUS)) // NEW: Get status
            );
            cursor.close();
        }
        db.close();
        return student;
    }

    /**
     * Retrieves all students from the database.
     * @return A list of all Student objects.
     */
    public List<Student> getAllStudents() {
        List<Student> studentList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_STUDENTS;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Student student = new Student(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_STUDENT_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_STUDENT_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_STUDENT_GENDER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_STUDENT_MOBILE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_STUDENT_GUARDIAN_MOBILE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_STUDENT_CURRENT_SEMESTER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_STUDENT_MONTH_OF_JOINING)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_STUDENT_YEAR_OF_JOINING)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_STUDENT_PROMOTION_HISTORY)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_STUDENT_STATUS)) // NEW: Get status
                );
                studentList.add(student);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return studentList;
    }

    /**
     * Updates an existing student's information.
     * @param student The Student object with updated information.
     * @return The number of rows affected.
     */
    public int updateStudent(Student student) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_STUDENT_NAME, student.getName());
        values.put(COL_STUDENT_GENDER, student.getGender());
        values.put(COL_STUDENT_MOBILE, student.getMobileNumber());
        values.put(COL_STUDENT_GUARDIAN_MOBILE, student.getGuardianMobileNumber());
        values.put(COL_STUDENT_CURRENT_SEMESTER, student.getCurrentSemester());
        values.put(COL_STUDENT_MONTH_OF_JOINING, student.getMonthOfJoining());
        values.put(COL_STUDENT_YEAR_OF_JOINING, student.getYearOfJoining());
        values.put(COL_STUDENT_PROMOTION_HISTORY, student.getPromotionHistoryJson());
        values.put(COL_STUDENT_STATUS, student.getStatus()); // NEW: Update status

        int rowsAffected = db.update(TABLE_STUDENTS, values, COL_STUDENT_ID + " = ?",
                new String[]{String.valueOf(student.getStudentId())});
        db.close();
        Log.d("DatabaseHelper", "Student updated: " + student.getName() + " Rows affected: " + rowsAffected);
        return rowsAffected;
    }

    /**
     * Deletes a student from the database.
     * @param studentId The ID of the student to delete.
     * @return The number of rows affected.
     */
    public int deleteStudent(int studentId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Also delete associated fee payments
        db.delete(TABLE_FEE_PAYMENTS, COL_PAYMENT_STUDENT_ID + " = ?",
                new String[]{String.valueOf(studentId)});
        int rowsAffected = db.delete(TABLE_STUDENTS, COL_STUDENT_ID + " = ?",
                new String[]{String.valueOf(studentId)});
        db.close();
        Log.d("DatabaseHelper", "Student deleted ID: " + studentId + " Rows affected: " + rowsAffected);
        return rowsAffected;
    }

    /**
     * Adds a new fee payment record.
     * @param feePayment The FeePayment object to add.
     * @return The ID of the newly inserted row, or -1 if an error occurred.
     */
    public long addFeePayment(FeePayment feePayment) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PAYMENT_STUDENT_ID, feePayment.getStudentId());
        values.put(COL_PAYMENT_AMOUNT, feePayment.getAmount());
        values.put(COL_PAYMENT_DATE, feePayment.getPaymentDate());
        values.put(COL_PAYMENT_MONTH_YEAR, feePayment.getMonthYear());
        values.put(COL_PAYMENT_SEMESTER_WHEN_PAID, feePayment.getSemesterWhenPaid());
        values.put(COL_PAYMENT_STATUS, feePayment.getStatus());

        long paymentId = db.insert(TABLE_FEE_PAYMENTS, null, values);
        db.close();
        Log.d("DatabaseHelper", "Fee payment added for student ID: " + feePayment.getStudentId() + " ID: " + paymentId);
        return paymentId;
    }

    /**
     * Retrieves fee payments for a specific student.
     * @param studentId The ID of the student.
     * @return A list of FeePayment objects for the student.
     */
    public List<FeePayment> getFeePaymentsForStudent(int studentId) {
        List<FeePayment> paymentList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_FEE_PAYMENTS, null, COL_PAYMENT_STUDENT_ID + "=?",
                new String[]{String.valueOf(studentId)}, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                FeePayment payment = new FeePayment(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_PAYMENT_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_PAYMENT_STUDENT_ID)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PAYMENT_AMOUNT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PAYMENT_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PAYMENT_MONTH_YEAR)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_PAYMENT_SEMESTER_WHEN_PAID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PAYMENT_STATUS))
                );
                paymentList.add(payment);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return paymentList;
    }

    /**
     * Retrieves a specific fee payment record by student ID, month-year, and semester.
     * This is useful to check if a payment exists for a given period.
     * @param studentId The ID of the student.
     * @param monthYear The month-year string (e.g., "Jun-2025").
     * @param semester The semester (1-6).
     * @return The FeePayment object, or null if not found.
     */
    public FeePayment getFeePayment(int studentId, String monthYear, int semester) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_FEE_PAYMENTS, null,
                COL_PAYMENT_STUDENT_ID + "=? AND " + COL_PAYMENT_MONTH_YEAR + "=? AND " + COL_PAYMENT_SEMESTER_WHEN_PAID + "=?",
                new String[]{String.valueOf(studentId), monthYear, String.valueOf(semester)},
                null, null, null);

        FeePayment payment = null;
        if (cursor != null && cursor.moveToFirst()) {
            payment = new FeePayment(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_PAYMENT_ID)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_PAYMENT_STUDENT_ID)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PAYMENT_AMOUNT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PAYMENT_DATE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PAYMENT_MONTH_YEAR)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_PAYMENT_SEMESTER_WHEN_PAID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PAYMENT_STATUS))
            );
            cursor.close();
        }
        db.close();
        return payment;
    }

    /**
     * Updates the status of an existing fee payment or inserts a new one if it doesn't exist.
     * This method ensures that for a given student, month-year, and semester, there's only one payment record.
     * @param studentId The ID of the student.
     * @param monthYear The month-year string (e.g., "Jun-2025").
     * @param semester The semester (1-6).
     * @param newStatus The new status ("paid" or "unpaid").
     * @param amount The amount (can be 0 if just marking status).
     * @param paymentDate The date of payment (YYYY-MM-DD), current date if marking unpaid to paid.
     * @return The number of rows affected (1 for update, 1 for insert), or -1 on error.
     */
    public long updateOrInsertFeePaymentStatus(int studentId, String monthYear, int semester, String newStatus, double amount, String paymentDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PAYMENT_STATUS, newStatus);
        values.put(COL_PAYMENT_AMOUNT, amount);
        values.put(COL_PAYMENT_DATE, paymentDate); // Update date if status changes to paid

        // Check if a record already exists for this student, month-year, and semester
        Cursor cursor = db.query(TABLE_FEE_PAYMENTS,
                new String[]{COL_PAYMENT_ID},
                COL_PAYMENT_STUDENT_ID + "=? AND " + COL_PAYMENT_MONTH_YEAR + "=? AND " + COL_PAYMENT_SEMESTER_WHEN_PAID + "=?",
                new String[]{String.valueOf(studentId), monthYear, String.valueOf(semester)},
                null, null, null);

        long result;
        if (cursor != null && cursor.moveToFirst()) {
            // Record exists, update it
            int paymentId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PAYMENT_ID));
            result = db.update(TABLE_FEE_PAYMENTS, values, COL_PAYMENT_ID + " = ?", new String[]{String.valueOf(paymentId)});
            Log.d("DatabaseHelper", "Updated fee payment for student ID: " + studentId + ", MonthYear: " + monthYear + ", Semester: " + semester + ", Status: " + newStatus);
        } else {
            // Record does not exist, insert a new one
            values.put(COL_PAYMENT_STUDENT_ID, studentId);
            values.put(COL_PAYMENT_MONTH_YEAR, monthYear);
            values.put(COL_PAYMENT_SEMESTER_WHEN_PAID, semester);
            result = db.insert(TABLE_FEE_PAYMENTS, null, values);
            Log.d("DatabaseHelper", "Inserted new fee payment for student ID: " + studentId + ", MonthYear: " + monthYear + ", Semester: " + semester + ", Status: " + newStatus);
        }
        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return result;
    }


    /**
     * Retrieves fee payments for a specific student, year, month, and semester.
     * @param studentId The ID of the student.
     * @param year The year (e.g., 2025).
     * @param month The month index (0-11).
     * @param semester The semester (1-6).
     * @return A list of FeePayment objects matching the criteria.
     */
    public List<FeePayment> getFeePaymentsForStudentAndPeriod(int studentId, int year, int month, int semester) {
        List<FeePayment> paymentList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String monthYear = DateUtils.getMonthYearString(month, year);

        String selection = COL_PAYMENT_STUDENT_ID + "=? AND " + COL_PAYMENT_MONTH_YEAR + "=? AND " + COL_PAYMENT_SEMESTER_WHEN_PAID + "=?";
        String[] selectionArgs = new String[]{String.valueOf(studentId), monthYear, String.valueOf(semester)};

        Cursor cursor = db.query(TABLE_FEE_PAYMENTS, null, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                FeePayment payment = new FeePayment(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_PAYMENT_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_PAYMENT_STUDENT_ID)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PAYMENT_AMOUNT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PAYMENT_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PAYMENT_MONTH_YEAR)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_PAYMENT_SEMESTER_WHEN_PAID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PAYMENT_STATUS))
                );
                paymentList.add(payment);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return paymentList;
    }

    /**
     * Retrieves fee payments for a specific year, month, and semester, with an optional status filter.
     * @param year The year (e.g., 2025).
     * @param month The month index (0-11).
     * @param semester The semester (1-6).
     * @param statusFilter Optional status filter ("paid", "unpaid", or null for all).
     * @return A list of FeePayment objects matching the criteria.
     */
    public List<FeePayment> getFeePaymentsByPeriodAndStatus(int year, int month, int semester, String statusFilter) {
        List<FeePayment> paymentList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String monthYear = DateUtils.getMonthYearString(month, year);

        String selection = COL_PAYMENT_MONTH_YEAR + "=? AND " + COL_PAYMENT_SEMESTER_WHEN_PAID + "=?";
        List<String> selectionArgs = new ArrayList<>();
        selectionArgs.add(monthYear);
        selectionArgs.add(String.valueOf(semester));

        if (statusFilter != null && !statusFilter.isEmpty() && !statusFilter.equalsIgnoreCase("all")) {
            selection += " AND " + COL_PAYMENT_STATUS + "=?";
            selectionArgs.add(statusFilter);
        }

        Cursor cursor = db.query(TABLE_FEE_PAYMENTS, null, selection, selectionArgs.toArray(new String[0]), null, null, null);

        if (cursor.moveToFirst()) {
            do {
                FeePayment payment = new FeePayment(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_PAYMENT_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_PAYMENT_STUDENT_ID)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PAYMENT_AMOUNT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PAYMENT_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PAYMENT_MONTH_YEAR)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_PAYMENT_SEMESTER_WHEN_PAID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PAYMENT_STATUS))
                );
                paymentList.add(payment);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return paymentList;
    }

    /**
     * Retrieves all fee payments for a given student in a specific year.
     * @param studentId The ID of the student.
     * @param year The year to retrieve payments for.
     * @return A list of FeePayment objects.
     */
    public List<FeePayment> getFeePaymentsForStudentInYear(int studentId, int year) {
        List<FeePayment> paymentList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Query for payments where month_year contains the specified year
        String selection = COL_PAYMENT_STUDENT_ID + "=? AND " + COL_PAYMENT_MONTH_YEAR + " LIKE ?";
        String[] selectionArgs = new String[]{String.valueOf(studentId), "%-" + year};

        Cursor cursor = db.query(TABLE_FEE_PAYMENTS, null, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                FeePayment payment = new FeePayment(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_PAYMENT_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_PAYMENT_STUDENT_ID)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PAYMENT_AMOUNT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PAYMENT_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PAYMENT_MONTH_YEAR)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_PAYMENT_SEMESTER_WHEN_PAID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PAYMENT_STATUS))
                );
                paymentList.add(payment);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return paymentList;
    }

    /**
     * Deletes all fee payments for a student for a specific year.
     * This is used during the annual transition to "archive" old data by deleting it.
     * @param studentId The ID of the student.
     * @param year The year to delete payments for.
     * @return The number of rows deleted.
     */
    public int deleteFeePaymentsForStudentInYear(int studentId, int year) {
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = COL_PAYMENT_STUDENT_ID + "=? AND " + COL_PAYMENT_MONTH_YEAR + " LIKE ?";
        String[] whereArgs = new String[]{String.valueOf(studentId), "%-" + year};
        int rowsAffected = db.delete(TABLE_FEE_PAYMENTS, whereClause, whereArgs);
        db.close();
        Log.d("DatabaseHelper", "Deleted " + rowsAffected + " fee payments for student " + studentId + " in year " + year);
        return rowsAffected;
    }

    /**
     * Checks if a fee payment exists for a given student, month-year, and semester.
     * @param studentId The ID of the student.
     * @param monthYear The month-year string (e.g., "Jun-2025").
     * @param semester The semester (1-6).
     * @return True if a payment exists, false otherwise.
     */
    public boolean doesFeePaymentExist(int studentId, String monthYear, int semester) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT 1 FROM " + TABLE_FEE_PAYMENTS +
                " WHERE " + COL_PAYMENT_STUDENT_ID + " = ? AND " +
                COL_PAYMENT_MONTH_YEAR + " = ? AND " +
                COL_PAYMENT_SEMESTER_WHEN_PAID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(studentId), monthYear, String.valueOf(semester)});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    /**
     * Retrieves the active semester for a student at a given month and year,
     * considering their promotion history.
     *
     * @param studentId The ID of the student.
     * @param targetYear The target year.
     * @param targetMonth The target month (0-indexed, Calendar.JANUARY to Calendar.DECEMBER).
     * @return The active semester for the student at the beginning of the specified month,
     * or 1 if no promotions occurred before or during that month.
     */
    public int getActiveSemesterForStudent(int studentId, int targetYear, int targetMonth) {
        Student student = getStudentById(studentId);
        if (student == null) {
            return 1; // Default to semester 1 if student not found
        }

        int joiningMonthIndex = DateUtils.getMonthIndex(student.getMonthOfJoining());
        if (targetYear < student.getYearOfJoining() || (targetYear == student.getYearOfJoining() && targetMonth < joiningMonthIndex)) {
            return 1;
        }


        int semesterAtTargetDate = 1;

        List<Map<String, String>> promotionHistory = student.getPromotionHistory();

        Collections.sort(promotionHistory, new Comparator<Map<String, String>>() {
            @Override
            public int compare(Map<String, String> m1, Map<String, String> m2) {
                String date1 = m1.get("date");
                String date2 = m2.get("date");
                return date1.compareTo(date2);
            }
        });

        for (Map<String, String> entry : promotionHistory) {
            try {
                int promotedSemester = Integer.parseInt(entry.get("semester"));
                String promotionDateStr = entry.get("date");

                Calendar promotionCal = DateUtils.parseDateString(promotionDateStr);
                if (promotionCal == null) {
                    Log.e("DatabaseHelper", "Invalid promotion date string: " + promotionDateStr);
                    continue;
                }

                Calendar targetCal = Calendar.getInstance();
                targetCal.set(targetYear, targetMonth, 1, 0, 0, 0);

                if (!promotionCal.after(targetCal)) {
                    semesterAtTargetDate = promotedSemester;
                } else {
                    break;
                }
            } catch (NumberFormatException | NullPointerException e) {
                Log.e("DatabaseHelper", "Error parsing promotion history entry: " + e.getMessage());
            }
        }
        return semesterAtTargetDate;
    }
}
