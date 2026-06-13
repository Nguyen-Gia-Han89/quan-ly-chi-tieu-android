package com.example.quanlychitieu.view;

import android.app.DatePickerDialog;
import android.content.Intent;
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
import java.util.Locale;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private ImageView btnBackHistory;
    private TextView btnSelectMonth, txtHistoryIncome, txtHistoryExpense;
    private RecyclerView rvTransactions;

    private TransactionController transactionController;
    private TransactionAdapter transactionAdapter;
    private String selectedMonthYear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        transactionController = new TransactionController();

        // Ánh xạ View từ layout XML
        btnBackHistory = findViewById(R.id.btnBackHistory);
        btnSelectMonth = findViewById(R.id.btnSelectMonth);
        txtHistoryIncome = findViewById(R.id.txtHistoryIncome);
        txtHistoryExpense = findViewById(R.id.txtHistoryExpense);
        rvTransactions = findViewById(R.id.rvTransactions);

        // Thiết lập RecyclerView hiển thị danh sách dạng Linear dọc
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));

        // KHỞI TẠO ADAPTER: Đã sửa lại logic click item để xem chi tiết
        transactionAdapter = new TransactionAdapter(new ArrayList<>(), transaction -> {
            Intent intent = new Intent(HistoryActivity.this, TransactionActivity.class);

            // 1. Chuyển sang chế độ xem chi tiết giao dịch (MODE_DETAIL = 2)
            intent.putExtra(TransactionActivity.KEY_MODE, TransactionActivity.MODE_DETAIL);

            // 2. Truyền object giao dịch được click sang màn hình TransactionActivity để hiển thị dữ liệu lên form
            intent.putExtra(TransactionActivity.KEY_DATA, transaction);

            startActivity(intent);
        });
        rvTransactions.setAdapter(transactionAdapter);

        // Lấy tháng năm hiện tại mặc định của máy để lọc khi vừa mở màn hình lên
        SimpleDateFormat sdf = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
        selectedMonthYear = sdf.format(Calendar.getInstance().getTime());
        btnSelectMonth.setText("Tháng " + selectedMonthYear + " ▾");

        // Tải dữ liệu lần đầu
        loadTransactionHistory();

        // Sự kiện click nút Chọn tháng
        btnSelectMonth.setOnClickListener(v -> showMonthYearPickerDialog());

        // Nút quay lại màn hình chính Home
        btnBackHistory.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Khi thêm/sửa/xóa từ màn hình TransactionActivity rồi quay về, danh sách tự động làm mới lại
        loadTransactionHistory();
    }

    private void loadTransactionHistory() {
        transactionController.getTransactionsByMonth(selectedMonthYear, new TransactionController.TransactionListCallback() {
            @Override
            public void onLoaded(List<Transaction> transactions, double totalIncome, double totalExpense) {
                // 1. Cập nhật danh sách giao dịch nạp vào RecyclerView thông qua Adapter
                transactionAdapter.updateList(transactions);

                // 2. Định dạng chuỗi số hiển thị tiền tệ
                DecimalFormat df = new DecimalFormat("#,###");
                txtHistoryIncome.setText("+" + df.format(totalIncome) + "đ");
                txtHistoryExpense.setText("-" + df.format(totalExpense) + "đ");

                if (transactions.isEmpty()) {
                    Toast.makeText(HistoryActivity.this, "Không có giao dịch nào trong tháng này", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(HistoryActivity.this, "Lỗi tải lịch sử: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMonthYearPickerDialog() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);

                    SimpleDateFormat sdf = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
                    selectedMonthYear = sdf.format(cal.getTime());

                    btnSelectMonth.setText("Tháng " + selectedMonthYear + " ▾");
                    loadTransactionHistory(); // Tải lại dữ liệu mới sau khi đổi tháng lọc
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.setTitle("Chọn Tháng / Năm cần lọc");
        datePickerDialog.show();
    }
}