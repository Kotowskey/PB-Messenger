package projekt.pb.sm;

import android.app.Application;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;

public class ChatApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Inicjalizacja FirebaseDatabase
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // Upewnij się, że status jest ustawiony na offline przy zamknięciu aplikacji
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId != null) {
            HashMap<String, Object> updates = new HashMap<>();
            updates.put("status", "offline");
            updates.put("lastSeen", String.valueOf(System.currentTimeMillis()));
            FirebaseDatabase.getInstance()
                    .getReference()
                    .child("Users")
                    .child(userId)
                    .updateChildren(updates);
        }
    }
}