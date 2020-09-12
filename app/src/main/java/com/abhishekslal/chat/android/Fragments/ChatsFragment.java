package com.abhishekslal.chat.android.Fragments;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.abhishekslal.chat.android.Chat.ChatActivity;
import com.abhishekslal.chat.android.CreateNewChatActivity;
import com.abhishekslal.chat.android.Model.Conversation;
import com.abhishekslal.chat.android.Model.Message;
import com.abhishekslal.chat.android.R;
import com.abhishekslal.chat.android.Utils.UserLastSeenTime;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.GregorianCalendar;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatsFragment extends Fragment {

    private View view;
    private FloatingActionButton fab;
    private RecyclerView chat_list_RV;
    private DatabaseReference myChatsDatabaseReference;
    private DatabaseReference userDatabaseReference;
    private Query lastMessageQuery;
    private FirebaseAuth mAuth;

    private FirebaseRecyclerAdapter<Conversation, ChatsVH> adapter;

    private String current_user_id;

    public ChatsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_chats, container, false);

        chat_list_RV = view.findViewById(R.id.chatList_recycler_view);
        fab = view.findViewById(R.id.chat_fab);

        mAuth = FirebaseAuth.getInstance();
        current_user_id = mAuth.getCurrentUser().getUid();

        myChatsDatabaseReference = FirebaseDatabase.getInstance().getReference().child("userChats").child(current_user_id).child("private");
        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users");

        chat_list_RV.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());

        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        chat_list_RV.setLayoutManager(linearLayoutManager);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), CreateNewChatActivity.class);
                startActivity(intent);
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        Query query = myChatsDatabaseReference.orderByChild("time");

        final FirebaseRecyclerOptions<Conversation> recyclerOptions = new FirebaseRecyclerOptions.Builder<Conversation>()
                .setQuery(query, Conversation.class)
                .build();

        adapter = new FirebaseRecyclerAdapter<Conversation, ChatsVH>(recyclerOptions) {
            @Override
            protected void onBindViewHolder(@NonNull final ChatsVH holder, int position, @NonNull Conversation model) {

                final String conversationID = getRef(position).getKey();

                //quering the db to get the userid of the
                Query query = myChatsDatabaseReference.child(conversationID);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            if (snapshot.child("friendId").getValue() != null) {

                                final String friendUserID = snapshot.child("friendId").getValue().toString();
                                //fetch last message

                                lastMessageQuery = FirebaseDatabase.getInstance().getReference().child("messages").child(conversationID).limitToLast(1);
                                lastMessageQuery.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot snapshot : dataSnapshot.getChildren())
                                        if (snapshot.exists()){
                                            String lastMessage = "";
                                            Message message = new Message();
                                            message.setMessage(snapshot.child("message").getValue().toString());
                                            message.setFrom(snapshot.child("from").getValue().toString());
                                            message.setTime((Long)(snapshot.child("time").getValue()));
                                            message.setType(snapshot.child("type").getValue().toString());
                                            message.setSeen(((Boolean) snapshot.child("seen").getValue()));
                                            if (!message.getFrom().equals(current_user_id)){
                                                lastMessage = "";
                                            }
                                            else {
                                                lastMessage = "✔ ";
                                            }
                                            if (message.getType().equals("text")){
                                                lastMessage += message.getMessage();
                                                holder.last_message.setText(lastMessage);
                                            }
                                            else {
                                                lastMessage+= "Photo";
                                                holder.last_message.setText(lastMessage);
                                            }

                                            long timeStamp = message.getTime();
                                            Calendar calendar = GregorianCalendar.getInstance();
                                            calendar.setTimeInMillis(timeStamp);
                                            String cal[] = calendar.getTime().toString().split(" ");
                                            String time_of_message = cal[1]+","+cal[2]+"  "+cal[3].substring(0,5);
                                            holder.message_time.setText(time_of_message);

                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });

                                userDatabaseReference.child(friendUserID).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            final String userName = dataSnapshot.child("user_name").getValue().toString();
                                            final String userPresence = dataSnapshot.child("active_now").getValue().toString();
                                            final String userThumbPhoto = dataSnapshot.child("user_thumb_image").getValue().toString();

                                            if (!userThumbPhoto.equals("default_image")) { // default image condition for new user
                                                Picasso.get()
                                                        .load(userThumbPhoto)
                                                        .networkPolicy(NetworkPolicy.OFFLINE) // for Offline
                                                        .placeholder(R.drawable.default_profile_image)
                                                        .into(holder.user_photo, new Callback() {
                                                            @Override
                                                            public void onSuccess() {
                                                            }

                                                            @Override
                                                            public void onError(Exception e) {
                                                                Picasso.get()
                                                                        .load(userThumbPhoto)
                                                                        .placeholder(R.drawable.default_profile_image)
                                                                        .into(holder.user_photo);
                                                            }
                                                        });
                                            }
                                            holder.user_name.setText(userName);

                                            //active status
                                            holder.active_icon.setVisibility(View.GONE);
                                            if (userPresence.contains("true")) {
//                                                holder.user_presence.setText("Active now");

                                                holder.active_icon.setVisibility(View.VISIBLE);
                                            } else {
                                                holder.active_icon.setVisibility(View.GONE);
                                                UserLastSeenTime lastSeenTime = new UserLastSeenTime();
                                                long last_seen = Long.parseLong(userPresence);
                                                String lastSeenOnScreenTime = lastSeenTime.getTimeAgo(last_seen, getContext());
                                                Log.e("lastSeenTime", lastSeenOnScreenTime);
                                                if (lastSeenOnScreenTime != null) {
//                                                    holder.user_presence.setText(lastSeenOnScreenTime);
                                                }
                                            }


                                            holder.itemView.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    // user active status validation
                                                    if (dataSnapshot.child("active_now").exists()) {

                                                        Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                                        chatIntent.putExtra("visitUserId", friendUserID);
                                                        chatIntent.putExtra("userName", userName);
                                                        startActivity(chatIntent);

                                                    } else {
                                                        userDatabaseReference.child(friendUserID).child("active_now")
                                                                .setValue(ServerValue.TIMESTAMP).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                                                chatIntent.putExtra("visitUserId", friendUserID);
                                                                chatIntent.putExtra("userName", userName);
                                                                startActivity(chatIntent);
                                                            }
                                                        });


                                                    }
                                                }
                                            });
                                        }

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                            }
                        }

                    }


                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });



            }

            @NonNull
            @Override
            public ChatsVH onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.rv_item_chatfragment, viewGroup, false);
                return new ChatsVH(view);
            }
        };

        chat_list_RV.setAdapter(adapter);
        adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    public static class ChatsVH extends RecyclerView.ViewHolder {
        TextView user_name, last_message, message_time;
        CircleImageView user_photo;
        ImageView active_icon;

        public ChatsVH(View itemView) {
            super(itemView);
            user_name = itemView.findViewById(R.id.all_user_name);
            user_photo = itemView.findViewById(R.id.all_user_profile_img);
            last_message = itemView.findViewById(R.id.last_message);
            active_icon = itemView.findViewById(R.id.activeIcon);
            message_time = itemView.findViewById(R.id.msg_time);
        }
    }


}
