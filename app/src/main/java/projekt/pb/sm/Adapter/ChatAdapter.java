package projekt.pb.sm.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import projekt.pb.sm.R;
import projekt.pb.sm.models.Message;

public class ChatAdapter extends RecyclerView.Adapter {

    private final ArrayList<Message> messageModel;
    private final Context context;
    private final String currentUserId;

    private static final int SENDER_VIEW_TYPE = 1;
    private static final int RECEIVER_VIEW_TYPE = 2;

    public ChatAdapter(ArrayList<Message> messageModel, Context context, String receiverId) {
        this.messageModel = messageModel;
        this.context = context;
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == SENDER_VIEW_TYPE) {
            View view = LayoutInflater.from(context).inflate(R.layout.sample_sender, parent, false);
            return new SenderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.sample_receiver, parent, false);
            return new ReceiverViewHolder(view);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageModel.get(position);
        if (message != null && message.getSenderId() != null && currentUserId != null
                && message.getSenderId().equals(currentUserId)) {
            return SENDER_VIEW_TYPE;
        }
        return RECEIVER_VIEW_TYPE;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageModel.get(position);
        if (message == null) return;

        if (holder instanceof SenderViewHolder) {
            SenderViewHolder viewHolder = (SenderViewHolder) holder;
            viewHolder.senderMsg.setText(message.getMessage());
            viewHolder.senderTime.setText(formatMessageTime(message.getTimestamp()));
        } else if (holder instanceof ReceiverViewHolder) {
            ReceiverViewHolder viewHolder = (ReceiverViewHolder) holder;
            viewHolder.receiverMsg.setText(message.getMessage());
            viewHolder.receiverTime.setText(formatMessageTime(message.getTimestamp()));
        }
    }

    private String formatMessageTime(String timestamp) {
        if (timestamp == null) return "--:--";

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
                return "wczoraj " + new SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(new Date(timeInMillis));
            } else if (diff < 7 * 24 * 60 * 60 * 1000) {
                // W tym tygodniu
                return new SimpleDateFormat("EEEE HH:mm", new Locale("pl"))
                        .format(new Date(timeInMillis));
            } else {
                // Starsze wiadomości
                return new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
                        .format(new Date(timeInMillis));
            }
        } catch (Exception e) {
            return "--:--";
        }
    }

    @Override
    public int getItemCount() {
        return messageModel != null ? messageModel.size() : 0;
    }

    public static class ReceiverViewHolder extends RecyclerView.ViewHolder {
        TextView receiverMsg, receiverTime;

        public ReceiverViewHolder(@NonNull View itemView) {
            super(itemView);
            receiverMsg = itemView.findViewById(R.id.receiverText);
            receiverTime = itemView.findViewById(R.id.receiverTime);
        }
    }

    public static class SenderViewHolder extends RecyclerView.ViewHolder {
        TextView senderMsg, senderTime;

        public SenderViewHolder(@NonNull View itemView) {
            super(itemView);
            senderMsg = itemView.findViewById(R.id.senderText);
            senderTime = itemView.findViewById(R.id.senderTime);
        }
    }
}