package projekt.pb.sm.Fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import projekt.pb.sm.R;
import projekt.pb.sm.models.Users;
import projekt.pb.sm.databinding.FragmentStatusBinding;

public class StatusFragment extends Fragment {

    private FragmentStatusBinding binding;
    private FirebaseAuth auth;
    private FirebaseDatabase database;

    public StatusFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentStatusBinding.inflate(inflater, container, false);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        // Load user status
        database.getReference().child("Users").child(auth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Users user = snapshot.getValue(Users.class);
                        if (user != null) {
                            Picasso.get()
                                    .load(user.getProfilePic())
                                    .placeholder(R.drawable.avatar)
                                    .into(binding.profileImage);
                            binding.userStatus.setText(user.getStatus());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Error loading status", Toast.LENGTH_SHORT).show();
                    }
                });

        // Handle status update button click
        binding.updateStatusBtn.setOnClickListener(v -> {
            String newStatus = binding.statusInput.getText().toString().trim();
            if (!newStatus.isEmpty()) {
                database.getReference().child("Users")
                        .child(auth.getUid())
                        .child("status")
                        .setValue(newStatus)
                        .addOnSuccessListener(unused -> Toast.makeText(getContext(), "Status updated", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update status", Toast.LENGTH_SHORT).show());
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}