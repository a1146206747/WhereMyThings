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

import com.google.ai.litert.task.vision.ImageSegmenter;
import com.google.ai.litert.task.vision.ImageSegmenterOptions;
import com.google.ai.litert.task.vision.ImageProcessingOptions;
import com.google.ai.litert.task.vision.segmenter.CategoryMask;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import org.tensorflow.lite.DataType;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
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

    private float[] currentColorInput;
    private ImageSegmenter imageSegmenter;

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

        // 初始化 ImageSegmenter（TensorFlow Lite）
        try {
            ImageSegmenterOptions options = ImageSegmenterOptions.builder()
                    .setModelAssetPath("deeplabv3.tflite")
                    .build();
            imageSegmenter = ImageSegmenter.createFromOptions(this, options);
            Log.d("ImageSegmenter", "ImageSegmenter initialized successfully");
        } catch (Exception e) {
            Log.e("ImageSegmenter", "Failed to initialize ImageSegmenter: " + e.getMessage());
        }

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
            // 背景移除
            Bitmap processedBitmap = removeBackground(selectedBitmap);
            if (processedBitmap == null) {
                Toast.makeText(ReportActivity.this, "Background removal failed", Toast.LENGTH_SHORT).show();
                return;
            }
            selectedBitmap = processedBitmap;
            imageViewPreview.setImageBitmap(selectedBitmap); // 更新預覽

            String prediction = classifyImage(ReportActivity.this, selectedBitmap);
            float[] embedding = extractEmbedding(ReportActivity.this, selectedBitmap);
            Toast.makeText(ReportActivity.this, "AI 判斷結果: " + prediction, Toast.LENGTH_SHORT).show();

            uploadImageToFirebaseStorage(prediction, reportType, location, description, uid, embedding);

        } catch (IOException e) {
            Toast.makeText(ReportActivity.this, "模型處理失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap removeBackground(Bitmap bitmap) {
        if (imageSegmenter == null) {
            Log.e("ImageSegmenter", "ImageSegmenter is not initialized");
            return null;
        }

        try {
            // 預處理圖像
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
            TensorImage tensorImage = TensorImage.fromBitmap(resized);
            ImageProcessingOptions processingOptions = ImageProcessingOptions.builder().build();

            // 執行分割
            ImageSegmenter.ImageSegmentationResult result = imageSegmenter.segment(tensorImage, processingOptions);
            CategoryMask categoryMask = result.getCategoryMask();

            // 應用分割結果
            Bitmap segmentedBitmap = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < 224; x++) {
                for (int y = 0; y < 224; y++) {
                    int categoryIndex = categoryMask.getCategoryIndex(x, y);
                    if (categoryIndex == 1) { // 假設 1 表示前景（根據模型輸出調整）
                        segmentedBitmap.setPixel(x, y, resized.getPixel(x, y));
                    } else {
                        segmentedBitmap.setPixel(x, y, 0x00000000); // 透明背景
                    }
                }
            }

            Log.d("BackgroundRemoval", "Processed bitmap created with size: " + segmentedBitmap.getWidth() + "x" + segmentedBitmap.getHeight());
            return segmentedBitmap;

        } catch (Exception e) {
            Log.e("BackgroundRemoval", "Failed to remove background: " + e.getMessage());
            return null;
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
        for (int i = 0; i < 224; i++) {
            for (int j = 0; j < 224; j++) {
                int pixelValue = resized.getPixel(j, i);
                if ((pixelValue & 0xFF000000) != 0) { // 忽略透明像素
                    r += imageInput[0][i][j][0];
                    g += imageInput[0][i][j][1];
                    b += imageInput[0][i][j][2];
                    foregroundPixels++;
                }
            }
        }
        colorInput[0][0] = foregroundPixels > 0 ? r / foregroundPixels : 0;
        colorInput[0][1] = foregroundPixels > 0 ? g / foregroundPixels : 0;
        colorInput[0][2] = foregroundPixels > 0 ? b / foregroundPixels : 0;

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imageSegmenter != null) {
            imageSegmenter.close();
        }
    }
}