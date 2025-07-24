package com.example.wheremythings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.os.Environment;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;
import org.tensorflow.lite.task.vision.detector.ObjectDetector.ObjectDetectorOptions;
import org.tensorflow.lite.task.vision.segmenter.ImageSegmenter;
import org.tensorflow.lite.task.vision.segmenter.ImageSegmenter.ImageSegmenterOptions;
import org.tensorflow.lite.task.vision.segmenter.OutputType;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.graphics.RectF;

public class ReportActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    RadioGroup radioGroupType;
    EditText inputLocation, inputDescription;
    ImageView imageViewPreview;
    Button uploadPhotoButton, submitReportButton, useCurrentLocationButton;
    Uri selectedImageUri = null;
    Bitmap selectedBitmap = null;
    private ImageButton backButton;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private FirebaseDatabase database;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;

    private float[] currentColorInput;
    private ImageSegmenter imageSegmenter;
    private ObjectDetector objectDetector;
    private static final int CAMERA_REQUEST_CODE = 2;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private Uri photoUri;

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
        useCurrentLocationButton = findViewById(R.id.useCurrentLocationButton);
        backButton = findViewById(R.id.backButton);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        database = FirebaseDatabase.getInstance("https://wheremything-47fa4-default-rtdb.asia-southeast1.firebasedatabase.app/");
        databaseReference = database.getReference("user_reports");
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize ImageSegmenter
        try {
            ImageSegmenterOptions options = ImageSegmenterOptions.builder()
                    .setOutputType(OutputType.CATEGORY_MASK)
                    .build();
            imageSegmenter = ImageSegmenter.createFromFileAndOptions(this, "deeplabv3.tflite", options);
            Log.d("ImageSegmenter", "ImageSegmenter initialized successfully");
        } catch (Exception e) {
            Log.e("ImageSegmenter", "Failed to initialize ImageSegmenter: " + e.getMessage());
        }

        // Initialize ObjectDetector
        initObjectDetector();

        // Back button click listener
        backButton.setOnClickListener(v -> finish());

        // Upload photo button click listener
        uploadPhotoButton.setOnClickListener(v -> openImageChooser());

        // Submit report button click listener
        submitReportButton.setOnClickListener(v -> submitReport());

        // Use current location button click listener
        useCurrentLocationButton.setOnClickListener(v -> getCurrentLocation());
    }

    private void openImageChooser() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            return;
        }

        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");


        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePhotoIntent.resolveActivity(getPackageManager()) != null) {

            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "創建照片文件失敗", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this,
                        "com.example.wheremythings.fileprovider",
                        photoFile);
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            }
        }

        Intent chooserIntent = Intent.createChooser(pickIntent, "選擇或拍攝圖片");
        if (takePhotoIntent.resolveActivity(getPackageManager()) != null) {
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePhotoIntent});
        }

        startActivityForResult(chooserIntent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "位置權限被拒絕", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImageChooser();
            } else {
                Toast.makeText(this, "相機權限被拒絕", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE_REQUEST) {
            if (data != null && data.getData() != null) {

                selectedImageUri = data.getData();
            } else if (photoUri != null) {

                selectedImageUri = photoUri;
            } else {
                Toast.makeText(this, "未選擇或拍攝任何圖片", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                imageViewPreview.setImageBitmap(selectedBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "載入圖片失敗", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getCurrentLocation() {
        // Check if location permissions are granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Check location settings
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> {
            // Location settings are satisfied, proceed to get location
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            String address = getAddressFromLocation(location.getLatitude(), location.getLongitude());
                            if (address != null) {
                                inputLocation.setText(address);
                            } else {
                                inputLocation.setText(String.format(Locale.US, "%f, %f", location.getLatitude(), location.getLongitude()));
                            }
                        } else {
                            Toast.makeText(this, "Unable to get location. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        task.addOnFailureListener(this, e -> {
            Toast.makeText(this, "Please enable location services to use this feature.", Toast.LENGTH_SHORT).show();
        });
    }

    private String getAddressFromLocation(double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder addressString = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    addressString.append(address.getAddressLine(i));
                    if (i < address.getMaxAddressLineIndex()) {
                        addressString.append(", ");
                    }
                }
                return addressString.toString();
            }
        } catch (IOException e) {
            Log.e("Geocoder", "Failed to get address: " + e.getMessage());
        }
        return null;
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
            String prediction = classifyImage(this, selectedBitmap);
            Bitmap croppedBitmap = detectAndCropTarget(selectedBitmap);
            if (croppedBitmap != null) {
                selectedBitmap = croppedBitmap;
                imageViewPreview.setImageBitmap(selectedBitmap);
            } else {
                Log.w("SubmitReport", "No object detected for cropping, using original image.");
            }
            float[] embedding = extractEmbedding(this, selectedBitmap);
            Toast.makeText(this, "AI Prediction: " + prediction, Toast.LENGTH_SHORT).show();
            uploadImageToFirebaseStorage(prediction, reportType, location, description, uid, embedding);
        } catch (IOException e) {
            Toast.makeText(this, "Model processing failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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

    private void uploadImageToFirebaseStorage(String predictedClass, String reportType, String location, String description, String uid, float[] embedding) {
        String imageFileName = "images/" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageReference.child(imageFileName);

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            saveReportToDatabase(predictedClass, reportType, location, description, imageUrl, uid, embedding);
                        })
                )
                .addOnFailureListener(e ->
                        Toast.makeText(ReportActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void saveReportToDatabase(String predictedClass, String reportType, String location, String description, String imageUrl, String uid, float[] embedding) {
        String reportId = databaseReference.push().getKey();
        if (reportId == null) {
            Toast.makeText(this, "Failed to generate report ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> report = new HashMap<>();
        report.put("uid", uid);
        report.put("predictedClass", predictedClass);
        report.put("reportType", reportType);
        report.put("location", location);
        report.put("description", description);
        report.put("imageUrl", imageUrl);
        report.put("timestamp", System.currentTimeMillis());

        Map<String, Object> embeddingMap = new HashMap<>();
        for (int i = 0; i < embedding.length; i++) {
            embeddingMap.put("e" + i, embedding[i]);
        }
        report.put("embedding", embeddingMap);

        if (currentColorInput != null) {
            Map<String, Object> colorMap = new HashMap<>();
            colorMap.put("r", currentColorInput[0]);
            colorMap.put("g", currentColorInput[1]);
            colorMap.put("b", currentColorInput[2]);
            report.put("color", colorMap);
            Log.d("SaveReport", "Saving color: " + Arrays.toString(currentColorInput));
        } else {
            Log.e("SaveReport", "currentColorInput is null, color information not saved.");
        }

        databaseReference.child(reportId).setValue(report)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ReportActivity.this, "Report submitted successfully!", Toast.LENGTH_SHORT).show();
                    radioGroupType.clearCheck();
                    inputLocation.setText("");
                    inputDescription.setText("");
                    imageViewPreview.setImageDrawable(null);
                    selectedImageUri = null;
                    selectedBitmap = null;

                    checkSimilarityAndNotify(reportId, predictedClass, reportType, embedding, uid);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ReportActivity.this, "Failed to save report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkSimilarityAndNotify(String reportId, String predictedClass, String reportType, float[] embedding, String currentUserId) {
        DatabaseReference compareRef = database.getReference("user_reports");

        compareRef.orderByChild("predictedClass").equalTo(predictedClass)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        float maxSimilarity = -1f;
                        String matchedReportId = null;
                        float[] matchedColorInput = null;

                        for (DataSnapshot item : snapshot.getChildren()) {
                            if (item.getKey().equals(reportId)) continue;

                            String otherType = item.child("reportType").getValue(String.class);
                            if (otherType != null && !otherType.equals(reportType)) {
                                Map<String, Object> emb = (Map<String, Object>) item.child("embedding").getValue();
                                if (emb == null) continue;

                                float[] otherEmbedding = new float[128];
                                for (int i = 0; i < 128; i++) {
                                    Object val = emb.get("e" + i);
                                    otherEmbedding[i] = val instanceof Number ? ((Number) val).floatValue() : 0f;
                                }

                                float dot = 0f, normA = 0f, normB = 0f;
                                for (int i = 0; i < 128; i++) {
                                    dot += embedding[i] * otherEmbedding[i];
                                    normA += embedding[i] * embedding[i];
                                    normB += otherEmbedding[i] * otherEmbedding[i];
                                }

                                float similarity = (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));

                                Map<String, Object> colorMap = (Map<String, Object>) item.child("color").getValue();
                                if (colorMap != null && currentColorInput != null) {
                                    float[] otherColorInput = new float[3];
                                    otherColorInput[0] = colorMap.get("r") != null ? ((Number) colorMap.get("r")).floatValue() : 0f;
                                    otherColorInput[1] = colorMap.get("g") != null ? ((Number) colorMap.get("g")).floatValue() : 0f;
                                    otherColorInput[2] = colorMap.get("b") != null ? ((Number) colorMap.get("b")).floatValue() : 0f;

                                    float colorDiff = Math.abs(currentColorInput[0] - otherColorInput[0]) +
                                            Math.abs(currentColorInput[1] - otherColorInput[1]) +
                                            Math.abs(currentColorInput[2] - otherColorInput[2]);
                                    Log.d("SimilarityCheck", "Color Difference: " + colorDiff + ", Current Color: " + Arrays.toString(currentColorInput) + ", Other Color: " + Arrays.toString(otherColorInput));

                                    if (colorDiff > 0.25f) {
                                        similarity *= 0.5f;
                                        Log.d("SimilarityCheck", "Adjusted similarity due to color difference: " + similarity);
                                    }

                                    if (similarity > maxSimilarity) {
                                        maxSimilarity = similarity;
                                        matchedReportId = item.getKey();
                                        matchedColorInput = otherColorInput;
                                    }
                                } else {
                                    Log.d("SimilarityCheck", "ColorInput is null - currentColorInput: " + (currentColorInput != null ? Arrays.toString(currentColorInput) : "null") + ", colorMap: " + (colorMap != null ? colorMap.toString() : "null"));
                                    if (similarity > maxSimilarity) {
                                        maxSimilarity = similarity;
                                        matchedReportId = item.getKey();
                                    }
                                }
                            }
                        }

                        if (maxSimilarity > 0.85f && matchedReportId != null) {
                            Log.d("SimilarityCheck", "🟢 Match found: " + matchedReportId + " (" + maxSimilarity + ")");
                            if (matchedColorInput != null) {
                                Log.d("SimilarityCheck", "Matched Color Input: " + Arrays.toString(matchedColorInput));
                            }

                            final String finalMatchedReportId = matchedReportId;

                            database.getReference("user_reports").child(finalMatchedReportId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            String otherUid = snapshot.child("uid").getValue(String.class);
                                            if (otherUid == null) return;

                                            long timestamp = System.currentTimeMillis();
                                            String notificationIdA = database.getReference().push().getKey();
                                            String notificationIdB = database.getReference().push().getKey();

                                            Map<String, Object> notifData = new HashMap<>();
                                            notifData.put("yourReportId", reportId);
                                            notifData.put("matchedReportId", finalMatchedReportId);
                                            notifData.put("timestamp", timestamp);
                                            notifData.put("seen", false);

                                            database.getReference("notifications").child(currentUserId).child(notificationIdA).setValue(notifData);
                                            database.getReference("notifications").child(otherUid).child(notificationIdB).setValue(notifData);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.e("NotifyUsers", "Failed to get matched report owner.");
                                        }
                                    });
                        } else {
                            Log.d("SimilarityCheck", "🔍 No strong match found. Max similarity: " + maxSimilarity);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("SimilarityCheck", "Firebase error: " + error.getMessage());
                    }
                });
    }

    private float[] extractEmbedding(Context context, Bitmap bitmap) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd("embedding_model_rgb_0511.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        MappedByteBuffer modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);

        Interpreter interpreter = new Interpreter(modelBuffer);

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true);

        float[][][][] imageInput = new float[1][224][224][3];
        int[] intValues = new int[224 * 224];
        resized.getPixels(intValues, 0, 224, 0, 0, 224, 224);
        for (int i = 0; i < 224; ++i) {
            for (int j = 0; j < 224; ++j) {
                int pixelValue = intValues[i * 224 + j];
                imageInput[0][i][j][0] = ((pixelValue >> 16) & 0xFF) / 255.0f;
                imageInput[0][i][j][1] = ((pixelValue >> 8) & 0xFF) / 255.0f;
                imageInput[0][i][j][2] = (pixelValue & 0xFF) / 255.0f;
            }
        }

        float[][] colorInput = new float[1][3];
        float r = 0, g = 0, b = 0;
        int foregroundPixels = 0;

        final float white = 1.0f;
        final float tolerance = 0.02f;

        for (int i = 0; i < 224; i++) {
            for (int j = 0; j < 224; j++) {
                float red = imageInput[0][i][j][0];
                float green = imageInput[0][i][j][1];
                float blue = imageInput[0][i][j][2];

                boolean isWhiteBackground =
                        Math.abs(red - white) < tolerance &&
                                Math.abs(green - white) < tolerance &&
                                Math.abs(blue - white) < tolerance;

                if (!isWhiteBackground) {
                    r += red;
                    g += green;
                    b += blue;
                    foregroundPixels++;
                }
            }
        }

        if (foregroundPixels > 0) {
            colorInput[0][0] = r / foregroundPixels;
            colorInput[0][1] = g / foregroundPixels;
            colorInput[0][2] = b / foregroundPixels;
        } else {
            Log.w("ExtractEmbedding", "⚠️ No valid foreground pixels. Fallback to full image average.");
            float fallbackR = 0, fallbackG = 0, fallbackB = 0;
            for (int i = 0; i < 224; i++) {
                for (int j = 0; j < 224; j++) {
                    fallbackR += imageInput[0][i][j][0];
                    fallbackG += imageInput[0][i][j][1];
                    fallbackB += imageInput[0][i][j][2];
                }
            }
            int totalPixels = 224 * 224;
            colorInput[0][0] = fallbackR / totalPixels;
            colorInput[0][1] = fallbackG / totalPixels;
            colorInput[0][2] = fallbackB / totalPixels;
        }

        currentColorInput = colorInput[0];
        Log.d("ExtractEmbedding", "currentColorInput set to: " + Arrays.toString(currentColorInput));

        Object[] inputs = new Object[]{colorInput, imageInput};
        Map<Integer, Object> outputs = new HashMap<>();
        float[][] embeddingOutput = new float[1][128];
        outputs.put(0, embeddingOutput);
        Log.d("TFLiteDebug", "Input tensor count: " + interpreter.getInputTensorCount());
        interpreter.runForMultipleInputsOutputs(inputs, outputs);

        return embeddingOutput[0];
    }

    private void initObjectDetector() {
        try {
            ObjectDetectorOptions options =
                    ObjectDetectorOptions.builder()
                            .setMaxResults(3)
                            .setScoreThreshold(0.5f)
                            .build();
            objectDetector = ObjectDetector.createFromFileAndOptions(
                    this,
                    "ssd_mobilenet_v1_1_metadata_1.tflite",
                    options
            );
            Log.d("ObjectDetector", "Object detector initialized");
        } catch (IOException e) {
            Log.e("ObjectDetector", "Failed to initialize: " + e.getMessage());
        }
    }

    private Bitmap detectAndCropTarget(Bitmap bitmap) {
        if (objectDetector == null) {
            Log.e("ObjectDetector", "Detector not initialized");
            return null;
        }

        TensorImage image = TensorImage.fromBitmap(bitmap);
        List<Detection> results = objectDetector.detect(image);

        if (results.isEmpty()) {
            Log.w("ObjectDetector", "No objects detected in image.");
            return null;
        }

        Detection detection = results.get(0);
        String label = detection.getCategories().get(0).getLabel();
        Log.d("ObjectDetector", "Detected first object: " + label);

        RectF box = detection.getBoundingBox();
        int left = Math.max(0, (int) box.left);
        int top = Math.max(0, (int) box.top);
        int right = Math.min(bitmap.getWidth(), (int) box.right);
        int bottom = Math.min(bitmap.getHeight(), (int) box.bottom);
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imageSegmenter != null) {
            imageSegmenter.close();
        }
        if (objectDetector != null) {
            objectDetector.close();
        }
    }
}