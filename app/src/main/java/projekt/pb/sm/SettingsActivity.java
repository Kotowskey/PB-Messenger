package projekt.pb.sm;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import android.view.LayoutInflater;
import android.view.View;
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

        // Initialize notifications switch
        SwitchMaterial notificationsSwitch = binding.notificationsSwitch;
        notificationsSwitch.setChecked(NotificationHelper.areNotificationsEnabled(this));

        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            NotificationHelper.setNotificationsEnabled(this, isChecked);
            Toast.makeText(this,
                    isChecked ? "Powiadomienia włączone" : "Powiadomienia wyłączone",
                    Toast.LENGTH_SHORT).show();
        });

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
                .setTitle("Usuń konto")
                .setMessage("Czy na pewno chcesz usunąć swoje konto? Tej operacji nie można cofnąć.")
                .setPositiveButton("Usuń", (dialog, which) -> showPasswordConfirmationDialog())
                .setNegativeButton("Anuluj", null)
                .show();
    }

    private void showPasswordConfirmationDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_password, null);
        TextInputEditText passwordInput = dialogView.findViewById(R.id.passwordInput);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Potwierdź hasło")
                .setView(dialogView)
                .setMessage("Wprowadź swoje hasło, aby potwierdzić usunięcie konta")
                .setPositiveButton("Potwierdź", (dialog, which) -> {
                    String password = passwordInput.getText().toString();
                    if (!password.isEmpty()) {
                        reauthenticateAndDelete(password);
                    }
                })
                .setNegativeButton("Anuluj", null)
                .show();
    }

    private void reauthenticateAndDelete(String password) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

            user.reauthenticate(credential)
                    .addOnCompleteListener(reauthTask -> {
                        if (reauthTask.isSuccessful()) {
                            deleteUserData(user);
                        } else {
                            new MaterialAlertDialogBuilder(this)
                                    .setTitle("Błąd")
                                    .setMessage("Nieprawidłowe hasło. Spróbuj ponownie.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    });
        }
    }

    private void deleteUserData(FirebaseUser user) {
        String userId = user.getUid();

        database.getReference().child("Users").child(userId)
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        deleteUserChats(userId, user);
                    } else {
                        showErrorDialog("Nie udało się usunąć danych użytkownika");
                    }
                });
    }

    private void deleteUserChats(String userId, FirebaseUser user) {
        database.getReference().child("chats").get()
                .addOnSuccessListener(snapshot -> {
                    for (String key : snapshot.getValue() != null ?
                            ((java.util.HashMap<String, Object>) snapshot.getValue()).keySet() :
                            new java.util.ArrayList<String>()) {
                        if (key.contains(userId)) {
                            database.getReference().child("chats").child(key).removeValue();
                        }
                    }
                    deleteUserAuthentication(user);
                })
                .addOnFailureListener(e -> {
                    showErrorDialog("Nie udało się usunąć czatów użytkownika");
                });
    }

    private void deleteUserAuthentication(FirebaseUser user) {
        user.delete()
                .addOnCompleteListener(deleteTask -> {
                    if (deleteTask.isSuccessful()) {
                        Intent intent = new Intent(SettingsActivity.this, SignInActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        showErrorDialog("Nie udało się usunąć konta");
                    }
                });
    }

    private void showErrorDialog(String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Błąd")
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