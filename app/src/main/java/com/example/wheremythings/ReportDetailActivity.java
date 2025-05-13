package com.example.wheremythings;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class ReportDetailActivity extends AppCompatActivity {

    private ImageView reportImage;
    private TextView reportType, predictedClass, location, description;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_detail);

        reportImage = findViewById(R.id.detailReportImage);
        reportType = findViewById(R.id.detailReportType);
        predictedClass = findViewById(R.id.detailPredictedClass);
        location = findViewById(R.id.detailLocation);
        description = findViewById(R.id.detailDescription);

        String reportId = getIntent().getStringExtra("reportId");
        DatabaseReference reportRef = FirebaseDatabase.getInstance("https://wheremything-47fa4-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("user_reports").child(reportId);

        reportRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Report report = dataSnapshot.getValue(Report.class);
                if (report != null) {
                    reportType.setText("Type: " + report.getReportType());
                    predictedClass.setText("Item: " + report.getPredictedClass());
                    location.setText("Location: " + report.getLocation());
                    description.setText("Description: " + report.getDescription());
                    Picasso.get().load(report.getImageUrl()).into(reportImage);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
            }
        });
    }
}