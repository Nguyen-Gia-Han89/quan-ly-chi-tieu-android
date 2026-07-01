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
        Calendar calendar = Calendar.getInstance();

        // Sử dụng style Theme_Holo_Light để ép hiển thị dạng vòng xoay (Spinner)
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                (view, year, month, dayOfMonth) -> {

                    // Định dạng MM/yyyy từ tháng và năm được chọn
                    selectedMonthYear = String.format(Locale.getDefault(),
                            "%02d/%04d",
                            month + 1, year);

                    btnSelectMonth.setText("Tháng " + selectedMonthYear + " ▾");

                    // Tải lại dữ liệu lịch sử theo tháng mới chọn
                    loadTransactionHistory();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        // Mẹo tìm và ẩn trường chọn NGÀY (Day Spinner)
        try {
            int daySpinnerId = android.content.res.Resources.getSystem()
                    .getIdentifier("day", "id", "android");
            if (daySpinnerId != 0) {
                android.view.View daySpinner = dialog.getDatePicker().findViewById(daySpinnerId);
                if (daySpinner != null) {
                    daySpinner.setVisibility(android.view.View.GONE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Đổi background sang trong suốt để xóa viền đen thừa của Theme Holo cũ
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }

        dialog.setTitle("Chọn Tháng / Năm");
        dialog.show();
    }
}