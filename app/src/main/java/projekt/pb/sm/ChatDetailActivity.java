package projekt.pb.sm;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import projekt.pb.sm.Adapter.ChatAdapter;
import projekt.pb.sm.databinding.ActivityChatDetailBinding;
import projekt.pb.sm.models.Message;

public class ChatDetailActivity extends AppCompatActivity {

    ActivityChatDetailBinding binding;
    FirebaseDatabase database;
    FirebaseAuth auth;
    String senderId;
    String receiverId;
    String userName;
    String profilePic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().hide();  // Hide default action bar

        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();

        // Get user details
        senderId = auth.getUid();
        receiverId = getIntent().getStringExtra("userId");
        userName = getIntent().getStringExtra("userName");
        profilePic = getIntent().getStringExtra("profilePic");

        // Set user details in toolbar
        binding.userName.setText(userName);
        Picasso.get().load(profilePic).placeholder(R.drawable.avatar).into(binding.profileImage);

        binding.backArrow.setOnClickListener(view -> finish());

        // Initialize message list and adapter
        final ArrayList<Message> messageList = new ArrayList<>();
        final ChatAdapter chatAdapter = new ChatAdapter(messageList, this, receiverId);
        binding.chatRecyclerView.setAdapter(chatAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.chatRecyclerView.setLayoutManager(layoutManager);

        final String senderRoom = senderId + receiverId;
        final String receiverRoom = receiverId + senderId;

        // Load messages
        database.getReference().child("chats")
                .child(senderRoom)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messageList.clear();
                        for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                            Message message = snapshot1.getValue(Message.class);
                            if (message != null) {
                                message.setMessageId(snapshot1.getKey());
                                messageList.add(message);
                            }
                        }
                        chatAdapter.notifyDataSetChanged();

                        // Scroll to last message
                        if (!messageList.isEmpty()) {
                            binding.chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ChatDetailActivity.this,
                                "Error loading messages: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        // Send message
        binding.send.setOnClickListener(view -> {
            String messageText = binding.etMessage.getText().toString().trim();
            if (!messageText.isEmpty()) {
                Date date = new Date();
                String timestamp = String.valueOf(date.getTime());

                Message message = new Message(null, messageText, senderId, timestamp);

                // Clear input field
                binding.etMessage.setText("");

                String messageKey = database.getReference().child("chats")
                        .child(senderRoom)
                        .push().getKey();

                HashMap<String, Object> lastMessageObj = new HashMap<>();
                lastMessageObj.put("lastMessage", messageText);
                lastMessageObj.put("lastMessageTime", timestamp);

                database.getReference().child("chats").child(senderRoom).child(messageKey)
                        .setValue(message).addOnSuccessListener(unused -> {
                            database.getReference().child("chats").child(receiverRoom).child(messageKey)
                                    .setValue(message).addOnSuccessListener(aVoid -> {
                                        // Update last message for both users
                                        database.getReference().child("Users").child(senderId)
                                                .updateChildren(lastMessageObj);
                                        database.getReference().child("Users").child(receiverId)
                                                .updateChildren(lastMessageObj);
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(ChatDetailActivity.this,
                                            "Error sending message to receiver",
                                            Toast.LENGTH_SHORT).show());
                        })
                        .addOnFailureListener(e -> Toast.makeText(ChatDetailActivity.this,
                                "Error sending message",
                                Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}