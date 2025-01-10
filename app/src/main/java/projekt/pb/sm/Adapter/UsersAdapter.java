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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
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

        // Pokazywanie statusu online/offline
        if ("online".equals(user.getStatus())) {
            holder.statusIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.statusIndicator.setVisibility(View.GONE);
        }

        // Wyświetlanie ostatniej wiadomości, statusu i czasu
        if (user.getLastMessage() != null) {
            String lastMessageText = user.getLastMessage();
            if (user.getLastMessageSenderId() != null && user.getLastMessageSenderId().equals(currentUserId)) {
                lastMessageText = "Ty: " + lastMessageText;
            }

            // Formatowanie czasu ostatniej wiadomości
            String timeStr = "";
            if (user.getLastMessageTimestamp() != null) {
                timeStr = formatMessageTime(user.getLastMessageTimestamp());
            }

            // Łączenie tekstu wiadomości i czasu
            String displayText = lastMessageText;
            if (!timeStr.isEmpty()) {
                displayText = displayText + "  ·  " + timeStr;
            }

            holder.lastMessage.setText(displayText);

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

    private String formatMessageTime(String timestamp) {
        if (timestamp == null) return "";

        try {
            long timeInMillis = Long.parseLong(timestamp);
            long now = System.currentTimeMillis();
            long diff = now - timeInMillis;

            Calendar messageCal = Calendar.getInstance();
            messageCal.setTimeInMillis(timeInMillis);

            Calendar nowCal = Calendar.getInstance();
            nowCal.setTimeInMillis(now);

            if (diff < 24 * 60 * 60 * 1000 &&
                    messageCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)) {
                // Dzisiaj
                return new SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(new Date(timeInMillis));
            } else if (diff < 48 * 60 * 60 * 1000 &&
                    messageCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR) - 1) {
                // Wczoraj
                return "wczoraj";
            } else if (diff < 7 * 24 * 60 * 60 * 1000) {
                // W tym tygodniu
                return new SimpleDateFormat("EEEE", new Locale("pl"))
                        .format(new Date(timeInMillis))
                        .toLowerCase();
            } else {
                // Starsze wiadomości
                return new SimpleDateFormat("d MMM", new Locale("pl"))
                        .format(new Date(timeInMillis))
                        .toLowerCase();
            }
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView image;
        TextView userName, lastMessage;
        View statusIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.profile_image);
            userName = itemView.findViewById(R.id.userName);
            lastMessage = itemView.findViewById(R.id.lastMessage);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
        }
    }
}