package com.voicenotes.app.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.voicenotes.app.R;
import com.voicenotes.app.utils.DateUtils;

import java.util.Calendar;

/**
 * Dialog for scheduling reminders with interactive Date and Time pickers.
 */
public class ReminderPickerDialog extends BottomSheetDialogFragment {

    public interface OnReminderSetListener {
        void onReminderSet(long reminderTimeMillis);
    }

    private OnReminderSetListener listener;
    private final Calendar selectedCalendar = Calendar.getInstance();
    private TextView tvSelectedDate;
    private TextView tvSelectedTime;

    public static ReminderPickerDialog newInstance(long initialTimeMillis) {
        ReminderPickerDialog dialog = new ReminderPickerDialog();
        Bundle args = new Bundle();
        args.putLong("initial_time", initialTimeMillis);
        dialog.setArguments(args);
        return dialog;
    }

    public void setListener(OnReminderSetListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_reminder_picker, container, false);

        long initialTime = System.currentTimeMillis() + 120000; // Default to 2 minutes from now
        if (getArguments() != null) {
            long passed = getArguments().getLong("initial_time", 0);
            if (passed > System.currentTimeMillis()) {
                initialTime = passed;
            }
        }
        selectedCalendar.setTimeInMillis(initialTime);

        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        tvSelectedTime = view.findViewById(R.id.tvSelectedTime);
        MaterialCardView btnPickDate = view.findViewById(R.id.btnPickDate);
        MaterialCardView btnPickTime = view.findViewById(R.id.btnPickTime);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancelReminder);
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirmReminder);

        updateLabels();

        btnPickDate.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(
                    requireContext(),
                    (view1, year, month, dayOfMonth) -> {
                        selectedCalendar.set(Calendar.YEAR, year);
                        selectedCalendar.set(Calendar.MONTH, month);
                        selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateLabels();
                    },
                    selectedCalendar.get(Calendar.YEAR),
                    selectedCalendar.get(Calendar.MONTH),
                    selectedCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePicker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            datePicker.show();
        });

        btnPickTime.setOnClickListener(v -> {
            TimePickerDialog timePicker = new TimePickerDialog(
                    requireContext(),
                    (view12, hourOfDay, minute) -> {
                        selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedCalendar.set(Calendar.MINUTE, minute);
                        selectedCalendar.set(Calendar.SECOND, 0);
                        selectedCalendar.set(Calendar.MILLISECOND, 0);
                        updateLabels();
                    },
                    selectedCalendar.get(Calendar.HOUR_OF_DAY),
                    selectedCalendar.get(Calendar.MINUTE),
                    false
            );
            timePicker.show();
        });

        btnCancel.setOnClickListener(v -> dismiss());

        btnConfirm.setOnClickListener(v -> {
            long reminderMillis = selectedCalendar.getTimeInMillis();
            if (reminderMillis <= System.currentTimeMillis()) {
                Toast.makeText(requireContext(), "Please select a future time", Toast.LENGTH_SHORT).show();
                return;
            }
            if (listener != null) {
                listener.onReminderSet(reminderMillis);
            }
            dismiss();
        });

        return view;
    }

    private void updateLabels() {
        tvSelectedDate.setText("Date: " + DateUtils.formatDate(selectedCalendar.getTimeInMillis()));
        tvSelectedTime.setText("Time: " + DateUtils.formatTime(selectedCalendar.getTimeInMillis()));
    }
}
