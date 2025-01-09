package projekt.pb.sm.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import projekt.pb.sm.ChatDetailActivity;
import projekt.pb.sm.R;
import projekt.pb.sm.models.Message;
import projekt.pb.sm.models.Users;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {

    ArrayList<Users> list;
    Context context;
    FirebaseDatabase database;
    String currentUserId;

    public UsersAdapter(ArrayList<Users> list, Context context) {
        this.list = list;
        this.context = context;
        this.database = FirebaseDatabase.getInstance();
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.sample_show_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Users users = list.get(position);
        Picasso.get()
                .load(users.getProfilePic())
                .placeholder(R.drawable.avatar)
                .into(holder.image);

        holder.userName.setText(users.getUserName());

        // Pobierz ostatnią wiadomość z chatu
        String chatRoomId = currentUserId + users.getUserId();
        database.getReference().child("chats")
                .child(chatRoomId)
                .limitToLast(1)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot messageSnap : snapshot.getChildren()) {
                                Message message = messageSnap.getValue(Message.class);
                                if (message != null) {
                                    holder.lastMessage.setText(message.getMessage());
                                }
                            }
                        } else {
                            holder.lastMessage.setText("");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        holder.lastMessage.setText("");
                    }
                });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ChatDetailActivity.class);
                intent.putExtra("userId", users.getUserId());
                intent.putExtra("profilePic", users.getProfilePic());
                intent.putExtra("userName", users.getUserName());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView userName, lastMessage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            image = itemView.findViewById(R.id.profile_image);
            userName = itemView.findViewById(R.id.userNameList);
            lastMessage = itemView.findViewById(R.id.lastMessage);
        }
    }
}