package com.example.quanlychitieu.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.quanlychitieu.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnSubmitLogin;
    private TextView txtError;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        edtEmail = findViewById(R.id.edtLoginEmail);
        edtPassword = findViewById(R.id.edtLoginPassword);
        btnSubmitLogin = findViewById(R.id.btnSubmitLogin);
        txtError = findViewById(R.id.txtLoginError);

        btnSubmitLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = edtEmail.getText().toString().trim();
                String password = edtPassword.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    txtError.setText("Vui lòng nhập đầy đủ thông tin!");
                    txtError.setVisibility(View.VISIBLE);
                    return;
                }

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();

                                    // KIỂM TRA XEM NGƯỜI DÙNG ĐÃ NHẤP VÀO LINK XÁC THỰC TRONG EMAIL CHƯA
                                    if (user != null && user.isEmailVerified()) {
                                        txtError.setVisibility(View.GONE);
                                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                                        // 🌟 ĐÃ CẬP NHẬT: Thay đổi đích đến từ BudgetActivity sang HomeActivity (Dashboard chính)
                                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                        startActivity(intent);
                                        finish(); // Đóng LoginActivity để chặn việc nhấn phím Back quay lại giao diện đăng nhập
                                    } else {
                                        // Nếu chưa bấm link kích hoạt tài khoản trong hòm thư
                                        txtError.setText("Tài khoản chưa xác thực! Hãy kiểm tra Gmail của bạn.");
                                        txtError.setVisibility(View.VISIBLE);
                                        mAuth.signOut(); // Đăng xuất tạm thời để ép xác thực
                                    }
                                } else {
                                    txtError.setText("Mật khẩu hoặc tài khoản không đúng");
                                    txtError.setVisibility(View.VISIBLE);
                                }
                            }
                        });
            }
        });
    }
}