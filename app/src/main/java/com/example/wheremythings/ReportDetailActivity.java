package com.example.wheremythings;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ReportDetailActivity extends AppCompatActivity {

    private ImageView reportImage;
    private TextView reportType, predictedClass, location, description;

    private EditText commentInput;
    private Button btnSubmitComment;
    private RecyclerView commentRecyclerView;
    private CommentAdapter commentAdapter;
    private List<Comment> commentList = new ArrayList<>();
    private DatabaseReference commentRef;
    private ImageButton backButtonReportDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_detail);

        backButtonReportDetail = findViewById(R.id.backButtonReportDetail);
        backButtonReportDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        reportImage = findViewById(R.id.detailReportImage);
        reportType = findViewById(R.id.detailReportType);
        predictedClass = findViewById(R.id.detailPredictedClass);
        location = findViewById(R.id.detailLocation);
        description = findViewById(R.id.detailDescription);

        commentInput = findViewById(R.id.commentInput);
        btnSubmitComment = findViewById(R.id.btnSubmitComment);
        commentRecyclerView = findViewById(R.id.commentRecyclerView);

        String reportId = getIntent().getStringExtra("reportId");

        commentAdapter = new CommentAdapter(this, commentList, reportId);
        commentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentRecyclerView.setAdapter(commentAdapter);


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
                Toast.makeText(ReportDetailActivity.this, "Failed to load report", Toast.LENGTH_SHORT).show();
            }
        });

        commentRef = FirebaseDatabase.getInstance("https://wheremything-47fa4-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("user_comments").child(reportId);

        btnSubmitComment.setOnClickListener(v -> {
            String text = commentInput.getText().toString().trim();
            if (!text.isEmpty()) {
                String commentId = commentRef.push().getKey();
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                long timestamp = System.currentTimeMillis();

                // 取得使用者名稱
                DatabaseReference userRef = FirebaseDatabase.getInstance("https://wheremything-47fa4-default-rtdb.asia-southeast1.firebasedatabase.app/")
                        .getReference("users").child(userId);

                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String username = snapshot.child("name").getValue(String.class);
                        if (username == null) username = "Anonymous";

                        Comment comment = new Comment(commentId, userId, username, text, timestamp);
                        commentRef.child(commentId).setValue(comment);
                        commentInput.setText(""); // 清空輸入框
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ReportDetailActivity.this, "Failed to get user name", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });


        commentRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                commentList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Comment comment = snap.getValue(Comment.class);
                    if (comment != null && comment.getText() != null) {
                        commentList.add(comment);
                    }
                }

                Collections.sort(commentList, (c1, c2) -> Long.compare(c2.getTimestamp(), c1.getTimestamp()));


                commentAdapter.notifyDataSetChanged();

                commentRecyclerView.scrollToPosition(0);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ReportDetailActivity.this, "Failed to load comments", Toast.LENGTH_SHORT).show();
            }
        });

    }
}
