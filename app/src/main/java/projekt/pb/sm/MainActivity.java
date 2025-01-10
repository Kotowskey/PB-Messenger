package projekt.pb.sm;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.List;
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
                                // Pobierz tylko ostatnią wiadomość
                                DataSnapshot lastMessageSnap = null;
                                for (DataSnapshot messageSnapshot : chatSnapshot.getChildren()) {
                                    lastMessageSnap = messageSnapshot;
                                }

                                if (lastMessageSnap != null) {
                                    Message lastMessage = lastMessageSnap.getValue(Message.class);
                                    if (lastMessage != null &&
                                            !lastMessage.getSenderId().equals(currentUserId) &&
                                            !lastMessage.isRead()) {

                                        // Sprawdź czy użytkownik nie jest aktualnie w chacie z tą osobą
                                        String otherUserId = lastMessage.getSenderId();
                                        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                                        List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(1);

                                        boolean isInChatDetail = false;
                                        if (!tasks.isEmpty()) {
                                            ComponentName topActivity = tasks.get(0).topActivity;
                                            isInChatDetail = topActivity != null &&
                                                    topActivity.getClassName().equals(ChatDetailActivity.class.getName());
                                        }

                                        // Nie pokazuj powiadomienia, jeśli użytkownik jest w ChatDetailActivity
                                        if (!isInChatDetail) {
                                            // Pokaż powiadomienie
                                            final Message finalLastMessage = lastMessage;
                                            database.getReference()
                                                    .child("Users")
                                                    .child(lastMessage.getSenderId())
                                                    .child("userName")
                                                    .get()
                                                    .addOnSuccessListener(senderSnapshot -> {
                                                        String senderName = senderSnapshot.getValue(String.class);
                                                        if (senderName != null) {
                                                            NotificationHelper.showMessageNotification(
                                                                    MainActivity.this,
                                                                    senderName,
                                                                    finalLastMessage.getMessage(),
                                                                    finalLastMessage.getSenderId()
                                                            );
                                                        }
                                                    });
                                        }
                                    }
                                }
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