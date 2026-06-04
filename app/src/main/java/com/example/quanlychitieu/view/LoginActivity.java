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
    private TextView txtForgotPassword;
    private TextView txtGoToRegister;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
            return;
        }


        edtEmail = findViewById(R.id.edtLoginEmail);
        edtPassword = findViewById(R.id.edtLoginPassword);
        btnSubmitLogin = findViewById(R.id.btnSubmitLogin);
        txtError = findViewById(R.id.txtLoginError);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);
        txtGoToRegister = findViewById(R.id.txtGoToRegister);


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


                                    if (user != null && user.isEmailVerified()) {
                                        txtError.setVisibility(View.GONE);
                                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        txtError.setText("Tài khoản chưa xác thực! Hãy kiểm tra Gmail của bạn.");
                                        txtError.setVisibility(View.VISIBLE);
                                        mAuth.signOut();
                                    }
                                } else {
                                    txtError.setText("Mật khẩu hoặc tài khoản không đúng");
                                    txtError.setVisibility(View.VISIBLE);
                                }
                            }
                        });
            }
        });


        txtForgotPassword.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();

            if (email.isEmpty()) {
                txtError.setText("Vui lòng điền Email của bạn vào ô tài khoản trước!");
                txtError.setVisibility(View.VISIBLE);
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            txtError.setVisibility(View.GONE);
                            Toast.makeText(LoginActivity.this,
                                    "Đường link đặt lại mật khẩu đã được gửi vào Gmail của bạn!",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            txtError.setText("Lỗi: Không thể gửi email khôi phục. Vui lòng kiểm tra lại Email!");
                            txtError.setVisibility(View.VISIBLE);
                        }
                    });
        });


        if (txtGoToRegister != null) {
            txtGoToRegister.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            });
        }
    }
}