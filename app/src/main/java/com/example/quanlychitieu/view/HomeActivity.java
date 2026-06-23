package com.example.quanlychitieu.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.quanlychitieu.R;
import com.example.quanlychitieu.controller.TransactionController;
import com.example.quanlychitieu.model.Transaction;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {

    private LinearLayout btnHistory, btnStats, btnCreateExpense;
    private CardView btnProfile;
    private TextView txtTotalSpent;
    private TextView txtTotalIncome;
    private TextView txtHomeUserName;

    private TransactionController transactionController;

    private LinearLayout btnReport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        transactionController = new TransactionController();

        btnProfile = findViewById(R.id.btnProfile);
        btnHistory = findViewById(R.id.btnHistory);
        btnStats = findViewById(R.id.btnStats);
        btnCreateExpense = findViewById(R.id.btnCreateExpense);
        txtTotalSpent = findViewById(R.id.txtTotalSpent);
        txtTotalIncome = findViewById(R.id.txtTotalIncome);
        txtHomeUserName = findViewById(R.id.txtHomeUserName);
        btnReport = findViewById(R.id.btnReport);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                return true;
            }

            if (item.getItemId() == R.id.nav_history) {
                startActivity(new Intent(HomeActivity.this, HistoryActivity.class));
                return true;
            }

            if (item.getItemId() == R.id.nav_goal) {
                startActivity(new Intent(HomeActivity.this, SavingGoalActivity.class));
                return true;
            }

            if (item.getItemId() == R.id.nav_notification) {
                bottomNavigation.removeBadge(R.id.nav_notification);
                startActivity(new Intent(HomeActivity.this, NotificationActivity.class));
                return true;
            }

            if (item.getItemId() == R.id.nav_profile) {
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                return true;
            }
            return true;
        });

        setupNotificationBadge(bottomNavigation);

        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        btnStats.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, StatsActivity.class);
            startActivity(intent);
        });

        btnReport.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ReportActivity.class);
            startActivity(intent);
        });


        btnCreateExpense.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TransactionActivity.class);
            intent.putExtra(TransactionActivity.KEY_MODE, TransactionActivity.MODE_ADD);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            if (bottomNavigation.getSelectedItemId() != R.id.nav_home) {
                bottomNavigation.setSelectedItemId(R.id.nav_home);
            }
        }
        checkBudgetLimit();
    }

    private void checkBudgetLimit() {
        SharedPreferences pref = getSharedPreferences("MoneyMate", MODE_PRIVATE);

        long limit = pref.getLong("budget_limit", 0);
        String userName = pref.getString("user_name", "");

        if (txtHomeUserName != null) {
            if (!userName.isEmpty()) {
                txtHomeUserName.setText("Chào " + userName + "!");
            } else {
                txtHomeUserName.setText("Chào bạn!");
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
        String currentMonthYear = sdf.format(Calendar.getInstance().getTime());

        transactionController.getTransactionsByMonth(currentMonthYear, new TransactionController.TransactionListCallback() {
            @Override
            public void onLoaded(List<Transaction> transactions, double totalIncome, double totalExpense) {
                DecimalFormat decimalFormat = new DecimalFormat("#,###");

                String formattedIncome = decimalFormat.format(totalIncome);
                txtTotalIncome.setText(formattedIncome + " VNĐ");

                String formattedPrice = decimalFormat.format(totalExpense);
                txtTotalSpent.setText(formattedPrice + " VNĐ");

                if (limit > 0 && totalExpense > limit) {
                    txtTotalSpent.setTextColor(Color.RED);
                    Toast.makeText(HomeActivity.this, "Cảnh báo: Chi tiêu vượt quá ngân sách cài đặt!", Toast.LENGTH_LONG).show();
                } else {
                    txtTotalSpent.setTextColor(Color.parseColor("#5E17EB"));
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                // Nếu lỗi mạng không lấy được tiền từ Firebase thì chạy tạm số 0
                txtTotalSpent.setText("0 VNĐ");
                txtTotalSpent.setTextColor(Color.parseColor("#5E17EB"));
                Toast.makeText(HomeActivity.this, "Không thể tải dữ liệu: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Đếm thông báo và hiển thị số thông báo mới
    private void setupNotificationBadge(BottomNavigationView bottomNavigation) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        FirebaseFirestore.getInstance().collection("users").document(uid).collection("notifications")
                .whereEqualTo("isRead", false).addSnapshotListener((value, error) -> {
                    if (value != null) {
                        int notificationCount = value.size();

                        BadgeDrawable badge = bottomNavigation.getOrCreateBadge(R.id.nav_notification);

                        if (notificationCount > 0) {
                            badge.setVisible(true);
                            badge.setNumber(notificationCount);
                            badge.setBackgroundColor(Color.RED);
                            badge.setBadgeTextColor(Color.WHITE);
                        } else {
                            bottomNavigation.removeBadge(R.id.nav_notification);
                        }
                    }
                });
    }
}