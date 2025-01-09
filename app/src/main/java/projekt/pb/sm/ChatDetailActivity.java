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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate binding and set content view
        binding = ActivityChatDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase instances
        database = FirebaseDatabase.getInstance("https://react-social-a8a4c-default-rtdb.europe-west1.firebasedatabase.app/");
        auth = FirebaseAuth.getInstance();

        // Get data from intent
        senderId = auth.getUid();
        receiverId = getIntent().getStringExtra("userId");
        userName = getIntent().getStringExtra("userName");
        profilePic = getIntent().getStringExtra("profilePic");

        // Set up chat rooms
        senderRoom = senderId + receiverId;
        receiverRoom = receiverId + senderId;

        // Set up UI elements
        binding.userName.setText(userName);
        Picasso.get()
                .load(profilePic)
                .placeholder(R.drawable.avatar)
                .into(binding.profileImage);

        binding.backArrow.setOnClickListener(v -> finish());

        // Initialize RecyclerView and Adapter
        final ArrayList<Message> messageList = new ArrayList<>();
        final ChatAdapter chatAdapter = new ChatAdapter(messageList, this, receiverId);

        // Ensure binding is not null before accessing chatRecyclerView
        if (binding != null && binding.chatRecyclerView != null) {
            binding.chatRecyclerView.setAdapter(chatAdapter);
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            binding.chatRecyclerView.setLayoutManager(layoutManager);
        }

        // Load messages from Firebase
        database.getReference().child("chats")
                .child(senderRoom)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messageList.clear();
                        for (DataSnapshot messageSnap : snapshot.getChildren()) {
                            Message message = messageSnap.getValue(Message.class);
                            if (message != null) {
                                message.setMessageId(messageSnap.getKey());
                                messageList.add(message);
                            }
                        }
                        chatAdapter.notifyDataSetChanged();

                        // Scroll to the bottom when new messages arrive
                        if (messageList.size() > 0 && binding != null) {
                            binding.chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle Firebase error
                    }
                });

        // Send message functionality
        binding.send.setOnClickListener(v -> {
            String messageText = binding.etMessage.getText().toString().trim();

            if (!messageText.isEmpty()) {
                String timestamp = String.valueOf(new Date().getTime());
                Message message = new Message(null, messageText, senderId, timestamp);

                // Clear message input field
                binding.etMessage.setText("");

                // Save message to sender's room
                database.getReference().child("chats")
                        .child(senderRoom)
                        .push()
                        .setValue(message)
                        .addOnSuccessListener(unused -> {
                            // Save message to receiver's room
                            database.getReference().child("chats")
                                    .child(receiverRoom)
                                    .push()
                                    .setValue(message);
                        });
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
        binding = null;
    }
}
