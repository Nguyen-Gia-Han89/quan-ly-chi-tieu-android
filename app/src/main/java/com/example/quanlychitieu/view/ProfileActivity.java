package com.example.quanlychitieu.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.quanlychitieu.R;
import com.example.quanlychitieu.controller.BudgetController; // Khai báo package của Controller
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private CardView avatarContainer;
    private ImageView imgAvatar;
    private EditText edtProfileName, edtProfilePhone, edtProfileBudget;
    private Button btnSaveProfile, btnCancelProfile;


    private FirebaseAuth mAuth;
    private FirebaseFirestore db;


    private BudgetController budgetController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);


        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        budgetController = new BudgetController();


        initUI();


        loadSavedProfileData();


        setupListeners();
    }

    private void initUI() {
        avatarContainer = findViewById(R.id.avatarContainer);
        imgAvatar = findViewById(R.id.imgAvatar);
        edtProfileName = findViewById(R.id.edtProfileName);
        edtProfilePhone = findViewById(R.id.edtProfilePhone);
        edtProfileBudget = findViewById(R.id.edtProfileBudget);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnCancelProfile = findViewById(R.id.btnCancelProfile);
    }

    private void loadSavedProfileData() {
        SharedPreferences pref = getSharedPreferences("MoneyMate", MODE_PRIVATE);


        String savedName = pref.getString("user_name", "");
        String savedPhone = pref.getString("user_phone", "");
        long savedBudget = pref.getLong("budget_limit", 0);

        edtProfileName.setText(savedName);
        edtProfilePhone.setText(savedPhone);

        if (savedBudget > 0) {
            edtProfileBudget.setText(String.valueOf(savedBudget));
        }
    }

    private void setupListeners() {

        avatarContainer.setOnClickListener(v -> {
            Toast.makeText(this, "Người sau: Thêm quyền và Intent mở Gallery chọn ảnh tại đây!", Toast.LENGTH_LONG).show();
        });


        btnSaveProfile.setOnClickListener(v -> {
            String name = edtProfileName.getText().toString().trim();
            String phone = edtProfilePhone.getText().toString().trim();
            String budgetStr = edtProfileBudget.getText().toString().trim();

            if (name.isEmpty() || budgetStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên và hạn mức ngân sách!", Toast.LENGTH_SHORT).show();
                return;
            }


            double budgetLimit = 0;
            try {

                budgetStr = budgetStr.replaceAll("[^0-9.]", "");
                budgetLimit = Double.parseDouble(budgetStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Định dạng số tiền ngân sách không hợp lệ!", Toast.LENGTH_SHORT).show();
                return;
            }


            budgetController.updateBudget(budgetLimit, new BudgetController.BudgetCallback() {
                @Override
                public void onSuccess(double budgetLimitReal) {
                    // 🌟 2. KHI CLOUD LƯU THÀNH CÔNG -> TIẾN HÀNH ĐỒNG BỘ TIẾP XUỐNG BỘ NHỚ MÁY (SHARED PREFERENCES)
                    SharedPreferences pref = getSharedPreferences("MoneyMate", MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("user_name", name);
                    editor.putString("user_phone", phone);
                    editor.putLong("budget_limit", (long) budgetLimitReal); // Ép kiểu ngược về long để lưu máy cục bộ
                    editor.apply(); // Xác nhận ghi dữ liệu

                    Toast.makeText(ProfileActivity.this, "Cập nhật và đồng bộ Cloud thành công!", Toast.LENGTH_SHORT).show();


                    finish();
                }

                @Override
                public void onFailure(String errorMessage) {

                    Toast.makeText(ProfileActivity.this, "Đồng bộ thất bại: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });


        btnCancelProfile.setOnClickListener(v -> finish());
    }
}