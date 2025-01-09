package projekt.pb.sm.Fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import projekt.pb.sm.Adapter.UsersAdapter;
import projekt.pb.sm.models.Message;
import projekt.pb.sm.models.Users;
import projekt.pb.sm.databinding.FragmentChatsBinding;

public class ChatsFragment extends Fragment {

    private FragmentChatsBinding binding;
    private ArrayList<Users> list = new ArrayList<>();
    private FirebaseDatabase database;
    private FirebaseAuth auth;
    private UsersAdapter adapter;
    private final Handler updateHandler = new Handler(Looper.getMainLooper());
    private final Set<String> pendingUpdates = new HashSet<>();
    private boolean isUpdateScheduled = false;
    private static final long UPDATE_DELAY = 300; // ms

    public ChatsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentChatsBinding.inflate(inflater, container, false);

        database = FirebaseDatabase.getInstance("https://react-social-a8a4c-default-rtdb.europe-west1.firebasedatabase.app/");
        auth = FirebaseAuth.getInstance();

        adapter = new UsersAdapter(list, getContext());
        binding.chatRecyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        binding.chatRecyclerView.setLayoutManager(layoutManager);
        binding.chatRecyclerView.setItemAnimator(null);

        loadUsers();

        return binding.getRoot();
    }

    private void loadUsers() {
        database.getReference().child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Users users = dataSnapshot.getValue(Users.class);
                    if (users != null) {
                        users.setUserId(dataSnapshot.getKey());
                        if (!users.getUserId().equals(auth.getUid())) {
                            list.add(users);
                            fetchLastMessage(users);
                        }
                    }
                }
                scheduleUpdate();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle potential errors
            }
        });
    }

    private void fetchLastMessage(Users user) {
        String senderRoom = auth.getUid() + user.getUserId();

        database.getReference().child("chats")
                .child(senderRoom)
                .orderByKey()
                .limitToLast(1)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        updateMessageForUser(user, snapshot);
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        updateMessageForUser(user, snapshot);
                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                        user.setLastMessage(null);
                        user.setLastMessageSenderId(null);
                        user.setLastMessageRead(true);
                        user.setLastMessageTimestamp(null);
                        scheduleUpdate();
                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updateMessageForUser(Users user, DataSnapshot snapshot) {
        Message message = snapshot.getValue(Message.class);
        if (message != null) {
            user.setLastMessage(message.getMessage());
            user.setLastMessageSenderId(message.getSenderId());
            user.setLastMessageRead(message.isRead());
            user.setLastMessageTimestamp(message.getTimestamp());
            pendingUpdates.add(user.getUserId());
            scheduleUpdate();
        }
    }

    private void scheduleUpdate() {
        if (!isUpdateScheduled) {
            isUpdateScheduled = true;
            updateHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    performUpdate();
                }
            }, UPDATE_DELAY);
        }
    }

    private void performUpdate() {
        if (isAdded() && !isDetached()) {
            sortUsersByLastMessage();
            adapter.notifyDataSetChanged();
        }
        pendingUpdates.clear();
        isUpdateScheduled = false;
    }

    private void sortUsersByLastMessage() {
        Collections.sort(list, new Comparator<Users>() {
            @Override
            public int compare(Users user1, Users user2) {
                String timestamp1 = user1.getLastMessageTimestamp();
                String timestamp2 = user2.getLastMessageTimestamp();

                if (timestamp1 == null && timestamp2 == null) {
                    return user1.getUserName().compareTo(user2.getUserName());
                }

                if (timestamp1 == null) return 1;
                if (timestamp2 == null) return -1;

                try {
                    long time1 = Long.parseLong(timestamp1);
                    long time2 = Long.parseLong(timestamp2);
                    return Long.compare(time2, time1);
                } catch (NumberFormatException e) {
                    return timestamp2.compareTo(timestamp1);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (auth.getCurrentUser() != null) {
            database.getReference().child("Users")
                    .child(auth.getCurrentUser().getUid())
                    .child("status")
                    .setValue("online");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (auth.getCurrentUser() != null) {
            database.getReference().child("Users")
                    .child(auth.getCurrentUser().getUid())
                    .child("status")
                    .setValue("offline");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        updateHandler.removeCallbacksAndMessages(null);
        binding = null;
    }
}