package com.aquaa.edupay.adapters;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView; // Import ImageView
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.aquaa.edupay.R;
import com.aquaa.edupay.database.DatabaseHelper;
import com.aquaa.edupay.models.FeePayment;
import com.aquaa.edupay.models.Student;
import com.aquaa.edupay.utils.Constants;
import com.aquaa.edupay.utils.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentAdapter extends ArrayAdapter<Student> {

    private static final String TAG = "StudentAdapter";

    private Context context;
    private List<Student> students;
    private Map<Integer, Boolean> expandedStates;
    private DatabaseHelper dbHelper;
    private int selectedYear;
    private OnStudentActionListener listener;

    public interface OnStudentActionListener {
        void onDeleteStudent(int studentId);
    }

    public void setOnStudentActionListener(OnStudentActionListener listener) {
        this.listener = listener;
    }

    public StudentAdapter(@NonNull Context context, List<Student> students, int selectedYear) {
        super(context, 0, students);
        this.context = context;
        this.students = students;
        this.selectedYear = selectedYear;
        this.expandedStates = new HashMap<>();
        this.dbHelper = new DatabaseHelper(context);
    }

    public void setSelectedYear(int year) {
        this.selectedYear = year;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Student currentStudent = students.get(position);
        boolean isExpanded = expandedStates.containsKey(currentStudent.getStudentId()) && expandedStates.get(currentStudent.getStudentId());

        View listItem;
        ViewHolder holder;

        if (convertView == null || !((boolean) convertView.getTag(R.id.tag_is_expanded) == isExpanded)) {
            if (isExpanded) {
                listItem = LayoutInflater.from(context).inflate(R.layout.item_student_profile_expanded, parent, false);
            } else {
                listItem = LayoutInflater.from(context).inflate(R.layout.item_student_profile_collapsed, parent, false);
            }
            holder = new ViewHolder();
            holder.tvStudentName = listItem.findViewById(isExpanded ? R.id.tvStudentNameExpanded : R.id.tvStudentNameCollapsed);
            holder.ivExpandCollapse = listItem.findViewById(isExpanded ? R.id.ivExpandCollapseExpanded : R.id.ivExpandCollapse);
            holder.llMonthBar = listItem.findViewById(isExpanded ? R.id.llMonthBarExpanded : R.id.llMonthBarCollapsed);

            if (isExpanded) {
                holder.tvGender = listItem.findViewById(R.id.tvGender);
                holder.tvMobile = listItem.findViewById(R.id.tvMobile);
                holder.tvGuardianMobile = listItem.findViewById(R.id.tvGuardianMobile);
                holder.btnDeleteStudent = listItem.findViewById(R.id.btnDeleteStudent);
            }
            listItem.setTag(holder);
            listItem.setTag(R.id.tag_is_expanded, isExpanded);
        } else {
            listItem = convertView;
            holder = (ViewHolder) listItem.getTag();
        }

        if (holder.tvStudentName != null) {
            holder.tvStudentName.setText(currentStudent.getName());
        } else {
            Log.e(TAG, "tvStudentName is null for student: " + currentStudent.getName());
        }


        if (isExpanded) {
            if (holder.tvGender != null) holder.tvGender.setText("Gender: " + currentStudent.getGender());
            if (holder.tvMobile != null) holder.tvMobile.setText("Mobile: " + currentStudent.getMobileNumber());
            if (holder.tvGuardianMobile != null) holder.tvGuardianMobile.setText("Guardian: " + currentStudent.getGuardianMobileNumber());

            if (holder.ivExpandCollapse != null) {
                holder.ivExpandCollapse.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                holder.ivExpandCollapse.setColorFilter(ContextCompat.getColor(context, R.color.light_on_surface));
            }

            if (holder.btnDeleteStudent != null) {
                // Adjust visibility based on student status
                if (Constants.STATUS_GRADUATED.equals(currentStudent.getStatus())) {
                    holder.btnDeleteStudent.setVisibility(View.GONE); // Hide if graduated
                } else {
                    holder.btnDeleteStudent.setVisibility(View.VISIBLE); // Show for active students
                }
                holder.btnDeleteStudent.setOnClickListener(v -> showDeleteConfirmationDialog(currentStudent));
                holder.btnDeleteStudent.setColorFilter(ContextCompat.getColor(context, R.color.light_on_surface));
            }

        } else {
            if (holder.ivExpandCollapse != null) {
                holder.ivExpandCollapse.setImageResource(android.R.drawable.ic_menu_add);
                holder.ivExpandCollapse.setColorFilter(ContextCompat.getColor(context, R.color.light_on_surface));
            }
        }

        if (holder.ivExpandCollapse != null) {
            holder.ivExpandCollapse.setOnClickListener(v -> {
                expandedStates.put(currentStudent.getStudentId(), !isExpanded);
                notifyDataSetChanged();
            });
        }

        if (holder.llMonthBar != null) {
            populateMonthBar(holder.llMonthBar, currentStudent, selectedYear, isExpanded);
        } else {
            Log.e(TAG, "llMonthBar is null for student: " + currentStudent.getName());
        }

        return listItem;
    }

    private void showDeleteConfirmationDialog(Student student) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.custom_confirmation_dialog);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        TextView dialogTitle = dialog.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialog.findViewById(R.id.dialogMessage);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnConfirm = dialog.findViewById(R.id.btnConfirm);

        if (dialogTitle != null) dialogTitle.setTextColor(ContextCompat.getColor(context, R.color.light_on_surface));
        if (dialogMessage != null) dialogMessage.setTextColor(ContextCompat.getColor(context, R.color.light_on_surface));
        if (btnCancel != null) btnCancel.setTextColor(ContextCompat.getColor(context, R.color.light_on_surface));


        if (dialogTitle != null) dialogTitle.setText("Delete Student: " + student.getName() + "?");
        if (dialogMessage != null) dialogMessage.setText("Are you sure you want to delete " + student.getName() + " and all their associated fee records? This action cannot be undone.");

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteStudent(student.getStudentId());
                }
                dialog.dismiss();
            });
        }
        dialog.show();
    }


    private void populateMonthBar(LinearLayout monthBarLayout, Student student, int year, boolean isExpanded) {
        monthBarLayout.removeAllViews();

        try {
            List<FeePayment> studentPayments = dbHelper.getFeePaymentsForStudentInYear(student.getStudentId(), year);
            Map<String, FeePayment> paymentsMap = new HashMap<>();
            for (FeePayment payment : studentPayments) {
                paymentsMap.put(payment.getMonthYear(), payment);
            }

            List<Map<String, String>> promotionHistory = student.getPromotionHistory();

            Calendar currentCal = Calendar.getInstance();
            int currentMonth = currentCal.get(Calendar.MONTH);
            int currentYear = currentCal.get(Calendar.YEAR);

            for (int i = 0; i < 12; i++) {
                TextView monthTextView = new TextView(context);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(4, 0, 4, 0);
                monthTextView.setLayoutParams(params);
                monthTextView.setPadding(8, 4, 8, 4);
                monthTextView.setTextSize(12);
                monthTextView.setTypeface(null, Typeface.BOLD);
                monthTextView.setTextColor(ContextCompat.getColor(context, R.color.white));

                String monthAbbreviation = DateUtils.getMonthName(i).substring(0, 3);
                String monthYearKey = DateUtils.getMonthYearString(i, year);

                int backgroundColor;
                String statusText = "";
                int semesterForMonth = -1;

                semesterForMonth = student.getCurrentSemester();
                for (Map<String, String> entry : promotionHistory) {
                    try {
                        int promotedSem = Integer.parseInt(entry.get("semester"));
                        String promotionDateStr = entry.get("date");
                        Calendar promoCal = DateUtils.parseDateString(promotionDateStr);

                        if (promoCal != null) {
                            int promoYear = promoCal.get(Calendar.YEAR);
                            int promoMonth = promoCal.get(Calendar.MONTH);

                            if (promoYear < year || (promoYear == year && promoMonth <= i)) {
                                semesterForMonth = promotedSem;
                            }
                        }
                    } catch (NumberFormatException | NullPointerException e) {
                        Log.e(TAG, "Error parsing promotion history entry for student " + student.getStudentId() + ": " + e.getMessage());
                    }
                }

                FeePayment payment = paymentsMap.get(monthYearKey);

                if (isExpanded) {
                    if (payment != null && Constants.STATUS_PAID.equals(payment.getStatus())) {
                        if (payment.getSemesterWhenPaid() != -1) {
                            backgroundColor = Constants.getSemesterColor(payment.getSemesterWhenPaid());
                        } else {
                            // Fallback if semesterWhenPaid is not set, use a generic paid color
                            backgroundColor = ContextCompat.getColor(context, R.color.green);
                        }
                        statusText = "Paid";
                    } else if (DateUtils.isFutureMonth(i, year)) {
                        backgroundColor = ContextCompat.getColor(context, R.color.gray);
                        statusText = "Future";
                    } else {
                        backgroundColor = ContextCompat.getColor(context, R.color.red);
                        statusText = "Unpaid";
                    }
                    monthTextView.setText(String.format(Locale.getDefault(), "%s\n(%s)", monthAbbreviation, statusText));
                    monthTextView.setTextSize(10);
                } else {
                    if (payment != null && Constants.STATUS_PAID.equals(payment.getStatus())) {
                        backgroundColor = ContextCompat.getColor(context, R.color.green);
                    } else if (DateUtils.isFutureMonth(i, year)) {
                        backgroundColor = ContextCompat.getColor(context, R.color.gray);
                    } else {
                        backgroundColor = ContextCompat.getColor(context, R.color.red);
                    }
                    monthTextView.setText(monthAbbreviation);
                }

                monthTextView.setBackgroundColor(backgroundColor);
                monthBarLayout.addView(monthTextView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Crash in populateMonthBar for student ID: " + student.getStudentId() + ", Name: " + student.getName() + ": " + e.getMessage(), e);
            Toast.makeText(context, "Error displaying student data for " + student.getName(), Toast.LENGTH_LONG).show();
            monthBarLayout.removeAllViews();
        }
    }

    static class ViewHolder {
        TextView tvStudentName;
        ImageView ivExpandCollapse;
        LinearLayout llMonthBar;
        TextView tvGender;
        TextView tvMobile;
        TextView tvGuardianMobile;
        ImageButton btnDeleteStudent;
    }
}
