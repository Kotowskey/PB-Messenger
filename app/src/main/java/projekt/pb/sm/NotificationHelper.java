package projekt.pb.sm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import projekt.pb.sm.models.Users;

public class NotificationHelper {
    private static final String CHANNEL_ID = "chat_messages";
    private static final String CHANNEL_NAME = "Chat Messages";
    private static final String CHANNEL_DESC = "Notifications for new chat messages";
    private static final String PREFS_NAME = "ChatPreferences";
    private static final String NOTIFICATIONS_ENABLED = "notifications_enabled";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void showMessageNotification(Context context, String senderName, String message, String senderId) {
        if (!areNotificationsEnabled(context)) {
            return;
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://react-social-a8a4c-default-rtdb.europe-west1.firebasedatabase.app/");

        // Pobierz pełne dane użytkownika
        database.getReference().child("Users").child(senderId).get()
                .addOnSuccessListener(dataSnapshot -> {
                    if (dataSnapshot.exists()) {
                        Users user = dataSnapshot.getValue(Users.class);
                        if (user != null) {
                            user.setUserId(senderId); // Upewnij się, że ID jest ustawione

                            // Przygotuj intent z pełnymi danymi użytkownika
                            Intent intent = new Intent(context, ChatDetailActivity.class);
                            intent.putExtra("userId", senderId);
                            intent.putExtra("userName", user.getUserName());
                            intent.putExtra("profilePic", user.getProfilePic());
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                            // Ustaw unikalny requestCode dla PendingIntent bazując na senderId
                            int requestCode = senderId.hashCode();

                            PendingIntent pendingIntent = PendingIntent.getActivity(
                                    context,
                                    requestCode,
                                    intent,
                                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                            );

                            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                                    .setContentTitle(user.getUserName())
                                    .setContentText(message)
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                    .setAutoCancel(true)
                                    .setContentIntent(pendingIntent);

                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                            try {
                                // Użyj senderId jako notificationId, aby każdy chat miał osobne powiadomienie
                                notificationManager.notify(senderId.hashCode(), builder.build());
                            } catch (SecurityException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
    }

    public static boolean areNotificationsEnabled(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(NOTIFICATIONS_ENABLED, true);
    }

    public static void setNotificationsEnabled(Context context, boolean enabled) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(NOTIFICATIONS_ENABLED, enabled).apply();
    }
}