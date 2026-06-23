package com.example.quanlychitieu.view;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.controller.ReportController;
import com.example.quanlychitieu.controller.TransactionController;
import com.example.quanlychitieu.model.Transaction;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReportActivity extends AppCompatActivity {
    private TextView txtIncome, txtExpense, txtTrend;
    private RecyclerView rv;
    private Button btnExport, btnViewHistory, btnFilterDate;
    private ImageView btnBackReport;
    private TextView btnSelectMonth;
    private String selectedMonth;

    //controller
    private final ReportController reportController = new ReportController();
    private final TransactionController transactionController = new TransactionController();

    //Utils
    private final NumberFormat moneyFormat =
            NumberFormat.getInstance(new Locale("vi", "VN"));
    private List<Transaction> currentList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        initViews();
        setListeners();
        loadData();
    }

    private void initViews() {
        txtIncome = findViewById(R.id.txtReportIncome);
        txtExpense = findViewById(R.id.txtReportExpense);
        txtTrend = findViewById(R.id.txtTrend);

        rv = findViewById(R.id.rvCategoryReport);
        rv.setLayoutManager(new LinearLayoutManager(this));

        btnExport = findViewById(R.id.btnExport);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        btnBackReport = findViewById(R.id.btnBackReport);
        btnFilterDate = findViewById(R.id.btnFilterDate);
        btnSelectMonth = findViewById(R.id.btnSelectMonth);
    }

    private void setListeners() {

        btnBackReport.setOnClickListener(v -> finish());

        btnViewHistory.setOnClickListener(v ->
                startActivity(new Intent(this, ReportHistoryActivity.class)));

        btnFilterDate.setOnClickListener(v -> openDatePicker());

        btnExport.setOnClickListener(v ->
                ReportExporter.exportToCSV(this, currentList));

        btnSelectMonth.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {

                        String monthYear =
                                String.format(Locale.getDefault(),
                                        "%02d/%04d",
                                        month + 1, year);

                        selectedMonth = monthYear;
                        btnSelectMonth.setText("Tháng " + monthYear + " ▾");

                        loadByMonth(monthYear);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

    }

    private void loadByMonth(String monthYear) {
        transactionController.getTransactionsByMonth(monthYear,
                new TransactionController.TransactionListCallback() {
                    @Override
                    public void onLoaded(List<Transaction> transactions,
                                         double income, double expense) {

                        reportController.generate(transactions, (inc, exp,
                                                                 trend,
                                                                 list) -> {

                            txtIncome.setText("Tổng thu: " + formatMoney(inc));
                            txtExpense.setText("Tổng chi: " + formatMoney(exp));
                            txtTrend.setText("Xu hướng: " + trend);

                            if (trend != null) {

                                if (trend.toLowerCase().contains("ổn định")) {
                                    txtTrend.setTextColor(Color.parseColor("#2E7D32"));
                                } else if (trend.toLowerCase().contains("chi vượt thu")) {
                                    txtTrend.setTextColor(Color.parseColor("#C62828"));
                                } else {
                                    txtTrend.setTextColor(Color.parseColor("#444444"));
                                }
                            }

                            rv.setAdapter(new ReportAdapter(list));
                        });
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(ReportActivity.this,
                                "Lỗi: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Date picker
    private void openDatePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {

                    String selectedDate = String.format(Locale.getDefault(),
                            "%02d/%02d/%04d",
                            dayOfMonth, month + 1, year);

                    filterByDate(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        dialog.show();
    }

    private void loadData() {

        // Lấy tháng hiện tại MM/yyyy
        SimpleDateFormat sdf =
                new SimpleDateFormat("MM/yyyy", Locale.getDefault());

        selectedMonth = sdf.format(Calendar.getInstance().getTime());

        // Hiển thị lên nút chọn tháng
        btnSelectMonth.setText("Tháng " + selectedMonth + " ▾");

        // Gọi load dữ liệu theo tháng
        transactionController.getTransactionsByMonth(selectedMonth,
                new TransactionController.TransactionListCallback() {
                    @Override
                    public void onLoaded(List<Transaction> transactions,
                                         double totalIncome, double totalExpense) {

                        if (transactions == null || transactions.isEmpty()) {
                            txtIncome.setText("Tổng thu: 0 ₫");
                            txtExpense.setText("Tổng chi: 0 ₫");
                            txtTrend.setText("Xu hướng: Không có dữ liệu");

                            rv.setAdapter(null);

                            Toast.makeText(ReportActivity.this,
                                    "Không có dữ liệu tháng này",
                                    Toast.LENGTH_SHORT).show();

                            return;
                        }

                        currentList = transactions;
                        reportController.generate(transactions,
                                (income, expense, trend,
                                 list) -> {
                                    updateUI(income, expense, trend, list);
                                });
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(ReportActivity.this,
                                "Lỗi: " + errorMessage,
                                Toast.LENGTH_SHORT).show();

                        txtIncome.setText("Tổng thu: 0 ₫");
                        txtExpense.setText("Tổng chi: 0 ₫");
                        txtTrend.setText("Xu hướng: Không có dữ liệu");

                        rv.setAdapter(null);
                    }
                });
    }

    // lọc theo ngày
    private void filterByDate(String date) {
        transactionController.getAllTransactions(new TransactionController.TransactionListCallback() {
            @Override
            public void onLoaded(List<Transaction> transactions,
                                 double income, double expense) {

                List<Transaction> filtered = new ArrayList<>();

                for (Transaction t : transactions) {
                    if (t.getDate() != null && t.getDate().startsWith(date)) {
                        filtered.add(t);
                    }
                }

                currentList = filtered;
                reportController.generate(filtered, (inc, exp, trend, list) -> {
                    updateUI(inc, exp, trend, list);
                });
            }

            @Override
            public void onFailure(String errorMessage) {}
        });
    }

    // cập nhật UI
    private void updateUI(double income, double expense, String trend,
                          List<ReportController.CategoryReport> list) {

        txtIncome.setText("Tổng thu: " + formatMoney(income));
        txtExpense.setText("Tổng chi: " + formatMoney(expense));

        txtTrend.setText("Xu hướng: " + trend);

        if (trend != null) {
            if (trend.toLowerCase().contains("ổn định")) {
                txtTrend.setTextColor(Color.parseColor("#2E7D32")); // xanh
            }
            else if (trend.toLowerCase().contains("chi vượt thu")
                    || expense > income) {
                txtTrend.setTextColor(Color.parseColor("#C62828")); // đỏ
            }
            else {
                txtTrend.setTextColor(Color.parseColor("#F9A825")); // vàng
            }
        }

        rv.setAdapter(new ReportAdapter(list));
    }

    private String formatMoney(double value) {
        return moneyFormat.format(value) + " ₫";
    }
}