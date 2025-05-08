package com.example.wheremythings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.*;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    RadioGroup radioGroupType;
    EditText inputLocation, inputDescription;
    ImageView imageViewPreview;
    Button uploadPhotoButton, submitReportButton;
    Uri selectedImageUri = null;

    // Firebase 相關變量
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private FirebaseDatabase database;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        radioGroupType = findViewById(R.id.radioGroupType);
        inputLocation = findViewById(R.id.inputLocation);
        inputDescription = findViewById(R.id.inputDescription);
        imageViewPreview = findViewById(R.id.imageView4);
        uploadPhotoButton = findViewById(R.id.uploadPhotoButton);
        submitReportButton = findViewById(R.id.submitReportButton);

        // 初始化 Firebase Storage、Database 和 Auth
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        database = FirebaseDatabase.getInstance("https://wheremything-47fa4-default-rtdb.asia-southeast1.firebasedatabase.app/");
        databaseReference = database.getReference("user_reports");
        mAuth = FirebaseAuth.getInstance();

        uploadPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageChooser();
            }
        });

        submitReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReport();
            }
        });
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                imageViewPreview.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void submitReport() {
        // 檢查用戶是否登錄
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to submit a report", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = currentUser.getUid();

        // 獲取表單數據
        int checkedRadioButtonId = radioGroupType.getCheckedRadioButtonId();
        if (checkedRadioButtonId == -1) {
            Toast.makeText(this, "Please select a report type", Toast.LENGTH_SHORT).show();
            return;
        }
        String reportType = ((RadioButton) findViewById(checkedRadioButtonId)).getText().toString();
        String location = inputLocation.getText().toString().trim();
        String description = inputDescription.getText().toString().trim();

        // 驗證輸入
        if (location.isEmpty() || description.isEmpty() || selectedImageUri == null) {
            Toast.makeText(this, "Please fill all fields and upload a photo", Toast.LENGTH_SHORT).show();
            return;
        }

        // 上傳圖片到 Firebase Storage
        uploadImageToFirebaseStorage(reportType, location, description, uid);
    }

    private void uploadImageToFirebaseStorage(String reportType, String location, String description, String uid) {
        // 創建圖片的文件名（使用時間戳避免重名）
        String imageFileName = "images/" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageReference.child(imageFileName);

        // 上傳圖片
        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // 獲取圖片的下載 URL
                        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String imageUrl = uri.toString();
                                Log.d("ReportActivity", "Image uploaded successfully: " + imageUrl);

                                // 將報告數據存入 Realtime Database
                                saveReportToDatabase(reportType, location, description, imageUrl, uid);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("ReportActivity", "Failed to get image URL: " + e.getMessage());
                                Toast.makeText(ReportActivity.this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("ReportActivity", "Failed to upload image: " + e.getMessage());
                        Toast.makeText(ReportActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveReportToDatabase(String reportType, String location, String description, String imageUrl, String uid) {
        // 使用 push() 生成唯一鍵
        String reportId = databaseReference.push().getKey();
        if (reportId == null) {
            Toast.makeText(this, "Failed to generate report ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // 創建報告數據
        Map<String, Object> report = new HashMap<>();
        report.put("uid", uid);
        report.put("reportType", reportType);
        report.put("location", location);
        report.put("description", description);
        report.put("imageUrl", imageUrl);
        report.put("timestamp", System.currentTimeMillis());

        // 存入 Realtime Database 的 user_reports 節點
        databaseReference.child(reportId).setValue(report)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("ReportActivity", "Report saved to database successfully");
                        Toast.makeText(ReportActivity.this, "Report submitted successfully!", Toast.LENGTH_SHORT).show();

                        // 清空表單
                        radioGroupType.clearCheck();
                        inputLocation.setText("");
                        inputDescription.setText("");
                        imageViewPreview.setImageDrawable(null);
                        selectedImageUri = null;
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("ReportActivity", "Failed to save report: " + e.getMessage());
                        Toast.makeText(ReportActivity.this, "Failed to save report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}