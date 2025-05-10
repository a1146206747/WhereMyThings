package com.example.wheremythings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.*;
import android.content.Context;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.DataType;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    RadioGroup radioGroupType;
    EditText inputLocation, inputDescription;
    ImageView imageViewPreview;
    Button uploadPhotoButton, submitReportButton;
    Uri selectedImageUri = null;
    Bitmap selectedBitmap = null;

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
                selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                imageViewPreview.setImageBitmap(selectedBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void submitReport() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to submit a report", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = currentUser.getUid();

        int checkedRadioButtonId = radioGroupType.getCheckedRadioButtonId();
        if (checkedRadioButtonId == -1) {
            Toast.makeText(this, "Please select a report type", Toast.LENGTH_SHORT).show();
            return;
        }
        String reportType = ((RadioButton) findViewById(checkedRadioButtonId)).getText().toString();
        String location = inputLocation.getText().toString().trim();
        String description = inputDescription.getText().toString().trim();

        if (location.isEmpty() || description.isEmpty() || selectedImageUri == null || selectedBitmap == null) {
            Toast.makeText(this, "Please fill all fields and upload a photo", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String prediction = classifyImage(ReportActivity.this, selectedBitmap);
            Toast.makeText(ReportActivity.this, "AI 判斷結果: " + prediction, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(ReportActivity.this, "模型分類失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        uploadImageToFirebaseStorage(reportType, location, description, uid);
    }

    private String classifyImage(Context context, Bitmap bitmap) throws IOException {
        Log.d("TFLite", "Loading model...");
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd("model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        MappedByteBuffer modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);

        Interpreter interpreter = new Interpreter(modelBuffer);
        Log.d("TFLite", "Model loaded successfully");

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
        Log.d("TFLite", "Image resized to 224x224");

        float[][][][] input = new float[1][224][224][3];
        int[] intValues = new int[224 * 224];
        resized.getPixels(intValues, 0, 224, 0, 0, 224, 224);
        for (int i = 0; i < 224; ++i) {
            for (int j = 0; j < 224; ++j) {
                int pixelValue = intValues[i * 224 + j];
                input[0][i][j][0] = ((pixelValue >> 16) & 0xFF) / 255.0f;
                input[0][i][j][1] = ((pixelValue >> 8) & 0xFF) / 255.0f;
                input[0][i][j][2] = (pixelValue & 0xFF) / 255.0f;
            }
        }

        float[][] output = new float[1][4];
        interpreter.run(input, output);

        // Log raw output probabilities
        for (int i = 0; i < 4; i++) {
            Log.d("TFLite", "Class " + i + " score: " + output[0][i]);
        }

        int maxIdx = 0;
        for (int i = 1; i < 4; i++) {
            if (output[0][i] > output[0][maxIdx]) maxIdx = i;
        }

        String[] labels = {"Cat", "Dog", "backpack", "wallet"};
        String result = labels[maxIdx];

        Log.d("TFLite", "Predicted class: " + result);
        return result;
    }


    private void uploadImageToFirebaseStorage(String reportType, String location, String description, String uid) {
        String imageFileName = "images/" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageReference.child(imageFileName);

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String imageUrl = uri.toString();
                                saveReportToDatabase(reportType, location, description, imageUrl, uid);
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ReportActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveReportToDatabase(String reportType, String location, String description, String imageUrl, String uid) {
        String reportId = databaseReference.push().getKey();
        if (reportId == null) {
            Toast.makeText(this, "Failed to generate report ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> report = new HashMap<>();
        report.put("uid", uid);
        report.put("reportType", reportType);
        report.put("location", location);
        report.put("description", description);
        report.put("imageUrl", imageUrl);
        report.put("timestamp", System.currentTimeMillis());

        databaseReference.child(reportId).setValue(report)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ReportActivity.this, "Report submitted successfully!", Toast.LENGTH_SHORT).show();
                    radioGroupType.clearCheck();
                    inputLocation.setText("");
                    inputDescription.setText("");
                    imageViewPreview.setImageDrawable(null);
                    selectedImageUri = null;
                    selectedBitmap = null;
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ReportActivity.this, "Failed to save report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}