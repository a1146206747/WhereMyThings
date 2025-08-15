package com.example.wheremythings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> commentList;
    private Context context;
    private String reportId;

    public CommentAdapter(Context context, List<Comment> commentList, String reportId) {
        this.context = context;
        this.commentList = commentList;
        this.reportId = reportId;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        holder.commentUserAndText.setText(comment.getUsername() + ": " + comment.getText());
        holder.commentTimestamp.setText(formatTimestamp(comment.getTimestamp()));

        List<Reply> replyList = new ArrayList<>();
        ReplyAdapter replyAdapter = new ReplyAdapter(replyList);
        holder.replyRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        holder.replyRecyclerView.setAdapter(replyAdapter);

        DatabaseReference replyRef = FirebaseDatabase.getInstance("https://wheremything-47fa4-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("user_comments")
                .child(reportId)
                .child(comment.getId())
                .child("replies");

        replyRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                replyList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Reply reply = snap.getValue(Reply.class);
                    if (reply != null && reply.getText() != null) {
                        replyList.add(reply);
                    }
                }

                Collections.sort(replyList, (r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
                replyAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        holder.btnReply.setOnClickListener(v -> {
            boolean isVisible = holder.replyInput.getVisibility() == View.VISIBLE;
            holder.replyInput.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            holder.btnSubmitReply.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        });

        holder.btnSubmitReply.setOnClickListener(v -> {
            String replyText = holder.replyInput.getText().toString().trim();
            if (!replyText.isEmpty()) {
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                String replyId = replyRef.push().getKey();
                long timestamp = System.currentTimeMillis();

                DatabaseReference userRef = FirebaseDatabase.getInstance("https://wheremything-47fa4-default-rtdb.asia-southeast1.firebasedatabase.app/")
                        .getReference("users").child(userId);

                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String username = snapshot.child("name").getValue(String.class);
                        if (username == null) username = "Anonymous";

                        Reply reply = new Reply(replyId, userId, username, replyText, timestamp);
                        replyRef.child(replyId).setValue(reply);
                        holder.replyInput.setText("");
                        holder.replyInput.setVisibility(View.GONE);
                        holder.btnSubmitReply.setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(context, "Failed to send reply", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView commentUserAndText, commentTimestamp;
        Button btnReply, btnSubmitReply;
        EditText replyInput;
        RecyclerView replyRecyclerView;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            commentUserAndText = itemView.findViewById(R.id.commentUserAndText);
            commentTimestamp = itemView.findViewById(R.id.commentTimestamp);
            btnReply = itemView.findViewById(R.id.btnReply);
            btnSubmitReply = itemView.findViewById(R.id.btnSubmitReply);
            replyInput = itemView.findViewById(R.id.replyInput);
            replyRecyclerView = itemView.findViewById(R.id.replyRecyclerView);
        }
    }

    private String formatTimestamp(long time) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(time));
    }
}
