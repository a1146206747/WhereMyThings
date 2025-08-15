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
import com.google.firebase.database.*;

import java.util.ArrayList;

public class PossibleMatchListActivity extends AppCompatActivity {

    private RecyclerView reportRecyclerView;
    private ReportAdapter reportAdapter;
    private ArrayList<Report> reportList = new ArrayList<>();
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_possible_match_list);

        reportRecyclerView = findViewById(R.id.reportRecyclerView);
        reportRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reportAdapter = new ReportAdapter(this, reportList);
        reportRecyclerView.setAdapter(reportAdapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        databaseReference = FirebaseDatabase.getInstance("https://wheremything-47fa4-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("user_reports");

        loadPossibleMatches();

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadPossibleMatches() {
        reportList.clear();
        String myUid = currentUser.getUid();

        databaseReference.orderByChild("uid").equalTo(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot myReportsSnapshot) {
                        Log.d("MatchDebug", "Total my reports: " + myReportsSnapshot.getChildrenCount());

                        for (DataSnapshot mySnap : myReportsSnapshot.getChildren()) {
                            String myType = mySnap.child("reportType").getValue(String.class);
                            String myCategory = mySnap.child("nlpCategory").getValue(String.class);
                            String myColor = mySnap.child("nlpColor").getValue(String.class);
                            String myLoc = mySnap.child("nlpLocation").getValue(String.class);

                            Log.d("MatchDebug", "My Report - Type: " + myType + ", Category: " + myCategory + ", Color: " + myColor + ", Loc: " + myLoc);

                            if (myType == null || myCategory == null) continue;

                            String oppositeType = myType.equalsIgnoreCase("Lost") ? "Found" : "Lost";

                            databaseReference.orderByChild("reportType").equalTo(oppositeType)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            Log.d("MatchDebug", "Found " + snapshot.getChildrenCount() + " opposite reports.");

                                            for (DataSnapshot otherSnap : snapshot.getChildren()) {
                                                String otherUid = otherSnap.child("uid").getValue(String.class);
                                                if (myUid.equals(otherUid)) continue;

                                                String otherCategory = otherSnap.child("nlpCategory").getValue(String.class);
                                                Log.d("MatchDebug", "Other Category: " + otherCategory);

                                                if (otherCategory == null || !otherCategory.equalsIgnoreCase(myCategory))
                                                    continue;

                                                float score = 1.0f;

                                                String otherColor = otherSnap.child("nlpColor").getValue(String.class);
                                                if (myColor != null && otherColor != null) {
                                                    if (myColor.equalsIgnoreCase(otherColor)) {
                                                        score += 0.5f;
                                                    } else if (NlpExtractor.isSimilarColor(myColor.toLowerCase(), otherColor.toLowerCase())) {
                                                        score += 0.3f;
                                                    }
                                                }

                                                String otherLoc = otherSnap.child("nlpLocation").getValue(String.class);
                                                if (myLoc != null && otherLoc != null) {
                                                    String myLocLower = myLoc.toLowerCase();
                                                    String otherLocLower = otherLoc.toLowerCase();
                                                    if (myLocLower.equals(otherLocLower) ||
                                                            myLocLower.contains(otherLocLower) ||
                                                            otherLocLower.contains(myLocLower)) {
                                                        score += 0.2f;
                                                    }
                                                }

                                                Report report = otherSnap.getValue(Report.class);
                                                if (report != null) {
                                                    report.setId(otherSnap.getKey());
                                                    Log.d("MatchDebug", "Matched Report ID: " + report.getId() + " with score: " + score);

                                                    if (!containsReport(reportList, report.getId())) {
                                                        reportList.add(report);
                                                        reportAdapter.notifyDataSetChanged();
                                                    }
                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.e("MatchScan", "Firebase error: " + error.getMessage());
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("MyReports", "Failed to load user reports: " + error.getMessage());
                    }
                });
    }

    private boolean containsReport(ArrayList<Report> list, String id) {
        for (Report r : list) {
            if (r.getId().equals(id)) return true;
        }
        return false;
    }
}
