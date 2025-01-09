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
import java.util.Date;
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

            String timestamp = message.getTimestamp();
            if (timestamp != null) {
                try {
                    long timeInMillis = Long.parseLong(timestamp);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
                    String formattedTime = dateFormat.format(new Date(timeInMillis));
                    viewHolder.senderTime.setText(formattedTime);
                } catch (Exception e) {
                    viewHolder.senderTime.setText("--:--");
                }
            } else {
                viewHolder.senderTime.setText("--:--");
            }
        } else if (holder instanceof ReceiverViewHolder) {
            ReceiverViewHolder viewHolder = (ReceiverViewHolder) holder;
            viewHolder.receiverMsg.setText(message.getMessage());

            String timestamp = message.getTimestamp();
            if (timestamp != null) {
                try {
                    long timeInMillis = Long.parseLong(timestamp);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
                    String formattedTime = dateFormat.format(new Date(timeInMillis));
                    viewHolder.receiverTime.setText(formattedTime);
                } catch (Exception e) {
                    viewHolder.receiverTime.setText("--:--");
                }
            } else {
                viewHolder.receiverTime.setText("--:--");
            }
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