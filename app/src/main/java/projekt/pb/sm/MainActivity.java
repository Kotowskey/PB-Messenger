package projekt.pb.sm;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import projekt.pb.sm.Adapter.FragmentsAdapter;
import projekt.pb.sm.databinding.ActivityMainBinding;
import projekt.pb.sm.models.Message;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private ValueEventListener messageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance("https://react-social-a8a4c-default-rtdb.europe-west1.firebasedatabase.app/");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        binding.viewPager.setAdapter(new FragmentsAdapter(getSupportFragmentManager()));
        binding.tabLayout.setupWithViewPager(binding.viewPager);

        // Initialize notification channel
        NotificationHelper.createNotificationChannel(this);

        // Set up message listener for notifications
        setupMessageListener();
    }

    private void setupMessageListener() {
        String currentUserId = mAuth.getCurrentUser().getUid();

        messageListener = database.getReference().child("chats")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                            String chatId = chatSnapshot.getKey();
                            if (chatId != null && chatId.contains(currentUserId)) {
                                // Get the other user's ID from the chat ID
                                final String otherUserId = chatId.replace(currentUserId, "")
                                        .replace(currentUserId, "");

                                // Listen for the last message
                                chatSnapshot.getChildren().forEach(messageSnapshot -> {
                                    Message message = messageSnapshot.getValue(Message.class);
                                    if (message != null &&
                                            !message.getSenderId().equals(currentUserId) &&
                                            !message.isRead()) {

                                        // Get sender's name and show notification
                                        database.getReference()
                                                .child("Users")
                                                .child(message.getSenderId())
                                                .child("userName")
                                                .get()
                                                .addOnSuccessListener(senderSnapshot -> {
                                                    String senderName = senderSnapshot.getValue(String.class);
                                                    if (senderName != null) {
                                                        NotificationHelper.showMessageNotification(
                                                                MainActivity.this,
                                                                senderName,
                                                                message.getMessage(),
                                                                message.getSenderId()
                                                        );
                                                    }
                                                });
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentId = FirebaseAuth.getInstance().getUid();
        if (currentId != null) {
            HashMap<String, Object> updates = new HashMap<>();
            updates.put("status", "online");
            updates.put("lastSeen", String.valueOf(System.currentTimeMillis()));
            database.getReference().child("Users").child(currentId).updateChildren(updates);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        String currentId = FirebaseAuth.getInstance().getUid();
        if (currentId != null) {
            HashMap<String, Object> updates = new HashMap<>();
            updates.put("status", "offline");
            updates.put("lastSeen", String.valueOf(System.currentTimeMillis()));
            database.getReference().child("Users").child(currentId).updateChildren(updates);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        String currentId = FirebaseAuth.getInstance().getUid();
        if (currentId != null) {
            HashMap<String, Object> updates = new HashMap<>();
            updates.put("status", "offline");
            updates.put("lastSeen", String.valueOf(System.currentTimeMillis()));
            database.getReference().child("Users").child(currentId).updateChildren(updates);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.logout) {
            String currentId = mAuth.getUid();
            if (currentId != null) {
                HashMap<String, Object> updates = new HashMap<>();
                updates.put("status", "offline");
                updates.put("lastSeen", String.valueOf(System.currentTimeMillis()));
                database.getReference().child("Users").child(currentId).updateChildren(updates)
                        .addOnCompleteListener(task -> {
                            mAuth.signOut();
                            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
                            startActivity(intent);
                            finish();
                        });
            } else {
                mAuth.signOut();
                Intent intent = new Intent(MainActivity.this, SignInActivity.class);
                startActivity(intent);
                finish();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            database.getReference().child("chats").removeEventListener(messageListener);
        }
        String currentId = FirebaseAuth.getInstance().getUid();
        if (currentId != null) {
            HashMap<String, Object> updates = new HashMap<>();
            updates.put("status", "offline");
            updates.put("lastSeen", String.valueOf(System.currentTimeMillis()));
            database.getReference().child("Users").child(currentId).updateChildren(updates);
        }
        binding = null;
    }
}