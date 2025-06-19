package com.aquaa.edupay.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aquaa.edupay.R;
import com.aquaa.edupay.database.DatabaseHelper;
import com.aquaa.edupay.models.FeePayment;
import com.aquaa.edupay.models.Student;
import com.aquaa.edupay.utils.Constants;
import com.aquaa.edupay.utils.DateUtils;

import java.util.List;
import java.util.Map;

public class FeeStatusAdapter extends ArrayAdapter<Student> {

    private Context context;
    private List<Student> students;
    private Map<Integer, FeePayment> studentPaymentMap; // studentId -> FeePayment for the selected period
    private int selectedYear;
    private int selectedMonth; // 0-indexed
    private int selectedSemester; // 1-indexed
    private DatabaseHelper dbHelper;
    private FeeStatusUpdateListener listener; // Listener for updates

    // Interface for callback
    public interface FeeStatusUpdateListener {
        void onFeeStatusUpdated();
    }

    public void setFeeStatusUpdateListener(FeeStatusUpdateListener listener) {
        this.listener = listener;
    }

    public FeeStatusAdapter(@NonNull Context context, List<Student> students,
                            Map<Integer, FeePayment> studentPaymentMap,
                            int selectedYear, int selectedMonth, int selectedSemester) {
        super(context, 0, students);
        this.context = context;
        this.students = students;
        this.studentPaymentMap = studentPaymentMap;
        this.selectedYear = selectedYear;
        this.selectedMonth = selectedMonth;
        this.selectedSemester = selectedSemester;
        this.dbHelper = new DatabaseHelper(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(context).inflate(R.layout.item_fee_status, parent, false);
        }

        Student currentStudent = students.get(position);
        TextView tvStudentName = listItem.findViewById(R.id.tvStudentName);
        CheckBox cbPaidStatus = listItem.findViewById(R.id.cbPaidStatus);

        tvStudentName.setText(currentStudent.getName());

        // Get the specific payment for this student for the selected period
        // This payment object should be up-to-date from the Activity's refreshFeeStatusList()
        FeePayment payment = studentPaymentMap.get(currentStudent.getStudentId());

        if (payment != null && Constants.STATUS_PAID.equals(payment.getStatus())) {
            cbPaidStatus.setChecked(true);
            cbPaidStatus.setText("Paid");
        } else {
            cbPaidStatus.setChecked(false);
            cbPaidStatus.setText("Unpaid");
        }

        cbPaidStatus.setOnCheckedChangeListener(null); // Remove previous listener to prevent infinite loops
        cbPaidStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = ((CheckBox) v).isChecked();
                String monthYear = DateUtils.getMonthYearString(selectedMonth, selectedYear);
                String status = isChecked ? Constants.STATUS_PAID : Constants.STATUS_UNPAID;
                String paymentDate = isChecked ? DateUtils.getCurrentDate() : ""; // Set current date if marked paid

                // Determine the semester for this payment based on the student's active semester for the selected month/year
                // This is crucial to ensure the payment is recorded against the correct semester if promotions occurred.
                int semesterForPayment = dbHelper.getActiveSemesterForStudent(currentStudent.getStudentId(), selectedYear, selectedMonth);


                // Update or insert the fee payment status
                long result = dbHelper.updateOrInsertFeePaymentStatus(
                        currentStudent.getStudentId(),
                        monthYear,
                        semesterForPayment, // Use the determined semester for the payment record
                        status,
                        0.0, // Amount can be 0 or a default value
                        paymentDate
                );

                if (result != -1) {
                    Toast.makeText(context, currentStudent.getName() + " fee status updated to " + status, Toast.LENGTH_SHORT).show();
                    // Notify the activity to refresh its list
                    if (listener != null) {
                        listener.onFeeStatusUpdated();
                    }
                } else {
                    Toast.makeText(context, "Failed to update fee status for " + currentStudent.getName(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        return listItem;
    }
}
