package projekt.pb.sm;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;
import projekt.pb.sm.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance("https://react-social-a8a4c-default-rtdb.europe-west1.firebasedatabase.app/");

        // Set up toolbar
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Load current profile image
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            database.getReference().child("Users").child(currentUser.getUid()).get()
                    .addOnSuccessListener(dataSnapshot -> {
                        if (dataSnapshot.exists()) {
                            String profilePicUrl = dataSnapshot.child("profilePic").getValue(String.class);
                            if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                                binding.profileImageUrl.setText(profilePicUrl);
                                Picasso.get()
                                        .load(profilePicUrl)
                                        .placeholder(R.drawable.avatar)
                                        .into(binding.profileImage);
                            }
                        }
                    });
        }

        // Update avatar button click handler
        binding.btnUpdateAvatar.setOnClickListener(v -> updateProfilePicture());

        // Delete account button click handler
        binding.btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void updateProfilePicture() {
        String imageUrl = binding.profileImageUrl.getText().toString().trim();
        if (imageUrl.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Błąd")
                    .setMessage("Proszę podać URL zdjęcia")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // Show loading dialog
        MaterialAlertDialogBuilder loadingDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Aktualizacja")
                .setMessage("Trwa aktualizacja zdjęcia profilowego...")
                .setCancelable(false);
        AlertDialog dialog = loadingDialog.show();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // First try to load the image to verify URL
            Picasso.get()
                    .load(imageUrl)
                    .into(binding.profileImage, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            // Image loaded successfully, now update in database
                            database.getReference()
                                    .child("Users")
                                    .child(currentUser.getUid())
                                    .child("profilePic")
                                    .setValue(imageUrl)
                                    .addOnSuccessListener(unused -> {
                                        dialog.dismiss();
                                        new MaterialAlertDialogBuilder(SettingsActivity.this)
                                                .setTitle("Sukces")
                                                .setMessage("Zdjęcie profilowe zostało pomyślnie zaktualizowane")
                                                .setPositiveButton("OK", null)
                                                .show();
                                    })
                                    .addOnFailureListener(e -> {
                                        dialog.dismiss();
                                        new MaterialAlertDialogBuilder(SettingsActivity.this)
                                                .setTitle("Błąd")
                                                .setMessage("Nie udało się zaktualizować zdjęcia: " + e.getMessage())
                                                .setPositiveButton("OK", null)
                                                .show();
                                    });
                        }

                        @Override
                        public void onError(Exception e) {
                            dialog.dismiss();
                            new MaterialAlertDialogBuilder(SettingsActivity.this)
                                    .setTitle("Błąd")
                                    .setMessage("Nieprawidłowy URL obrazu lub problem z jego pobraniem")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    });
        }
    }

    private void showDeleteAccountDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> showPasswordConfirmationDialog())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPasswordConfirmationDialog() {
        // Inflate custom layout for password input
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_password, null);
        TextInputEditText passwordInput = dialogView.findViewById(R.id.passwordInput);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Password")
                .setView(dialogView)
                .setMessage("Please enter your password to confirm account deletion")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String password = passwordInput.getText().toString();
                    if (!password.isEmpty()) {
                        reauthenticateAndDelete(password);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void reauthenticateAndDelete(String password) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            // Create credentials with current email and provided password
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

            // Reauthenticate
            user.reauthenticate(credential)
                    .addOnCompleteListener(reauthTask -> {
                        if (reauthTask.isSuccessful()) {
                            // After successful reauthentication, proceed with deletion
                            deleteUserData(user);
                        } else {
                            new MaterialAlertDialogBuilder(this)
                                    .setTitle("Error")
                                    .setMessage("Invalid password. Please try again.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    });
        }
    }

    private void deleteUserData(FirebaseUser user) {
        String userId = user.getUid();

        // First delete user data from database
        database.getReference().child("Users").child(userId)
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Also delete all user's chats
                        deleteUserChats(userId, user);
                    } else {
                        showErrorDialog("Failed to delete user data");
                    }
                });
    }

    private void deleteUserChats(String userId, FirebaseUser user) {
        // Delete all chats where user is involved
        database.getReference().child("chats").get()
                .addOnSuccessListener(snapshot -> {
                    // Delete all chat rooms that contain this user's ID
                    for (String key : snapshot.getValue() != null ? ((java.util.HashMap<String, Object>) snapshot.getValue()).keySet() : new java.util.ArrayList<String>()) {
                        if (key.contains(userId)) {
                            database.getReference().child("chats").child(key).removeValue();
                        }
                    }
                    // Finally delete the user authentication
                    deleteUserAuthentication(user);
                })
                .addOnFailureListener(e -> {
                    showErrorDialog("Failed to delete user chats");
                });
    }

    private void deleteUserAuthentication(FirebaseUser user) {
        user.delete()
                .addOnCompleteListener(deleteTask -> {
                    if (deleteTask.isSuccessful()) {
                        // Redirect to sign in screen
                        Intent intent = new Intent(SettingsActivity.this, SignInActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        showErrorDialog("Failed to delete account");
                    }
                });
    }

    private void showErrorDialog(String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}