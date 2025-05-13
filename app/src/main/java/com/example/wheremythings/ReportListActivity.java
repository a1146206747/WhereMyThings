package com.example.wheremythings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ReportListActivity extends AppCompatActivity {

    private RecyclerView reportRecyclerView;
    private ReportAdapter reportAdapter;
    private ArrayList<Report> reportList = new ArrayList<>();
    private ArrayList<Report> fullReportList = new ArrayList<>();
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;
    private Button btnLostReports, btnFoundReports, btnSubmitReport;
    private SearchView searchView;
    private ArrayList<Report> currentFilteredList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_list);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        databaseReference = FirebaseDatabase.getInstance("https://wheremything-47fa4-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("user_reports");

        reportRecyclerView = findViewById(R.id.reportRecyclerView);
        reportRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reportAdapter = new ReportAdapter(this, reportList);
        reportRecyclerView.setAdapter(reportAdapter);

        // Buttons
        btnLostReports = findViewById(R.id.btn_lost_reports);
        btnFoundReports = findViewById(R.id.btn_found_reports);
        btnSubmitReport = findViewById(R.id.btn_submit_report);

        // Title bar profile button
        ImageButton btnProfileTitle = findViewById(R.id.btn_profile_title);
        btnProfileTitle.setOnClickListener(v -> {
            startActivity(new Intent(ReportListActivity.this, ProfileActivity.class));
        });

        // Submit report button
        btnSubmitReport.setOnClickListener(v -> {
            startActivity(new Intent(ReportListActivity.this, ReportActivity.class));
        });

        // Lost reports
        btnLostReports.setOnClickListener(v -> {
            filterReportsByType("lost");
            highlightSelectedButton(btnLostReports);
        });

        // Found reports
        btnFoundReports.setOnClickListener(v -> {
            filterReportsByType("found");
            highlightSelectedButton(btnFoundReports);
        });

        // Redirect to login if not signed in
        if (currentUser == null) {
            startActivity(new Intent(ReportListActivity.this, MainActivity.class));
            finish();
            return;
        }
        searchView = findViewById(R.id.searchView);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterReportsByKeyword(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterReportsByKeyword(newText);
                return true;
            }
        });

        // Load all reports from Firebase
        loadAllReports();
    }

    private void loadAllReports() {
        String currentUserID = currentUser.getUid();

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullReportList.clear();
                for (DataSnapshot reportSnapshot : snapshot.getChildren()) {
                    Report report = reportSnapshot.getValue(Report.class);
                    if (report != null) {
                        report.setId(reportSnapshot.getKey());
                        fullReportList.add(report);
                    }
                }

                // 預設顯示 lost 報告並高亮按鈕
                filterReportsByType("lost");
                highlightSelectedButton(btnLostReports);

                Log.d("ReportListActivity", "Total reports loaded: " + fullReportList.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ReportListActivity.this, "Failed to load reports: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterReportsByType(String type) {
        currentFilteredList.clear();
        for (Report report : fullReportList) {
            if (report.getReportType() != null && report.getReportType().equalsIgnoreCase(type)) {
                currentFilteredList.add(report);
            }
        }

        // 預設不加搜尋關鍵字時顯示全部分類內的項目
        reportList.clear();
        reportList.addAll(currentFilteredList);
        reportAdapter.notifyDataSetChanged();

        Log.d("ReportListActivity", "Filtered " + type + " reports: " + currentFilteredList.size());
    }


    private void highlightSelectedButton(Button selectedButton) {
        // Reset all buttons to default color
        btnLostReports.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        btnFoundReports.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));

        // Highlight selected one
        selectedButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
    }
    private void filterReportsByKeyword(String keyword) {
        reportList.clear();
        keyword = keyword.toLowerCase();

        for (Report report : currentFilteredList) {
            if ((report.getDescription() != null && report.getDescription().toLowerCase().contains(keyword)) ||
                    (report.getLocation() != null && report.getLocation().toLowerCase().contains(keyword)) ||
                    (report.getPredictedClass() != null && report.getPredictedClass().toLowerCase().contains(keyword))) {
                reportList.add(report);
            }
        }

        reportAdapter.notifyDataSetChanged();
    }


}
