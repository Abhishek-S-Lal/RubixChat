package com.abhishekslal.chat.android;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.abhishekslal.chat.android.Chat.ChatActivity;
import com.abhishekslal.chat.android.CreateGroupChat.CreateGroupChat_Activity;
import com.abhishekslal.chat.android.Model.Friends;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class CreateNewChatActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView friend_list_RV;

    private DatabaseReference userFriendsDatabaseReference;
    private DatabaseReference userDatabaseReference;

    private FirebaseAuth mAuth;
    private Context context;
    String current_user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_chat);

        context = this;
        toolbar = findViewById(R.id.newChat_appbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("New Chat");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mAuth = FirebaseAuth.getInstance();
        current_user_id = mAuth.getCurrentUser().getUid();

        //for friends
        userFriendsDatabaseReference = FirebaseDatabase.getInstance().getReference().child("friends").child(current_user_id);
        userFriendsDatabaseReference.keepSynced(true); // for offline

        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users");
        userDatabaseReference.keepSynced(true); // for offline

        // Setup recycler view
        friend_list_RV = findViewById(R.id.newChatFriendList);
        friend_list_RV.setHasFixedSize(true);
        friend_list_RV.setLayoutManager(new LinearLayoutManager(this));

        //display friends
        showPeopleList();
    }

    private void showPeopleList() {
        FirebaseRecyclerOptions<Friends> recyclerOptions = new FirebaseRecyclerOptions.Builder<Friends>()
                .setQuery(userFriendsDatabaseReference, Friends.class)
                .build();

        FirebaseRecyclerAdapter<Friends, FriendsVH> recyclerAdapter = new FirebaseRecyclerAdapter<Friends, FriendsVH>(recyclerOptions) {
            @Override
            protected void onBindViewHolder(@NonNull final FriendsVH holder, int position, @NonNull Friends model) {


                final String userID = getRef(position).getKey();

                userDatabaseReference.child(userID).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                        final String userName = dataSnapshot.child("user_name").getValue().toString();
                        String userThumbPhoto = dataSnapshot.child("user_thumb_image").getValue().toString();
                        String active_status = dataSnapshot.child("active_now").getValue().toString();
                        String userStatus = dataSnapshot.child("user_status").getValue().toString();

                        // online active status
                        holder.active_icon.setVisibility(View.GONE);
                        if (active_status.contains("active_now")) {
                            holder.active_icon.setVisibility(View.VISIBLE);
                        } else {
                            holder.active_icon.setVisibility(View.GONE);
                        }

                        holder.name.setText(userName);
                        holder.status.setText(userStatus);

                        Picasso.get()
                                .load(userThumbPhoto)
                                .networkPolicy(NetworkPolicy.OFFLINE) // for Offline
                                .placeholder(R.drawable.default_profile_image)
                                .into(holder.profile_thumb);


                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                // user active status validation
                                if (dataSnapshot.child("active_now").exists()) {
                                    Intent chatIntent = new Intent(CreateNewChatActivity.this, ChatActivity.class);
                                    chatIntent.putExtra("visitUserId", userID);
                                    chatIntent.putExtra("userName", userName);
                                    startActivity(chatIntent);

                                } else {
                                    userDatabaseReference.child(userID).child("active_now")
                                            .setValue(ServerValue.TIMESTAMP).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Intent chatIntent = new Intent(CreateNewChatActivity.this, ChatActivity.class);
                                            chatIntent.putExtra("visitUserId", userID);
                                            chatIntent.putExtra("userName", userName);
                                            startActivity(chatIntent);
                                        }
                                    });


                                }


                            }
                        });


                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });


            }

            @NonNull
            @Override
            public FriendsVH onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.rv_item_friendrequest, viewGroup, false);
                return new FriendsVH(view);
            }
        };

        friend_list_RV.setAdapter(recyclerAdapter);
        recyclerAdapter.startListening();
    }


    public static class FriendsVH extends RecyclerView.ViewHolder {
        public TextView name;
        TextView status;
        CircleImageView profile_thumb;
        ImageView active_icon;

        public FriendsVH(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.r_profileName);
            status = itemView.findViewById(R.id.r_profileStatus);
            profile_thumb = itemView.findViewById(R.id.r_profileImage);
            active_icon = itemView.findViewById(R.id.r_activeIcon);
        }
    }

    // tool bar action menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.new_chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.menu_newGroup) {
            Intent intent = new Intent(CreateNewChatActivity.this, CreateGroupChat_Activity.class);
            startActivity(intent);
        }
        return true;
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}