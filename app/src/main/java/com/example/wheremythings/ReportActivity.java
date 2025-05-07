package com.example.wheremythings;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;
import android.view.View;

import java.io.IOException;

public class ReportActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    RadioGroup radioGroupType;
    EditText inputLocation, inputDescription;
    ImageView imageViewPreview;
    Button uploadPhotoButton, submitReportButton;
    Uri selectedImageUri = null;

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
        String reportType = ((RadioButton) findViewById(radioGroupType.getCheckedRadioButtonId())).getText().toString();
        String location = inputLocation.getText().toString().trim();
        String description = inputDescription.getText().toString().trim();

        if (location.isEmpty() || description.isEmpty() || selectedImageUri == null) {
            Toast.makeText(this, "Please fill all fields and upload a photo", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔜 這裡可以接上 Firebase Storage + Database
        Toast.makeText(this, "Report Ready to Submit (Type: " + reportType + ")", Toast.LENGTH_SHORT).show();
    }
}
