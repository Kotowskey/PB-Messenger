package projekt.pb.sm;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;

public class UserStatusManager {
    private static UserStatusManager instance;
    private final FirebaseDatabase database;
    private final String userId;
    private int activeActivities = 0;

    private UserStatusManager() {
        database = FirebaseDatabase.getInstance("https://react-social-a8a4c-default-rtdb.europe-west1.firebasedatabase.app/");
        userId = FirebaseAuth.getInstance().getUid();
    }

    public static synchronized UserStatusManager getInstance() {
        if (instance == null) {
            instance = new UserStatusManager();
        }
        return instance;
    }

    public synchronized void onActivityResumed() {
        activeActivities++;
        if (activeActivities == 1) {
            setUserStatus("online");
        }
    }

    public synchronized void onActivityPaused() {
        activeActivities--;
        if (activeActivities == 0) {
            setUserStatus("offline");
        }
    }

    private void setUserStatus(String status) {
        if (userId != null) {
            HashMap<String, Object> updates = new HashMap<>();
            updates.put("status", status);
            if (status.equals("offline")) {
                updates.put("lastSeen", String.valueOf(System.currentTimeMillis()));
            }
            database.getReference().child("Users").child(userId).updateChildren(updates);
        }
    }
}