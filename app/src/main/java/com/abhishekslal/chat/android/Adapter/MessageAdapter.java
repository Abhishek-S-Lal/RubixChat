package com.abhishekslal.chat.android.Adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.abhishekslal.chat.android.Model.Message;
import com.abhishekslal.chat.android.R;
import com.makeramen.roundedimageview.RoundedImageView;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

import static androidx.core.content.ContextCompat.startActivity;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder>{

    private List<Message> messageList;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    //-----GETTING LIST OF ALL MESSAGES FROM CHAT ACTIVITY ----
    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    //---CREATING SINGLE HOLDER AND RETURNING ITS VIEW---
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_message_pchat, parent, false);
        mAuth = FirebaseAuth.getInstance();
        return new MessageViewHolder(view);
    }

    //----SETTING EACH HOLDER WITH DATA----
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder holder, int position) {
        String currentUser_UID = mAuth.getCurrentUser().getUid();
        final Message message = messageList.get(position);

        String from_user_ID = message.getFrom();
        String from_message_TYPE = message.getType();

        //---ADDING NAME THUMB_IMAGE TO THE HOLDER----
        databaseReference = FirebaseDatabase.getInstance().getReference().child("users").child(from_user_ID);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){

                    String chattype = message.getChat_type();
                    if (chattype.equals("group")){
                        holder.senderName.setVisibility(View.VISIBLE);
                        String userName = dataSnapshot.child("user_name").getValue().toString();
                        holder.senderName.setText(userName);
                    }
                    else {
                        holder.senderName.setVisibility(View.GONE);
                    }
                    String userProfileImage = dataSnapshot.child("user_thumb_image").getValue().toString();
                    //
                    Picasso.get()
                            .load(userProfileImage)
                            .networkPolicy(NetworkPolicy.OFFLINE) // for Offline
                            .placeholder(R.drawable.default_profile_image)
                            .into(holder.user_profile_image);
                }

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        // if message type is TEXT
        if (from_message_TYPE.equals("text")){

            // when msg is TEXT, image views are gone
            holder.senderImageMsg.setVisibility(View.GONE);
            holder.receiverImageMsg.setVisibility(View.GONE);
            holder.imageMsgTimeReceiver.setVisibility(View.GONE);
            holder.imageMsgTimeSender.setVisibility(View.GONE);

            if (from_user_ID.equals(currentUser_UID)){
                //hiding receiver msg views and showing sender msg view
                holder.receiverMessage.setVisibility(View.INVISIBLE);
                holder.user_profile_image.setVisibility(View.INVISIBLE);
                holder.senderMessage.setVisibility(View.VISIBLE);

                //remaining
                holder.sender_text_message.setBackgroundResource(R.drawable.single_message_text_another_background);
                holder.sender_text_message.setTextColor(Color.BLACK);
                holder.sender_text_message.setGravity(Gravity.LEFT);
                holder.sender_text_message.setText(message.getMessage());

                long timeStamp = message.getTime();
                Calendar calendar = GregorianCalendar.getInstance();
                calendar.setTimeInMillis(timeStamp);
                String cal[] = calendar.getTime().toString().split(" ");
                String time_of_message = cal[1]+","+cal[2]+"  "+cal[3].substring(0,5);
                holder.senderMsgTime.setText(time_of_message);

            } else {
                //hiding sender msg views and showing receiving msg views
                holder.senderMessage.setVisibility(View.INVISIBLE);
                holder.receiverMessage.setVisibility(View.VISIBLE);
                holder.user_profile_image.setVisibility(View.VISIBLE);

                holder.receiver_text_message.setBackgroundResource(R.drawable.single_message_text_background);
                holder.receiver_text_message.setTextColor(Color.WHITE);
                holder.receiver_text_message.setGravity(Gravity.LEFT);
                holder.receiver_text_message.setText(message.getMessage());

                long timeStamp = message.getTime();
                Calendar calendar = GregorianCalendar.getInstance();
                calendar.setTimeInMillis(timeStamp);
                String cal[] = calendar.getTime().toString().split(" ");
                String time_of_message = cal[1]+","+cal[2]+"  "+cal[3].substring(0,5);
                holder.receiverMsgTime.setText(time_of_message);
            }
        }
        if (from_message_TYPE.equals("image")){ // if message type is NON TEXT
            // when msg has IMAGE, text views are GONE
            holder.senderMessage.setVisibility(View.GONE);
            holder.receiverMessage.setVisibility(View.GONE);


            if (from_user_ID.equals(currentUser_UID)){
                //hiding current user views and showing sender's image view
                holder.user_profile_image.setVisibility(View.GONE);
                holder.receiverImageMsg.setVisibility(View.GONE);
                holder.imageMsgTimeReceiver.setVisibility(View.GONE);
                holder.senderImageMsg.setVisibility(View.VISIBLE);
                holder.imageMsgTimeSender.setVisibility(View.VISIBLE);
                Picasso.get()
                        .load(message.getMessage())
//                        .networkPolicy(NetworkPolicy.OFFLINE) // for Offline
                         //.placeholder(R.drawable.default_profile_image)
                        .into(holder.senderImageMsg);

                long timeStamp = message.getTime();
                Calendar calendar = GregorianCalendar.getInstance();
                calendar.setTimeInMillis(timeStamp);
                String cal[] = calendar.getTime().toString().split(" ");
                String time_of_message = cal[1]+","+cal[2]+"  "+cal[3].substring(0,5);
                holder.imageMsgTimeSender.setText(time_of_message);

                holder.senderImageMsg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {


                    }
                });

            } else {
                //showing sender's image view and hiding
                holder.user_profile_image.setVisibility(View.VISIBLE);
                holder.receiverImageMsg.setVisibility(View.VISIBLE);
                holder.imageMsgTimeReceiver.setVisibility(View.VISIBLE);
                holder.imageMsgTimeSender.setVisibility(View.GONE);
                holder.senderImageMsg.setVisibility(View.GONE);
                Picasso.get()
                        .load(message.getMessage())
//                        .networkPolicy(NetworkPolicy.OFFLINE) // for Offline
                         //.placeholder(R.drawable.default_profile_image)
                        .into(holder.receiverImageMsg);

                long timeStamp = message.getTime();
                Calendar calendar = GregorianCalendar.getInstance();
                calendar.setTimeInMillis(timeStamp);
                String cal[] = calendar.getTime().toString().split(" ");
                String time_of_message = cal[1]+","+cal[2]+"  "+cal[3].substring(0,5);
                holder.imageMsgTimeReceiver.setText(time_of_message);

            }

        }

    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder{
        TextView sender_text_message, receiver_text_message, senderMsgTime, receiverMsgTime, imageMsgTimeReceiver, imageMsgTimeSender,senderName;
        CircleImageView user_profile_image;
        RoundedImageView senderImageMsg, receiverImageMsg;
        LinearLayout receiverMessage,senderMessage;

        MessageViewHolder(View view){
            super(view);
            senderName = view.findViewById(R.id.group_senderName);
            sender_text_message = view.findViewById(R.id.senderMessageText);
            receiver_text_message = view.findViewById(R.id.receiverMessageText);
            user_profile_image = view.findViewById(R.id.messageUserImage);
            receiverMessage = view.findViewById(R.id.receiverMessage);
            senderMessage = view.findViewById(R.id.senderMessage);
            senderImageMsg = view.findViewById(R.id.messageImageVsender);
            receiverImageMsg = view.findViewById(R.id.messageImageReceiver);
            senderMsgTime = view.findViewById(R.id.senderMsgTime);
            receiverMsgTime =view.findViewById(R.id.receiverMsgTime);
            imageMsgTimeReceiver =view.findViewById(R.id.timeImageReceiver);
            imageMsgTimeSender =view.findViewById(R.id.timeImageSender);
        }

    }
}
