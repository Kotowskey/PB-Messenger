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

    ActivityChatDetailBinding binding;
    FirebaseDatabase database;
    FirebaseAuth auth;
    String senderId;
    String receiverId;
    String userName;
    String profilePic;
    String senderRoom;
    String receiverRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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

        // Set up UI
        binding.userName.setText(userName);
        Picasso.get()
                .load(profilePic)
                .placeholder(R.drawable.avatar)
                .into(binding.profileImage);

        binding.backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Set up RecyclerView
        final ArrayList<Message> messageList = new ArrayList<>();
        final ChatAdapter chatAdapter = new ChatAdapter(messageList, this, receiverId);
        binding.chatRecyclerView.setAdapter(chatAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.chatRecyclerView.setLayoutManager(layoutManager);

        // Load messages
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

                        // Scroll to bottom when new message arrives
                        if (messageList.size() > 0) {
                            binding.chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error
                    }
                });

        // Send message
        binding.send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = binding.etMessage.getText().toString().trim();

                if (!messageText.isEmpty()) {
                    String timestamp = String.valueOf(new Date().getTime());
                    final Message message = new Message(null, messageText, senderId, timestamp);

                    // Clear input field
                    binding.etMessage.setText("");

                    // Save message to sender's room
                    database.getReference().child("chats")
                            .child(senderRoom)
                            .push()
                            .setValue(message)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    // Save message to receiver's room
                                    database.getReference().child("chats")
                                            .child(receiverRoom)
                                            .push()
                                            .setValue(message);
                                }
                            });
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set user status to online
        if (senderId != null) {
            database.getReference().child("Users").child(senderId).child("status").setValue("online");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Set user status to offline
        if (senderId != null) {
            database.getReference().child("Users").child(senderId).child("status").setValue("offline");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up
        if (senderId != null) {
            database.getReference().child("Users").child(senderId).child("status").setValue("offline");
        }
        binding = null;
    }
}