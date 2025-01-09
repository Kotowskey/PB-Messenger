package projekt.pb.sm;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import projekt.pb.sm.Adapter.FragmentsAdapter;
import projekt.pb.sm.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;

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