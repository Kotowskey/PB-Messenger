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
import java.util.HashMap;
import java.util.Map;

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
    private ValueEventListener statusListener;
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

        // Nasłuchiwanie statusu odbiorcy
        setupStatusListener();

        // Mark messages as read when entering chat
        markMessagesAsRead();

        // Set up chat listener
        setupChatListener();

        binding.send.setOnClickListener(v -> {
            if (binding == null) return;

            String messageText = binding.etMessage.getText().toString().trim();

            if (!messageText.isEmpty()) {
                String timestamp = String.valueOf(new Date().getTime());
                Message message = new Message();
                message.setMessage(messageText);
                message.setSenderId(senderId);
                message.setTimestamp(timestamp);
                message.setRead(false);

                binding.etMessage.setText("");

                String messageId = database.getReference().child("chats")
                        .child(senderRoom)
                        .push().getKey();

                if (messageId != null) {
                    message.setMessageId(messageId);

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("/chats/" + senderRoom + "/" + messageId, message);
                    updates.put("/chats/" + receiverRoom + "/" + messageId, message);

                    database.getReference().updateChildren(updates);
                }
            }
        });
    }

    private void setupStatusListener() {
        statusListener = database.getReference()
                .child("Users")
                .child(receiverId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String status = snapshot.child("status").getValue(String.class);
                            if ("online".equals(status)) {
                                binding.statusView.setVisibility(View.VISIBLE);
                                binding.statusText.setText("online");
                            } else {
                                binding.statusView.setVisibility(View.GONE);
                                String lastSeen = snapshot.child("lastSeen").getValue(String.class);
                                if (lastSeen != null) {
                                    try {
                                        long lastSeenTime = Long.parseLong(lastSeen);
                                        String formattedTime = formatLastSeen(lastSeenTime);
                                        binding.statusText.setText("ostatnio widziany " + formattedTime);
                                    } catch (NumberFormatException e) {
                                        binding.statusText.setText("offline");
                                    }
                                } else {
                                    binding.statusText.setText("offline");
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private String formatLastSeen(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 24 * 60 * 60 * 1000) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } else {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
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
                                database.getReference().child("chats")
                                        .child(senderRoom)
                                        .child(messageSnap.getKey())
                                        .child("read")
                                        .setValue(true);

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
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (senderId != null) {
            HashMap<String, Object> updates = new HashMap<>();
            updates.put("status", "online");
            updates.put("lastSeen", String.valueOf(System.currentTimeMillis()));
            database.getReference().child("Users").child(senderId).updateChildren(updates);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (senderId != null) {
            HashMap<String, Object> updates = new HashMap<>();
            updates.put("status", "offline");
            updates.put("lastSeen", String.valueOf(System.currentTimeMillis()));
            database.getReference().child("Users").child(senderId).updateChildren(updates);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (senderId != null) {
            HashMap<String, Object> updates = new HashMap<>();
            updates.put("status", "offline");
            updates.put("lastSeen", String.valueOf(System.currentTimeMillis()));
            database.getReference().child("Users").child(senderId).updateChildren(updates);
        }

        if (chatListener != null) {
            database.getReference().child("chats")
                    .child(senderRoom)
                    .removeEventListener(chatListener);
        }

        if (statusListener != null) {
            database.getReference()
                    .child("Users")
                    .child(receiverId)
                    .removeEventListener(statusListener);
        }

        binding = null;
    }
}