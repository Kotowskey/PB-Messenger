package projekt.pb.sm;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.widget.Toolbar;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import projekt.pb.sm.Adapter.FragmentsAdapter;
import projekt.pb.sm.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        mAuth = FirebaseAuth.getInstance();

        // Konfiguracja Toolbara
        setSupportActionBar(binding.toolbar);

        // Konfiguracja ViewPager i TabLayout
        binding.viewPager.setAdapter(new FragmentsAdapter(getSupportFragmentManager()));
        binding.tabLayout.setupWithViewPager(binding.viewPager);
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
            Toast.makeText(this, "Settings is clicked", Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.groupChat) {
            Toast.makeText(this, "Group Chat is clicked", Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.logout) {
            mAuth.signOut();
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
