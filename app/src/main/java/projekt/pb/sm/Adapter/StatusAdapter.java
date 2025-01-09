package projekt.pb.sm.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import de.hdodenhof.circleimageview.CircleImageView;
import projekt.pb.sm.R;
import projekt.pb.sm.models.Users;

public class StatusAdapter extends RecyclerView.Adapter<StatusAdapter.ViewHolder> {

    ArrayList<Users> list;
    Context context;

    public StatusAdapter(ArrayList<Users> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.sample_status_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Users users = list.get(position);
        Picasso.get()
                .load(users.getProfilePic())
                .placeholder(R.drawable.avatar)
                .into(holder.profileImage);

        holder.userName.setText(users.getUserName());

        // Logika statusu online/offline
        if ("online".equals(users.getStatus())) {
            holder.statusIndicator.setVisibility(View.VISIBLE);
            holder.userStatus.setText("online");
        } else {
            holder.statusIndicator.setVisibility(View.GONE);

            // Wyświetlanie czasu ostatniej aktywności
            if (users.getLastSeen() != null) {
                try {
                    long lastSeenTime = Long.parseLong(users.getLastSeen());
                    String lastSeenStr = formatLastSeen(lastSeenTime);
                    holder.userStatus.setText("ostatnio widziany " + lastSeenStr);
                } catch (NumberFormatException e) {
                    holder.userStatus.setText("offline");
                }
            } else {
                holder.userStatus.setText("offline");
            }
        }
    }

    private String formatLastSeen(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        // Jeśli mniej niż 24 godziny temu
        if (diff < 24 * 60 * 60 * 1000) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(new Date(timestamp));
        } else {
            return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    .format(new Date(timestamp));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView userName, userStatus;
        View statusIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            userName = itemView.findViewById(R.id.userNameStatus);
            userStatus = itemView.findViewById(R.id.userStatus);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
        }
    }
}