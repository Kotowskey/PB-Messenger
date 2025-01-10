package projekt.pb.sm;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import projekt.pb.sm.Adapter.ChatAdapter;
import projekt.pb.sm.databinding.ActivityChatDetailBinding;
import projekt.pb.sm.models.Message;
import projekt.pb.sm.models.Users;

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
    private boolean isFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = FirebaseDatabase.getInstance("https://react-social-a8a4c-default-rtdb.europe-west1.firebasedatabase.app/");
        auth = FirebaseAuth.getInstance();

        senderId = auth.getUid();
        receiverId = getIntent().getStringExtra("userId");

        if (receiverId == null) {
            finish();
            return;
        }

        NotificationManagerCompat.from(this).cancel(receiverId.hashCode());

        userName = getIntent().getStringExtra("userName");
        profilePic = getIntent().getStringExtra("profilePic");

        senderRoom = senderId + receiverId;
        receiverRoom = receiverId + senderId;

        if (userName == null || profilePic == null) {
            loadUserData();
        } else {
            updateUserInterface();
        }

        binding.backArrow.setOnClickListener(v -> finish());

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.chatRecyclerView.setLayoutManager(layoutManager);

        chatAdapter = new ChatAdapter(messageList, this, receiverId);
        binding.chatRecyclerView.setAdapter(chatAdapter);

        setupStatusListener();
        markMessagesAsRead();
        setupChatListener();

        binding.send.setOnClickListener(v -> sendMessage());
    }

    private void loadUserData() {
        database.getReference().child("Users").child(receiverId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Users user = snapshot.getValue(Users.class);
                        if (user != null) {
                            userName = user.getUserName();
                            profilePic = user.getProfilePic();
                            updateUserInterface();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void updateUserInterface() {
        binding.userName.setText(userName);
        Picasso.get()
                .load(profilePic)
                .placeholder(R.drawable.avatar)
                .into(binding.profileImage);
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

        Calendar lastSeenCal = Calendar.getInstance();
        lastSeenCal.setTimeInMillis(timestamp);

        Calendar nowCal = Calendar.getInstance();
        nowCal.setTimeInMillis(now);

        if (diff < 60 * 1000) { // mniej niż minuta
            return "przed chwilą";
        } else if (diff < 60 * 60 * 1000) { // mniej niż godzina
            long minutes = diff / (60 * 1000);
            return minutes + " min temu";
        } else if (diff < 24 * 60 * 60 * 1000 &&
                lastSeenCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)) {
            // Dzisiaj
            return "dzisiaj " + new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(new Date(timestamp));
        } else if (diff < 48 * 60 * 60 * 1000 &&
                lastSeenCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR) - 1) {
            // Wczoraj
            return "wczoraj " + new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(new Date(timestamp));
        } else if (diff < 7 * 24 * 60 * 60 * 1000) {
            // W tym tygodniu
            return new SimpleDateFormat("EEEE HH:mm", new Locale("pl"))
                    .format(new Date(timestamp));
        } else {
            // Starsze
            return new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
                    .format(new Date(timestamp));
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

                chatAdapter.notifyDataSetChanged();

                if (messageList.size() > 0) {
                    int lastPosition = messageList.size() - 1;
                    if (isFirstLoad) {
                        binding.chatRecyclerView.scrollToPosition(lastPosition);
                        binding.chatRecyclerView.scrollToPosition(lastPosition);
                        isFirstLoad = false;
                    } else {
                        binding.chatRecyclerView.smoothScrollToPosition(lastPosition);
                    }
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

    private void sendMessage() {
        String messageText = binding.etMessage.getText().toString().trim();
        if (messageText.isEmpty()) return;

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
        UserStatusManager.getInstance().onActivityResumed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        UserStatusManager.getInstance().onActivityPaused();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UserStatusManager.getInstance().onActivityPaused();

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