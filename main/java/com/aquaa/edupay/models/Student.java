package com.aquaa.edupay.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Student {
    private int studentId;
    private String name;
    private String gender;
    private String mobileNumber;
    private String guardianMobileNumber;
    private int currentSemester;
    private String monthOfJoining; // e.g., "June"
    private int yearOfJoining; // e.g., 2025
    private List<Map<String, String>> promotionHistory; // Stores {"semester": "1", "date": "YYYY-MM-DD"}
    private String status; // NEW FIELD: "active" or "graduated"

    public Student() {
        // Default constructor
        this.promotionHistory = new ArrayList<>();
        this.status = "active"; // Default status for new students
    }

    public Student(int studentId, String name, String gender, String mobileNumber, String guardianMobileNumber,
                   int currentSemester, String monthOfJoining, int yearOfJoining, String promotionHistoryJson) {
        this(studentId, name, gender, mobileNumber, guardianMobileNumber, currentSemester,
                monthOfJoining, yearOfJoining, promotionHistoryJson, "active"); // Default status for old constructor
    }

    // New constructor with status
    public Student(int studentId, String name, String gender, String mobileNumber, String guardianMobileNumber,
                   int currentSemester, String monthOfJoining, int yearOfJoining, String promotionHistoryJson, String status) {
        this.studentId = studentId;
        this.name = name;
        this.gender = gender;
        this.mobileNumber = mobileNumber;
        this.guardianMobileNumber = guardianMobileNumber;
        this.currentSemester = currentSemester;
        this.monthOfJoining = monthOfJoining;
        this.yearOfJoining = yearOfJoining;
        setPromotionHistoryFromJson(promotionHistoryJson);
        this.status = status;
    }

    // Getters
    public int getStudentId() {
        return studentId;
    }

    public String getName() {
        return name;
    }

    public String getGender() {
        return gender;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getGuardianMobileNumber() {
        return guardianMobileNumber;
    }

    public int getCurrentSemester() {
        return currentSemester;
    }

    public String getMonthOfJoining() {
        return monthOfJoining;
    }

    public int getYearOfJoining() {
        return yearOfJoining;
    }

    public List<Map<String, String>> getPromotionHistory() {
        return promotionHistory;
    }

    public String getStatus() { // NEW Getter
        return status;
    }

    // Setters
    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public void setGuardianMobileNumber(String guardianMobileNumber) {
        this.guardianMobileNumber = guardianMobileNumber;
    }

    public void setCurrentSemester(int currentSemester) {
        this.currentSemester = currentSemester;
    }

    public void setMonthOfJoining(String monthOfJoining) {
        this.monthOfJoining = monthOfJoining;
    }

    public void setYearOfJoining(int yearOfJoining) {
        this.yearOfJoining = yearOfJoining;
    }

    public void setPromotionHistory(List<Map<String, String>> promotionHistory) {
        this.promotionHistory = promotionHistory;
    }

    public void setStatus(String status) { // NEW Setter
        this.status = status;
    }

    /**
     * Converts the List of promotion history maps to a JSON string.
     * @return JSON string representation of promotion history.
     */
    public String getPromotionHistoryJson() {
        JSONArray jsonArray = new JSONArray();
        for (Map<String, String> entry : promotionHistory) {
            JSONObject jsonObject = new JSONObject(entry);
            jsonArray.put(jsonObject);
        }
        return jsonArray.toString();
    }

    /**
     * Parses a JSON string into the promotion history list.
     * @param jsonString JSON string of promotion history.
     */
    public void setPromotionHistoryFromJson(String jsonString) {
        this.promotionHistory = new ArrayList<>();
        if (jsonString != null && !jsonString.isEmpty() && !jsonString.equals("null")) {
            try {
                JSONArray jsonArray = new JSONArray(jsonString);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    Map<String, String> entry = new HashMap<>();
                    if (jsonObject.has("semester")) {
                        entry.put("semester", jsonObject.getString("semester"));
                    }
                    if (jsonObject.has("date")) {
                        entry.put("date", jsonObject.getString("date"));
                    }
                    promotionHistory.add(entry);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Adds a new promotion entry to the history.
     * @param semester The semester promoted to.
     * @param date The date of promotion (YYYY-MM-DD).
     */
    public void addPromotionEntry(int semester, String date) {
        Map<String, String> entry = new HashMap<>();
        entry.put("semester", String.valueOf(semester));
        entry.put("date", date);
        this.promotionHistory.add(entry);
    }
}
