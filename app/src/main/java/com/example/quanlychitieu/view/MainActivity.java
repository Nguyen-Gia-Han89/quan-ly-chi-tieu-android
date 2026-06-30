package com.example.quanlychitieu.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.quanlychitieu.R;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private Button btnMainLogin, btnMainRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🌟 BƯỚC CỐT LÕI: Kiểm tra luồng vết đăng nhập ngầm trước khi nạp giao diện UI
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            // Nếu phiên đăng nhập cũ trên Firebase còn hiệu lực -> Bỏ qua màn hình chào
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            startActivity(intent);
            finish(); // Đóng hẳn MainActivity để không bị quay lại khi nhấn nút Back
            return;   // Dừng hoàn toàn việc thực thi các lệnh phía dưới
        }

        // Nếu người dùng chưa từng đăng nhập hoặc đã đăng xuất, nạp giao diện chào bình thường
        setContentView(R.layout.activity_main);

        btnMainLogin = findViewById(R.id.btnMainLogin);
        btnMainRegister = findViewById(R.id.btnMainRegister);

        btnMainLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        btnMainRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }
}