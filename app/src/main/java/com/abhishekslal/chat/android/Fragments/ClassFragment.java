package com.abhishekslal.chat.android.Fragments;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.abhishekslal.chat.android.Chat.GroupChatActivity;
import com.abhishekslal.chat.android.CreateGroupChat.CreateGroupChat_Activity;
import com.abhishekslal.chat.android.Model.Conversation;
import com.abhishekslal.chat.android.Model.Message;
import com.abhishekslal.chat.android.R;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.GregorianCalendar;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class ClassFragment extends Fragment {

    private View view;
    private FloatingActionButton fab;

    private RecyclerView class_list_RV;
    private DatabaseReference myClassDatabaseReference;
    private DatabaseReference groupChatDatabaseReference;
    private Query userDatabaseReference;
    private Query lastMessageQuery;
    private FirebaseAuth mAuth;

    private FirebaseRecyclerAdapter<Conversation, ChatsVH> adapter;

    private String current_user_id, userName="...";;

    public ClassFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_class, container, false);
        fab = view.findViewById(R.id.class_fab);


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), CreateGroupChat_Activity.class);
                startActivity(intent);
            }
        });

        class_list_RV = view.findViewById(R.id.groupList_recycler_view);
        fab = view.findViewById(R.id.class_fab);

        mAuth = FirebaseAuth.getInstance();
        current_user_id = mAuth.getCurrentUser().getUid();

        myClassDatabaseReference = FirebaseDatabase.getInstance().getReference().child("userChats").child(current_user_id).child("public");
        groupChatDatabaseReference = FirebaseDatabase.getInstance().getReference().child("groupChats");

        class_list_RV.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());

        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        class_list_RV.setLayoutManager(linearLayoutManager);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        Query query = myClassDatabaseReference.orderByChild("time");

        final FirebaseRecyclerOptions<Conversation> recyclerOptions = new FirebaseRecyclerOptions.Builder<Conversation>()
                .setQuery(query, Conversation.class)
                .build();

        adapter = new FirebaseRecyclerAdapter<Conversation, ChatsVH>(recyclerOptions) {
            @Override
            protected void onBindViewHolder(@NonNull final ChatsVH holder, final int position, @NonNull Conversation model) {

                final String conversationID = getRef(position).getKey();

                //quering the db to get the userid of the
                Query query = myClassDatabaseReference.child(conversationID);
                query.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {

                            groupChatDatabaseReference.child(conversationID).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        final String groupName = dataSnapshot.child("groupName").getValue().toString();
                                        final String groupThumbPhoto = dataSnapshot.child("group_image").getValue().toString();

                                        if (!groupThumbPhoto.equals("default_group_image")) { // default image condition for new group
                                            Picasso.get()
                                                    .load(groupThumbPhoto)
                                                    .networkPolicy(NetworkPolicy.OFFLINE) // for Offline
                                                    .placeholder(R.drawable.default_profile_image)
                                                    .into(holder.user_photo, new Callback() {
                                                        @Override
                                                        public void onSuccess() {
                                                        }

                                                        @Override
                                                        public void onError(Exception e) {
                                                            Picasso.get()
                                                                    .load(groupThumbPhoto)
                                                                    .placeholder(R.drawable.default_profile_image)
                                                                    .into(holder.user_photo);
                                                        }
                                                    });
                                        }
                                        holder.user_name.setText(groupName);

                                        //active status
                                        holder.active_icon.setVisibility(View.GONE);


                                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {

                                                Intent chatIntent = new Intent(getContext(), GroupChatActivity.class);
                                                chatIntent.putExtra("visitUserId", conversationID);
                                                chatIntent.putExtra("userName", groupName);
                                                startActivity(chatIntent);
                                            }
                                        });
                                    }

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                            final String metadata = snapshot.child("metadata").getValue().toString();
                            //fetch last message
                            if (metadata.equals("You were added") || metadata.contains("added you") || metadata.equals("You created this group")) {
                                holder.last_message.setText(metadata);
                            }
                            lastMessageQuery = FirebaseDatabase.getInstance().getReference().child("messages").child(conversationID).limitToLast(1);
                            lastMessageQuery.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    for (DataSnapshot snapshot : dataSnapshot.getChildren())
                                    if (snapshot.exists()) {


                                        final Message message = new Message();
                                        message.setMessage(snapshot.child("message").getValue().toString());
                                        message.setFrom(snapshot.child("from").getValue().toString());
                                        message.setTime((Long) (snapshot.child("time").getValue()));
                                        message.setType(snapshot.child("type").getValue().toString());
                                        message.setSeen(((Boolean) snapshot.child("seen").getValue()));


                                        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users").child(message.getFrom());
                                        userDatabaseReference.addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                userName = snapshot.child("user_name").getValue().toString();
                                                String lastMessage = "";
                                                if (!message.getFrom().equals(current_user_id)) {
                                                    lastMessage = userName + ": ";
                                                } else {
                                                    lastMessage = "✔ ";
                                                }
                                                if (message.getType().equals("text")) {
                                                    lastMessage += message.getMessage();
                                                    holder.last_message.setText(lastMessage);
                                                } else {
                                                    lastMessage += "Photo";
                                                    holder.last_message.setText(lastMessage);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });



                                        long timeStamp = message.getTime();
                                        Calendar calendar = GregorianCalendar.getInstance();
                                        calendar.setTimeInMillis(timeStamp);
                                        String cal[] = calendar.getTime().toString().split(" ");
                                        String time_of_message = cal[1] + "," + cal[2] + "  " + cal[3].substring(0, 5);
                                        holder.message_time.setText(time_of_message);

                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });




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

        class_list_RV.setAdapter(adapter);
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
