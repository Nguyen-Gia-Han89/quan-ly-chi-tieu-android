package com.example.quanlychitieu.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.controller.ReportController;
import com.example.quanlychitieu.controller.TransactionController;
import com.example.quanlychitieu.model.Transaction;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReportActivity extends AppCompatActivity {

    private TextView txtIncome, txtExpense, txtTrend;
    private RecyclerView rv;
    private Button btnExport, btnViewHistory;
    private ImageView btnBackReport;

    private final ReportController reportController = new ReportController();
    private final TransactionController transactionController = new TransactionController();
    private final NumberFormat moneyFormat =
            NumberFormat.getInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        txtIncome = findViewById(R.id.txtReportIncome);
        txtExpense = findViewById(R.id.txtReportExpense);
        txtTrend = findViewById(R.id.txtTrend);
        rv = findViewById(R.id.rvCategoryReport);
        btnExport = findViewById(R.id.btnExport);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        btnBackReport = findViewById(R.id.btnBackReport);

        rv.setLayoutManager(new LinearLayoutManager(this));

        btnBackReport.setOnClickListener(v -> finish());

        loadData();
    }

    private void loadData() {

        SimpleDateFormat sdf = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
        String monthYear = sdf.format(Calendar.getInstance().getTime());

        transactionController.getTransactionsByMonth(monthYear,
                new TransactionController.TransactionListCallback() {

                    @Override
                    public void onLoaded(List<Transaction> transactions,
                                         double totalIncome,
                                         double totalExpense) {

                        if (transactions == null || transactions.isEmpty()) {
                            Toast.makeText(ReportActivity.this,
                                    "Không có dữ liệu",
                                    Toast.LENGTH_SHORT).show();
                        }

                        reportController.generate(transactions, (income, expense, trend, list) -> {

                            txtIncome.setText("Tổng thu: " + formatMoney(income));
                            txtExpense.setText("Tổng chi: " + formatMoney(expense));
                            txtTrend.setText("Xu hướng: " + trend);

                            rv.setAdapter(new ReportAdapter(list));
                        });

                        btnExport.setOnClickListener(v ->
                                ReportExporter.exportToCSV(
                                        ReportActivity.this,
                                        transactions
                                )
                        );

                        btnViewHistory.setOnClickListener(v -> {
                            Intent intent = new Intent(
                                    ReportActivity.this,
                                    ReportHistoryActivity.class
                            );
                            startActivity(intent);
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
                    }
                });
    }

    private String formatMoney(double value) {
        return moneyFormat.format(value) + " ₫";
    }
}