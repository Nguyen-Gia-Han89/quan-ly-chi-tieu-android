package com.example.quanlychitieu.view;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;
import com.example.quanlychitieu.R;
import com.example.quanlychitieu.controller.BudgetController;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private CardView avatarContainer;
    private ImageView imgAvatar;
    private EditText edtProfileName, edtProfilePhone;
    private SwitchMaterial switchReminder;
    private TextView txtReminderTime;
    private Button btnSaveProfile, btnCancelProfile, btnLogoutProfile;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private BudgetController budgetController;

    private ActivityResultLauncher<String> pickImageLauncher;
    private Uri selectedImageUri = null;

    private int chosenHour = 21;
    private int chosenMinute = 0;

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
        switchReminder = findViewById(R.id.switchReminder);
        txtReminderTime = findViewById(R.id.txtReminderTime);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnCancelProfile = findViewById(R.id.btnCancelProfile);
        btnLogoutProfile = findViewById(R.id.btnLogoutProfile);
    }

    private void loadSavedProfileData() {
        SharedPreferences pref = getSharedPreferences("MoneyMate", MODE_PRIVATE);

        String savedName = pref.getString("user_name", "");
        String savedPhone = pref.getString("user_phone", "");
        String savedAvatarUri = pref.getString("user_avatar_uri", "");

        boolean isReminderOn = pref.getBoolean("daily_reminder_on", false);
        chosenHour = pref.getInt("reminder_hour", 21);
        chosenMinute = pref.getInt("reminder_minute", 0);

        edtProfileName.setText(savedName);
        edtProfilePhone.setText(savedPhone);
        switchReminder.setChecked(isReminderOn);

        setReminderTimeText(chosenHour, chosenMinute);

        if (!savedAvatarUri.isEmpty()) {
            // SỬA ĐỔI: Thêm khối try-catch bảo vệ ứng dụng khỏi crash do URI cũ hết hạn
            try {
                imgAvatar.setImageURI(Uri.parse(savedAvatarUri));
            } catch (SecurityException e) {
                e.printStackTrace();
                // Nếu lỗi, xóa URI lỗi đó đi và đặt lại ảnh mặc định
                imgAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
                pref.edit().remove("user_avatar_uri").apply();
            }
        }
    }

    private void registerImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        // SỬA ĐỔI: Sao chép file trực tiếp vào bộ nhớ trong của ứng dụng
                        Uri internalUri = copyUriToInternalStorage(this, uri);
                        if (internalUri != null) {
                            selectedImageUri = internalUri;
                            imgAvatar.setImageURI(internalUri);
                            Toast.makeText(this, "Đã chọn ảnh thành công!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Lỗi khi xử lý hình ảnh!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    // THÊM MỚI: Hàm phụ trợ sao chép tệp để sở hữu quyền truy cập vĩnh viễn
    private Uri copyUriToInternalStorage(Context context, Uri sourceUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
            if (inputStream == null) return null;

            // Đặt tên tệp avatar cố định bên trong thư mục files của ứng dụng
            File file = new File(context.getFilesDir(), "user_avatar_cached.jpg");
            OutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            return Uri.fromFile(file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setupListeners() {
        avatarContainer.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        txtReminderTime.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    (view, hourOfDay, minute) -> {
                        chosenHour = hourOfDay;
                        chosenMinute = minute;
                        setReminderTimeText(chosenHour, chosenMinute);
                    }, chosenHour, chosenMinute, true);
            timePickerDialog.show();
        });

        btnSaveProfile.setOnClickListener(v -> {
            String name = edtProfileName.getText().toString().trim();
            String phone = edtProfilePhone.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên người dùng!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (mAuth.getCurrentUser() != null) {
                String uid = mAuth.getCurrentUser().getUid();
                Map<String, Object> userData = new HashMap<>();
                userData.put("userName", name);
                userData.put("userPhone", phone);
                userData.put("dailyReminderOn", switchReminder.isChecked());
                userData.put("reminderHour", chosenHour);
                userData.put("reminderMinute", chosenMinute);

                db.collection("users").document(uid).set(userData, com.google.firebase.firestore.SetOptions.merge());
            }

            SharedPreferences pref = getSharedPreferences("MoneyMate", MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("user_name", name);
            editor.putString("user_phone", phone);
            editor.putBoolean("daily_reminder_on", switchReminder.isChecked());
            editor.putInt("reminder_hour", chosenHour);
            editor.putInt("reminder_minute", chosenMinute);

            if (selectedImageUri != null) {
                editor.putString("user_avatar_uri", selectedImageUri.toString());
            }
            editor.apply();

            manageReminderAlarm(switchReminder.isChecked(), chosenHour, chosenMinute);

            Toast.makeText(ProfileActivity.this, "Cập nhật hồ sơ và tiện ích thành công!", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnLogoutProfile.setOnClickListener(v -> {
            mAuth.signOut();
            SharedPreferences pref = getSharedPreferences("MoneyMate", MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.clear();
            editor.apply();

            manageReminderAlarm(false, 0, 0);

            Toast.makeText(this, "Đăng xuất thành công!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnCancelProfile.setOnClickListener(v -> finish());
    }

    private void setReminderTimeText(int hour, int minute) {
        String baseText = String.format("🕒 %02d:%02d (Thay đổi)", hour, minute);
        SpannableString spannableString = new SpannableString(baseText);

        int startIndex = baseText.indexOf("(Thay đổi)");
        if (startIndex != -1) {
            spannableString.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    startIndex,
                    startIndex + "(Thay đổi)".length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        txtReminderTime.setText(spannableString);
    }

    private void manageReminderAlarm(boolean isEnabled, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager == null) return;

        if (isEnabled) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
            android.util.Log.d("MoneyMate_Alarm", "Đã đặt lịch chính xác vào lúc: " + hour + ":" + minute);
        } else {
            alarmManager.cancel(pendingIntent);
        }
    }

    public static class ReminderReceiver extends BroadcastReceiver {

        @android.annotation.SuppressLint("ScheduleExactAlarm")
        @Override
        public void onReceive(Context context, Intent intent) {
            String channelId = "money_mate_reminder";

            android.app.NotificationManager notificationManager =
                    (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                android.app.NotificationChannel channel = new android.app.NotificationChannel(
                        channelId,
                        "Nhắc nhở ghi chép",
                        android.app.NotificationManager.IMPORTANCE_HIGH
                );

                channel.setDescription("Kênh nhận thông báo nhắc nhở ghi chép chi tiêu cuối ngày");
                channel.enableLights(true);
                channel.enableVibration(true);
                channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);

                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                }
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle("MoneyMate Nhắc Nhở 📝")
                    .setContentText("Cuối ngày rồi, hãy vào ứng dụng để ghi chép lại các khoản chi tiêu hôm nay nhé!")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(true);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(context,
                        android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    android.util.Log.e("MoneyMate_Notification", "Chưa có quyền gửi thông báo trên hệ thống Android 13+");
                    return;
                }
            }

            try {
                if (notificationManager != null) {
                    notificationManager.notify(2002, builder.build());
                    android.util.Log.d("MoneyMate_Notification", "Đã bắn thông báo ra hệ thống thành công!");
                }
            } catch (Exception e) {
                android.util.Log.e("MoneyMate_Notification", "Lỗi hiển thị thông báo: " + e.getMessage());
            }

            SharedPreferences pref = context.getSharedPreferences("MoneyMate", Context.MODE_PRIVATE);
            boolean isReminderOn = pref.getBoolean("daily_reminder_on", false);
            if (isReminderOn) {
                int savedHour = pref.getInt("reminder_hour", 21);
                int savedMinute = pref.getInt("reminder_minute", 0);

                Calendar nextCalendar = Calendar.getInstance();
                nextCalendar.set(Calendar.HOUR_OF_DAY, savedHour);
                nextCalendar.set(Calendar.MINUTE, savedMinute);
                nextCalendar.set(Calendar.SECOND, 0);
                nextCalendar.set(Calendar.MILLISECOND, 0);
                nextCalendar.add(Calendar.DAY_OF_YEAR, 1);

                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                PendingIntent pi = PendingIntent.getBroadcast(
                        context,
                        1001,
                        new Intent(context, ReminderReceiver.class),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                if (am != null) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextCalendar.getTimeInMillis(), pi);
                    } else {
                        am.setExact(AlarmManager.RTC_WAKEUP, nextCalendar.getTimeInMillis(), pi);
                    }
                    android.util.Log.d("MoneyMate_Alarm", "Đã tự động gối lịch nhắc nhở sang ngày mai.");
                }
            }
        }
    }
}