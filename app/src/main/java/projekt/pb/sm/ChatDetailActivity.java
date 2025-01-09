package projekt.pb.sm;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.Date;
import projekt.pb.sm.Adapter.ChatAdapter;
import projekt.pb.sm.databinding.ActivityChatDetailBinding;
import projekt.pb.sm.models.Message;

public class ChatDetailActivity extends AppCompatActivity {

    private ActivityChatDetailBinding binding;
    private FirebaseDatabase database;
    private FirebaseAuth auth;
    private String senderId;
    private String receiverId;
    private String userName;
    private String profilePic;
    private String senderRoom;
    private String receiverRoom;
    private ValueEventListener chatListener;
    private final ArrayList<Message> messageList = new ArrayList<>();
    private ChatAdapter chatAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityChatDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = FirebaseDatabase.getInstance("https://react-social-a8a4c-default-rtdb.europe-west1.firebasedatabase.app/");
        auth = FirebaseAuth.getInstance();

        senderId = auth.getUid();
        receiverId = getIntent().getStringExtra("userId");
        userName = getIntent().getStringExtra("userName");
        profilePic = getIntent().getStringExtra("profilePic");

        senderRoom = senderId + receiverId;
        receiverRoom = receiverId + senderId;

        binding.userName.setText(userName);
        Picasso.get()
                .load(profilePic)
                .placeholder(R.drawable.avatar)
                .into(binding.profileImage);

        binding.backArrow.setOnClickListener(v -> finish());

        chatAdapter = new ChatAdapter(messageList, this, receiverId);
        binding.chatRecyclerView.setAdapter(chatAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.chatRecyclerView.setLayoutManager(layoutManager);

        // Mark messages as read when entering chat
        markMessagesAsRead();

        // Set up chat listener
        setupChatListener();

        binding.send.setOnClickListener(v -> {
            if (binding == null) return;

            String messageText = binding.etMessage.getText().toString().trim();

            if (!messageText.isEmpty()) {
                String timestamp = String.valueOf(new Date().getTime());
                Message message = new Message(null, messageText, senderId, timestamp);
                message.setRead(false);

                binding.etMessage.setText("");

                database.getReference().child("chats")
                        .child(senderRoom)
                        .push()
                        .setValue(message)
                        .addOnSuccessListener(unused -> {
                            database.getReference().child("chats")
                                    .child(receiverRoom)
                                    .push()
                                    .setValue(message);
                        });
            }
        });
    }

    private void setupChatListener() {
        chatListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;

                messageList.clear();
                for (DataSnapshot messageSnap : snapshot.getChildren()) {
                    Message message = messageSnap.getValue(Message.class);
                    if (message != null) {
                        message.setMessageId(messageSnap.getKey());
                        messageList.add(message);
                    }
                }
                if (chatAdapter != null) {
                    chatAdapter.notifyDataSetChanged();
                }

                if (messageList.size() > 0 && binding != null && binding.chatRecyclerView != null) {
                    binding.chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle Firebase error
            }
        };

        database.getReference().child("chats")
                .child(senderRoom)
                .addValueEventListener(chatListener);
    }

    private void markMessagesAsRead() {
        database.getReference().child("chats")
                .child(senderRoom)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot messageSnap : snapshot.getChildren()) {
                            Message message = messageSnap.getValue(Message.class);
                            if (message != null && !message.isRead() && !message.getSenderId().equals(senderId)) {
                                // Update in sender's room
                                database.getReference().child("chats")
                                        .child(senderRoom)
                                        .child(messageSnap.getKey())
                                        .child("read")
                                        .setValue(true);

                                // Update in receiver's room
                                database.getReference().child("chats")
                                        .child(receiverRoom)
                                        .child(messageSnap.getKey())
                                        .child("read")
                                        .setValue(true);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (senderId != null) {
            database.getReference().child("Users").child(senderId).child("status").setValue("online");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (senderId != null) {
            database.getReference().child("Users").child(senderId).child("status").setValue("offline");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (senderId != null) {
            database.getReference().child("Users").child(senderId).child("status").setValue("offline");
        }

        // Remove chat listener
        if (chatListener != null) {
            database.getReference().child("chats")
                    .child(senderRoom)
                    .removeEventListener(chatListener);
        }

        binding = null;
    }
}