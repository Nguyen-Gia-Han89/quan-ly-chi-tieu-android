package com.example.quanlychitieu.view;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.controller.TransactionController;
import com.example.quanlychitieu.model.Transaction;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    // FILTER STATE
    private String currentFilter = "ALL";
    private String selectedMonthYear;

    // UI
    private ImageView btnBackHistory;

    private TextView btnSelectMonth;

    private TextView tabAll, tabIncome, tabExpense;

    private TextView txtIncomeTotal, txtExpenseTotal, txtBalance;

    private RecyclerView rvTransactions;

    // DATA
    private TransactionController transactionController;
    private TransactionAdapter transactionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        transactionController = new TransactionController();

        // ====== INIT VIEW ======
        btnBackHistory = findViewById(R.id.btnBackHistory);
        btnSelectMonth = findViewById(R.id.btnSelectMonth);

        tabAll = findViewById(R.id.tabAll);
        tabIncome = findViewById(R.id.tabIncome);
        tabExpense = findViewById(R.id.tabExpense);

        txtIncomeTotal = findViewById(R.id.txtIncomeTotal);
        txtExpenseTotal = findViewById(R.id.txtExpenseTotal);
        txtBalance = findViewById(R.id.txtBalance);

        rvTransactions = findViewById(R.id.rvTransactions);

        // ====== RECYCLER ======
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));

        transactionAdapter = new TransactionAdapter(new ArrayList<>(), transaction -> {
            Intent intent = new Intent(HistoryActivity.this, TransactionActivity.class);
            intent.putExtra(TransactionActivity.KEY_MODE, TransactionActivity.MODE_DETAIL);
            intent.putExtra(TransactionActivity.KEY_DATA, transaction);
            startActivity(intent);
        });

        rvTransactions.setAdapter(transactionAdapter);

        // ====== DEFAULT MONTH ======
        SimpleDateFormat sdf = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
        selectedMonthYear = sdf.format(Calendar.getInstance().getTime());
        btnSelectMonth.setText("Tháng " + selectedMonthYear + " ▾");

        // ====== EVENTS ======
        btnBackHistory.setOnClickListener(v -> finish());

        btnSelectMonth.setOnClickListener(v -> showMonthPicker());

        setupTabClicks();

        updateTabUI(tabAll);

        loadTransactionHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTransactionHistory();
    }

    // ================= FILTER TABS =================
    private void setupTabClicks() {

        tabAll.setOnClickListener(v -> {
            currentFilter = "ALL";
            updateTabUI(tabAll);
            loadTransactionHistory();
        });

        tabIncome.setOnClickListener(v -> {
            currentFilter = "INCOME";
            updateTabUI(tabIncome);
            loadTransactionHistory();
        });

        tabExpense.setOnClickListener(v -> {
            currentFilter = "EXPENSE";
            updateTabUI(tabExpense);
            loadTransactionHistory();
        });
    }

    private void updateTabUI(TextView selected) {

        resetTab(tabAll);
        resetTab(tabIncome);
        resetTab(tabExpense);

        selected.setBackgroundResource(R.drawable.bg_tab_selected);
        selected.setTextColor(Color.WHITE);
    }

    private void resetTab(TextView tab) {
        tab.setBackground(null);
        tab.setTextColor(Color.GRAY);
    }

    // ================= LOAD DATA =================
    private void loadTransactionHistory() {

        transactionController.getTransactionsByMonth(selectedMonthYear,
                new TransactionController.TransactionListCallback() {

                    @Override
                    public void onLoaded(List<Transaction> transactions,
                                         double totalIncome,
                                         double totalExpense) {

                        List<Transaction> filtered = new ArrayList<>();

                        for (Transaction t : transactions) {

                            String type = t.getType();

                            if (currentFilter.equals("ALL")) {
                                filtered.add(t);
                            } else if (currentFilter.equals("INCOME")
                                    && type.equalsIgnoreCase("income")) {
                                filtered.add(t);
                            } else if (currentFilter.equals("EXPENSE")
                                    && type.equalsIgnoreCase("expense")) {
                                filtered.add(t);
                            }
                        }

                        transactionAdapter.updateList(filtered);

                        updateSummary(totalIncome, totalExpense);

                        if (filtered.isEmpty()) {
                            Toast.makeText(HistoryActivity.this,
                                    "Không có giao dịch",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(HistoryActivity.this,
                                "Lỗi: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ================= SUMMARY UI =================
    private void updateSummary(double income, double expense) {

        DecimalFormat df = new DecimalFormat("#,###");

        txtIncomeTotal.setText(df.format(income) + " ₫");
        txtExpenseTotal.setText(df.format(expense) + " ₫");

        double balance = income - expense;
        txtBalance.setText(df.format(balance) + " ₫");

        // màu cân đối
        if (balance < 0) {
            txtBalance.setTextColor(Color.RED);
        } else {
            txtBalance.setTextColor(Color.parseColor("#5E17EB"));
        }
    }

    // ================= MONTH PICKER =================
    private void showMonthPicker() {

        String[] months = {
                "01", "02", "03", "04", "05", "06",
                "07", "08", "09", "10", "11", "12"
        };

        String[] years = {"2024", "2025", "2026", "2027"};

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Chọn tháng");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);

        android.widget.NumberPicker monthPicker = new android.widget.NumberPicker(this);
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(months.length - 1);
        monthPicker.setDisplayedValues(months);

        android.widget.NumberPicker yearPicker = new android.widget.NumberPicker(this);
        yearPicker.setMinValue(0);
        yearPicker.setMaxValue(years.length - 1);
        yearPicker.setDisplayedValues(years);

        layout.addView(monthPicker);
        layout.addView(yearPicker);

        builder.setView(layout);

        builder.setPositiveButton("OK", (dialog, which) -> {

            String month = months[monthPicker.getValue()];
            String year = years[yearPicker.getValue()];

            selectedMonthYear = month + "/" + year;

            btnSelectMonth.setText("Tháng " + selectedMonthYear + " ▾");

            loadTransactionHistory();
        });

        builder.setNegativeButton("Hủy", null);

        builder.show();
    }
}