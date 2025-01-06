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
import java.util.Locale;
import projekt.pb.sm.R;
import projekt.pb.sm.models.Message;

public class ChatAdapter extends RecyclerView.Adapter {
    ArrayList<Message> messageModel;
    Context context;
    int SENDER_VIEW_TYPE = 1;
    int RECEIVER_VIEW_TYPE = 2;

    public ChatAdapter(ArrayList<Message> messageModel, Context context) {
        this.messageModel = messageModel;
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == SENDER_VIEW_TYPE) {
            View view = LayoutInflater.from(context).inflate(R.layout.sample_sender, parent, false);
            return new SenderViewHolder(view);
        }
        View view = LayoutInflater.from(context).inflate(R.layout.sample_receiver, parent, false);
        return new ReceiverViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        if (FirebaseAuth.getInstance().getUid() != null &&
                FirebaseAuth.getInstance().getUid().equals(messageModel.get(position).getSenderId())) {
            return SENDER_VIEW_TYPE;
        }
        return RECEIVER_VIEW_TYPE;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageModel.get(position);

        if (holder instanceof SenderViewHolder) {
            SenderViewHolder viewHolder = (SenderViewHolder) holder;
            viewHolder.senderMsg.setText(message.getMessage());
            if (message.getTimestamp() != null) {
                viewHolder.senderTime.setText(formatTime(message.getTimestamp()));
            }
        } else {
            ReceiverViewHolder viewHolder = (ReceiverViewHolder) holder;
            viewHolder.receiverMsg.setText(message.getMessage());
            if (message.getTimestamp() != null) {
                viewHolder.receiverTime.setText(formatTime(message.getTimestamp()));
            }
        }
    }

    private String formatTime(String timestamp) {
        try {
            long timeInMillis = Long.parseLong(timestamp);
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return dateFormat.format(new Date(timeInMillis));
        } catch (Exception e) {
            return "--:--";
        }
    }

    @Override
    public int getItemCount() {
        return messageModel.size();
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