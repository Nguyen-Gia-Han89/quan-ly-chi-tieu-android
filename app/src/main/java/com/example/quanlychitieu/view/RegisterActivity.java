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

public class RegisterActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword, edtConfirmPassword;
    private Button btnSubmitRegister;
    private TextView txtError;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();


        edtEmail = findViewById(R.id.edtRegisterEmail);
        edtPassword = findViewById(R.id.edtRegisterPassword);
        edtConfirmPassword = findViewById(R.id.edtRegisterConfirmPassword);
        btnSubmitRegister = findViewById(R.id.btnSubmitRegister);
        txtError = findViewById(R.id.txtRegisterError);


        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Đăng ký tài khoản"); // Đặt tiêu đề cho thanh Action Bar
        }


        btnSubmitRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = edtEmail.getText().toString().trim();
                String password = edtPassword.getText().toString().trim();
                String confirmPassword = edtConfirmPassword.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    showError("Vui lòng nhập đầy đủ thông tin!");
                    return;
                }

                if (password.length() < 6) {
                    showError("Mật khẩu phải có ít nhất 6 ký tự!");
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    showError("Mật khẩu xác nhận không đúng!");
                    return;
                }


                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> taskAuth) {
                                if (taskAuth.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    if (user != null) {
                                        // Gửi email xác thực kích hoạt tài khoản
                                        user.sendEmailVerification()
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> taskEmail) {
                                                        if (taskEmail.isSuccessful()) {
                                                            Toast.makeText(RegisterActivity.this,
                                                                    "Đăng ký thành công! Hãy kiểm tra hộp thư Gmail để xác thực.",
                                                                    Toast.LENGTH_LONG).show();


                                                            mAuth.signOut();

                                                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                                            startActivity(intent);
                                                            finish();
                                                        } else {
                                                            showError("Lỗi gửi mail: " + taskEmail.getException().getMessage());
                                                        }
                                                    }
                                                });
                                    }
                                } else {
                                    showError("Lỗi đăng ký: " + taskAuth.getException().getMessage());
                                }
                            }
                        });
            }
        });
    }


    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void showError(String message) {
        if (txtError != null) {
            txtError.setText(message);
            txtError.setVisibility(View.VISIBLE);
        }
    }
}