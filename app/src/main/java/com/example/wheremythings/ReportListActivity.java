package com.example.wheremythings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class ReportListActivity extends AppCompatActivity {

    private RecyclerView reportRecyclerView;
    private ReportAdapter reportAdapter;
    private ArrayList<Report> reportList;
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private DatabaseReference databaseReference;
    private FirebaseUser currentUser;
    private ImageButton btnProfileTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_list);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        database = FirebaseDatabase.getInstance("https://wheremything-47fa4-default-rtdb.asia-southeast1.firebasedatabase.app/");
        databaseReference = database.getReference("user_reports");

        reportRecyclerView = findViewById(R.id.reportRecyclerView);
        reportRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        reportList = new ArrayList<>();
        reportAdapter = new ReportAdapter(this, reportList);
        reportRecyclerView.setAdapter(reportAdapter);

        ImageButton btnProfileTitle = findViewById(R.id.btn_profile_title);
        btnProfileTitle.setOnClickListener(v -> {
            Intent intent = new Intent(ReportListActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // Load reports from Firebase
        if (currentUser != null) {
            String currentUserID = currentUser.getUid();
            databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    reportList.clear();
                    for (DataSnapshot reportSnapshot : dataSnapshot.getChildren()) {
                        String reportId = reportSnapshot.getKey();
                        Report report = reportSnapshot.getValue(Report.class);
                        if (report != null) {
                            report.setId(reportId);
                            // Optionally filter reports for the current user
                            if (report.getUid().equals(currentUserID)) {
                                reportList.add(report);
                            }
                        }
                    }
                    Log.d("ReportListActivity", "Size of reportList: " + reportList.size());
                    reportAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(ReportListActivity.this, "Failed to load reports: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Setup navigation buttons
        ImageButton btnProfile = findViewById(R.id.btn_profile);
        ImageButton btnHomePage = findViewById(R.id.btn_homePage);
        ImageButton btnLogout = findViewById(R.id.btn_logout);
        ImageButton btnSubmitReport = findViewById(R.id.btn_submitReport);

        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ReportListActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        btnHomePage.setOnClickListener(v -> {
            Intent intent = new Intent(ReportListActivity.this, MainActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(ReportListActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        btnSubmitReport.setOnClickListener(v -> {
            Intent intent = new Intent(ReportListActivity.this, ReportActivity.class);
            startActivity(intent);
        });

        // Redirect to login if user is not authenticated
        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(ReportListActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}