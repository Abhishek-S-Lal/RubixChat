package com.abhishekslal.chat.android.Profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.abhishekslal.chat.android.CreateGroupChat.CreateGroupChat_Activity;
import com.abhishekslal.chat.android.Home.MainActivity;
import com.abhishekslal.chat.android.Settings.GroupEditActivity;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.abhishekslal.chat.android.Chat.ChatActivity;
import com.abhishekslal.chat.android.Model.Friends;
import com.abhishekslal.chat.android.R;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import xyz.hasnat.sweettoast.SweetToast;

public class GroupProfileActivity extends AppCompatActivity {

    private int flag;
    private String groupChatID, currentUserId, gCreatorUid;
    private Toolbar mToolbar;
    private ProgressDialog progressDialog;
    private ImageView groupProPic;
    private TextView groupName, groupDescription, groupOtherDetails, editGroupDetails;
    private Button exitGroupBtn, addMembersBtn;
    private RecyclerView membersRecyclerView;
    private DatabaseReference rootReference;
    private DatabaseReference groupDetailsDatabaseReference;
    private DatabaseReference userFriendsDatabaseReference;

    private DatabaseReference userDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_profile);

        //-----GETTING FROM INTENT----
        groupChatID = getIntent().getExtras().get("visitUserId").toString();

        mToolbar = findViewById(R.id.group_profile_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Group Details");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        groupProPic = findViewById(R.id.group_profile_image);
        groupName = findViewById(R.id.group_name);
        groupDescription = findViewById(R.id.group_description);
        groupOtherDetails = findViewById(R.id.created_at_and_by);
        editGroupDetails = findViewById(R.id.edit_details);

        exitGroupBtn = findViewById(R.id.exitGroupButton);
        addMembersBtn = findViewById(R.id.addMembers);

        progressDialog = new ProgressDialog(this);

        currentUserId = FirebaseAuth.getInstance().getUid();

        rootReference = FirebaseDatabase.getInstance().getReference();

        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users");
        userDatabaseReference.keepSynced(true); // for offline

        //for checking member is friend of current user
        userFriendsDatabaseReference = FirebaseDatabase.getInstance().getReference().child("friends").child(currentUserId);

        // Setup recycler view
        membersRecyclerView = findViewById(R.id.membersRV);
        membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        //display other details
        groupDetailsDatabaseReference = FirebaseDatabase.getInstance().getReference().child("groupChats").child(groupChatID);
        groupDetailsDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String gName = snapshot.child("groupName").getValue().toString();
                    String gDescription = snapshot.child("groupDescription").getValue().toString();
                    long timeStamp = (long) snapshot.child("created_at").getValue();
                    gCreatorUid = snapshot.child("creator_uId").getValue().toString();
                    String gCreatorUname = snapshot.child("creator_uName").getValue().toString();
                    final String imageUrl = snapshot.child("group_image").getValue().toString();


                    groupName.setText(gName);
                    groupDescription.setText(gDescription);

                    Calendar calendar = GregorianCalendar.getInstance();
                    calendar.setTimeInMillis(timeStamp);
                    String cal[] = calendar.getTime().toString().split(" ");
                    String gOtherDetails = "Created at " + cal[1] + "," + cal[2] + "  " + cal[3].substring(0, 5) + " by " + gCreatorUname;

                    groupOtherDetails.setText(gOtherDetails);

                    Picasso.get()
                            .load(imageUrl)
                            .networkPolicy(NetworkPolicy.OFFLINE) // for Offline
                            .placeholder(R.drawable.default_profile_image)
                            .into(groupProPic, new Callback() {
                                @Override
                                public void onSuccess() {
                                }

                                @Override
                                public void onError(Exception e) {
                                    Picasso.get()
                                            .load(imageUrl)
                                            .placeholder(R.drawable.default_profile_image)
                                            .into(groupProPic);
                                }
                            });

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //for displaying members
        showPeopleList();

        editGroupDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(GroupProfileActivity.this, GroupEditActivity.class);
                intent.putExtra("groupId",groupChatID);
                startActivity(intent);

            }
        });

        addMembersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(GroupProfileActivity.this, CreateGroupChat_Activity.class);
                intent.putExtra("groupId", groupChatID);
                startActivity(intent);
            }
        });

        exitGroupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(GroupProfileActivity.this);
                builder.setTitle("Do you really want to exit");
                builder.setCancelable(true);
                builder.setPositiveButton("Yes", new MyListener());
                builder.setNegativeButton("Cancel", null);
                builder.show();
            }
        });
    }

    public class MyListener implements DialogInterface.OnClickListener {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            removeFromGroup();
        }

    }

    private void removeFromGroup() {
        //remove from group members
        progressDialog.setMessage("Removing..");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        groupDetailsDatabaseReference.child("members").child(currentUserId).removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            //remove from user chats
                            FirebaseDatabase.getInstance().getReference()
                                    .child("userChats").child(currentUserId).child("public").child(groupChatID)
                                    .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    progressDialog.dismiss();
                                    if (task.isSuccessful()) {
                                        SweetToast.success(GroupProfileActivity.this, "Exited from group.");
                                        Intent intent = new Intent(GroupProfileActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }else {
                                        SweetToast.error(GroupProfileActivity.this,"Failed to exit.");
                                    }
                                }
                            });
                        }
                    }
                });
    }


    private void showPeopleList() {
        DatabaseReference groupMembersDatabaseReference = groupDetailsDatabaseReference.child("members");
        FirebaseRecyclerOptions<Friends> recyclerOptions = new FirebaseRecyclerOptions.Builder<Friends>()
                .setQuery(groupMembersDatabaseReference, Friends.class)
                .build();

        final FirebaseRecyclerAdapter<Friends, FriendsVH> recyclerAdapter = new FirebaseRecyclerAdapter<Friends, FriendsVH>(recyclerOptions) {
            @Override
            protected void onBindViewHolder(@NonNull final FriendsVH holder, int position, @NonNull Friends model) {


                final String userID = getRef(position).getKey();

                userDatabaseReference.child(userID).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                        final String userName = dataSnapshot.child("user_name").getValue().toString();
                        final String userThumbPhoto = dataSnapshot.child("user_thumb_image").getValue().toString();
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
                                .into(holder.profile_thumb, new Callback() {
                                    @Override
                                    public void onSuccess() {
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        Picasso.get()
                                                .load(userThumbPhoto)
                                                .placeholder(R.drawable.default_profile_image)
                                                .into(holder.profile_thumb);
                                    }
                                });

                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                // user active status validation
                                String memberId = dataSnapshot.getKey().toString();
                                userFriendsDatabaseReference.child(memberId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            Intent chatIntent = new Intent(GroupProfileActivity.this, ChatActivity.class);
                                            chatIntent.putExtra("visitUserId", userID);
                                            chatIntent.putExtra("userName", userName);
                                            startActivity(chatIntent);
                                        } else {
                                            Intent profileIntent = new Intent(GroupProfileActivity.this, ProfileActivity.class);
                                            profileIntent.putExtra("visitUserId", userID);
                                            profileIntent.putExtra("userName", userName);
                                            startActivity(profileIntent);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });

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

        membersRecyclerView.setAdapter(recyclerAdapter);
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
}