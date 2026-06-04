package com.example.quanlychitieu.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.quanlychitieu.R;
import com.example.quanlychitieu.controller.BudgetController;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private CardView avatarContainer;
    private ImageView imgAvatar;
    private EditText edtProfileName, edtProfilePhone, edtProfileBudget;
    private Button btnSaveProfile, btnCancelProfile, btnLogoutProfile; // 🌟 BỔ SUNG: Biến nút đăng xuất

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private BudgetController budgetController;

    private ActivityResultLauncher<String> pickImageLauncher;
    private Uri selectedImageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        budgetController = new BudgetController();

        registerImagePicker();
        initUI();
        loadSavedProfileData();
        setupListeners();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chỉnh sửa hồ sơ");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void initUI() {
        avatarContainer = findViewById(R.id.avatarContainer);
        imgAvatar = findViewById(R.id.imgAvatar);
        edtProfileName = findViewById(R.id.edtProfileName);
        edtProfilePhone = findViewById(R.id.edtProfilePhone);
        edtProfileBudget = findViewById(R.id.edtProfileBudget);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnCancelProfile = findViewById(R.id.btnCancelProfile);
        btnLogoutProfile = findViewById(R.id.btnLogoutProfile); // 🌟 BỔ SUNG: Ánh xạ ID từ XML
    }

    private void loadSavedProfileData() {
        SharedPreferences pref = getSharedPreferences("MoneyMate", MODE_PRIVATE);

        String savedName = pref.getString("user_name", "");
        String savedPhone = pref.getString("user_phone", "");
        long savedBudget = pref.getLong("budget_limit", 0);
        String savedAvatarUri = pref.getString("user_avatar_uri", "");

        edtProfileName.setText(savedName);
        edtProfilePhone.setText(savedPhone);

        if (savedBudget > 0) {
            edtProfileBudget.setText(String.valueOf(savedBudget));
        }

        if (!savedAvatarUri.isEmpty()) {
            imgAvatar.setImageURI(Uri.parse(savedAvatarUri));
        }
    }

    private void registerImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        imgAvatar.setImageURI(uri);
                        Toast.makeText(this, "Đã chọn ảnh thành công!", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupListeners() {
        avatarContainer.setOnClickListener(v -> {
            pickImageLauncher.launch("image/*");
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

            double finalBudgetLimit = budgetLimit;
            budgetController.updateBudget(finalBudgetLimit, new BudgetController.BudgetCallback() {
                @Override
                public void onSuccess(double budgetLimitReal) {
                    SharedPreferences pref = getSharedPreferences("MoneyMate", MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("user_name", name);
                    editor.putString("user_phone", phone);
                    editor.putLong("budget_limit", (long) budgetLimitReal);

                    if (selectedImageUri != null) {
                        editor.putString("user_avatar_uri", selectedImageUri.toString());
                    }
                    editor.apply();

                    Toast.makeText(ProfileActivity.this, "Cập nhật và đồng bộ Cloud thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(ProfileActivity.this, "Đồng bộ thất bại: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });


        btnLogoutProfile.setOnClickListener(v -> {

            mAuth.signOut();


            SharedPreferences pref = getSharedPreferences("MoneyMate", MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.clear(); // Xóa sạch toàn bộ data trong file MoneyMate
            editor.apply();

            Toast.makeText(this, "Đăng xuất thành công!", Toast.LENGTH_SHORT).show();


            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);


            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            finish();
        });

        btnCancelProfile.setOnClickListener(v -> finish());
    }
}