package projekt.pb.sm.Adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import de.hdodenhof.circleimageview.CircleImageView;
import projekt.pb.sm.ChatDetailActivity;
import projekt.pb.sm.R;
import projekt.pb.sm.models.Users;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {

    private ArrayList<Users> list;
    private Context context;
    private String currentUserId;

    public UsersAdapter(ArrayList<Users> list, Context context) {
        this.list = list;
        this.context = context;
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
        Users user = list.get(position);
        Picasso.get().load(user.getProfilePic()).placeholder(R.drawable.avatar).into(holder.image);
        holder.userName.setText(user.getUserName());

        // Ustawienie tekstu ostatniej wiadomości
        if (user.getLastMessage() != null) {
            String lastMessageText = user.getLastMessage();
            if (user.getLastMessageSenderId() != null && user.getLastMessageSenderId().equals(currentUserId)) {
                lastMessageText = "You: " + lastMessageText;
            }
            holder.lastMessage.setText(lastMessageText);

            // Styl tekstu w zależności od stanu odczytu
            if (!user.isLastMessageRead() && !user.getLastMessageSenderId().equals(currentUserId)) {
                holder.lastMessage.setTypeface(holder.lastMessage.getTypeface(), Typeface.BOLD);
                holder.userName.setTypeface(holder.userName.getTypeface(), Typeface.BOLD);
                holder.lastMessage.setTextColor(ContextCompat.getColor(context, R.color.md_theme_light_primary));
            } else {
                holder.lastMessage.setTypeface(null, Typeface.NORMAL);
                holder.userName.setTypeface(null, Typeface.NORMAL);
                holder.lastMessage.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            }
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatDetailActivity.class);
            intent.putExtra("userId", user.getUserId());
            intent.putExtra("userName", user.getUserName());
            intent.putExtra("profilePic", user.getProfilePic());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView image;
        TextView userName, lastMessage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.profile_image);
            userName = itemView.findViewById(R.id.userName);
            lastMessage = itemView.findViewById(R.id.lastMessage);
        }
    }
}