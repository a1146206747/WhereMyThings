package com.example.wheremythings;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections; // Import Collections

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView notificationsRecyclerView;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notificationList;
    private DatabaseReference notificationsRef;
    private ImageButton backButtonNotifications;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification); // Assuming you have a layout file named activity_notification.xml

        notificationsRecyclerView = findViewById(R.id.recyclerViewNotifications); // Assuming your RecyclerView has this ID
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        backButtonNotifications = findViewById(R.id.backButtonNotifications);
        backButtonNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        notificationList = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(this, notificationList);
        notificationsRecyclerView.setAdapter(notificationAdapter);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            notificationsRef = FirebaseDatabase.getInstance().getReference("notifications").child(currentUserId);
            fetchNotifications();
        } else {
            // Handle the case where the user is not logged in
            // You might want to redirect to the login screen
        }
    }

    private void fetchNotifications() {
        notificationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                notificationList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Assuming Notification class has a constructor or setters to handle the data structure in Firebase
                    Notification notification = snapshot.getValue(Notification.class);
                    if (notification != null) {
                        notificationList.add(notification);
                    }
                }
                notificationAdapter.notifyDataSetChanged();
            }



            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("NotificationActivity", "loadNotifications:onCancelled", databaseError.toException());
                // Handle database error
            }
        });
    }
}