package com.aquaa.edupay.models;

public class FeePayment {
    private int paymentId;
    private int studentId;
    private double amount;
    private String paymentDate; // YYYY-MM-DD
    private String monthYear;   // e.g., "Jun-2025"
    private int semesterWhenPaid; // 1-6
    private String status;      // "paid" or "unpaid"

    public FeePayment() {
        // Default constructor
    }

    public FeePayment(int paymentId, int studentId, double amount, String paymentDate,
                      String monthYear, int semesterWhenPaid, String status) {
        this.paymentId = paymentId;
        this.studentId = studentId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.monthYear = monthYear;
        this.semesterWhenPaid = semesterWhenPaid;
        this.status = status;
    }

    // Getters
    public int getPaymentId() {
        return paymentId;
    }

    public int getStudentId() {
        return studentId;
    }

    public double getAmount() {
        return amount;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public String getMonthYear() {
        return monthYear;
    }

    public int getSemesterWhenPaid() {
        return semesterWhenPaid;
    }

    public String getStatus() {
        return status;
    }

    // Setters
    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }

    public void setMonthYear(String monthYear) {
        this.monthYear = monthYear;
    }

    public void setSemesterWhenPaid(int semesterWhenPaid) {
        this.semesterWhenPaid = semesterWhenPaid;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
