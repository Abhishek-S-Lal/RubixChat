package com.abhishekslal.chat.android.CreateGroupChat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.abhishekslal.chat.android.Home.MainActivity;
import com.abhishekslal.chat.android.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import xyz.hasnat.sweettoast.SweetToast;

public class CreateGroupChat_Activity extends AppCompatActivity implements CreateGroupChat_Listener {

    private int flag;
    private ProgressDialog progressDialog;
    private Button GroupChatBtn;
    private Toolbar toolbar;
    private DatabaseReference rootReference;
    private DatabaseReference userFriendsDatabaseReference;
    private DatabaseReference userDatabaseReference;
    private ValueEventListener friendValueEventListener,userValueEventListener;
    private FirebaseAuth mAuth;

    private String current_user_id, current_user_name;
    private String group_chat_id;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group_chat);

        //intent from already created group add members.
        group_chat_id = getIntent().getStringExtra("groupId");

        toolbar = findViewById(R.id.create_group_chat_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("New Group");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        progressDialog = new ProgressDialog(this);
        RecyclerView newChatsRecyclerView = findViewById(R.id.create_group_chat_friendListRV);
        newChatsRecyclerView.setHasFixedSize(true);
        newChatsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        //for adding members in already created group
        GroupChatBtn = findViewById(R.id.buttonCreateGroup);
        if (group_chat_id != null){
            GroupChatBtn.setText("Add to group");
            getSupportActionBar().setTitle("Select your friends");
        }

        mAuth = FirebaseAuth.getInstance();
        current_user_id = mAuth.getCurrentUser().getUid();

        rootReference = FirebaseDatabase.getInstance().getReference();
        //for friends
        userFriendsDatabaseReference = FirebaseDatabase.getInstance().getReference().child("friends").child(current_user_id);

        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users");

        final List<CreateGroupChat_ItemModel> createGroupChatItemList = new ArrayList<>();
        final CreateGroupChat_Adapter adapter = new CreateGroupChat_Adapter(createGroupChatItemList,this);
        newChatsRecyclerView.setAdapter(adapter);

        friendValueEventListener =new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot datasnapshot : snapshot.getChildren()) {
                    if (datasnapshot.exists()){
                        String userID = datasnapshot.getKey();
                        userValueEventListener = new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()){
                                    CreateGroupChat_ItemModel createGroupChatItemModel = new CreateGroupChat_ItemModel();

                                    createGroupChatItemModel.userId = snapshot.getKey().toString();
                                    createGroupChatItemModel.userName = snapshot.child("user_name").getValue().toString();
                                    createGroupChatItemModel.Image = snapshot.child("user_thumb_image").getValue().toString();
                                    createGroupChatItemModel.status = snapshot.child("user_status").getValue().toString();
                                    createGroupChatItemModel.isSelected = false;
                                    createGroupChatItemList.add(createGroupChatItemModel);
                                    adapter.notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        };
                        userDatabaseReference.child(userID).addValueEventListener(userValueEventListener);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        userFriendsDatabaseReference.addValueEventListener(friendValueEventListener);

        userDatabaseReference.child(current_user_id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    current_user_name = snapshot.child("user_name").getValue().toString();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });




        GroupChatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // following is the list of selected items in recycler view.
                final List<CreateGroupChat_ItemModel> selectedChats = adapter.getSelectedNewChats();
                Calendar myCalendar = Calendar.getInstance();
                SimpleDateFormat currentDate = new SimpleDateFormat("EEEE, dd MMM, yyyy");
                final String date = currentDate.format(myCalendar.getTime());

                if (group_chat_id == null) {


                    // Custom Alert Dialog
                    final AlertDialog.Builder builder = new AlertDialog.Builder(CreateGroupChat_Activity.this);
                    final View v = LayoutInflater.from(CreateGroupChat_Activity.this).inflate(R.layout.dialog_create_group_chat, null);

                    builder.setCancelable(false);
                    final EditText groupName = v.findViewById(R.id.groupName);
                    final EditText groupDescription = v.findViewById(R.id.groupDescription);

                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String group_name = groupName.getText().toString();
                            String group_description = groupDescription.getText().toString();
                            if (TextUtils.isEmpty(group_name)) {
                                SweetToast.error(CreateGroupChat_Activity.this, "Group name is required");
                            } else if (TextUtils.isEmpty(group_description)) {
                                SweetToast.error(CreateGroupChat_Activity.this, "Group description is required");
                            } else {
                                //add new group

                                progressDialog.setMessage("Please wait...");
                                progressDialog.show();
                                progressDialog.setCanceledOnTouchOutside(false);

                                DatabaseReference groupChatReference = rootReference.child("groupChats").push();
                                final String group_id = groupChatReference.getKey();

                                HashMap<String, Object> memberDetails = new HashMap<>();
                                memberDetails.put("added_on", date);
                                memberDetails.put("added_by_uId", current_user_id);
                                memberDetails.put("added_by_uName", current_user_name);
                                rootReference.child("groupChats").child(group_id).child("members").child(current_user_id).setValue(memberDetails); //for adding group creator
                                for (int i = 0; i < selectedChats.size(); i++) {
                                    rootReference.child("groupChats").child(group_id).child("members").child(selectedChats.get(i).userId).setValue(memberDetails);
                                }

                                String newGroupReference = "groupChats/" + group_id;
                                HashMap<String, Object> GroupDetails = new HashMap<>();

                                GroupDetails.put(newGroupReference + "/groupName", group_name);
                                GroupDetails.put(newGroupReference + "/groupId", group_id);
                                GroupDetails.put(newGroupReference + "/groupDescription", group_description);
                                GroupDetails.put(newGroupReference + "/creator_uId", current_user_id);
                                GroupDetails.put(newGroupReference + "/creator_uName", current_user_name);
                                GroupDetails.put(newGroupReference + "/created_at", ServerValue.TIMESTAMP);
                                GroupDetails.put(newGroupReference + "/group_image", "default_group_image");
                                rootReference.updateChildren(GroupDetails, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        flag = 0;
                                        if (databaseError != null) {
                                            flag = 1;
                                        } else {
                                            String group_creator_reference = "userChats/" + current_user_id + "/public/" + group_id;
                                            HashMap<String, Object> userChatsUpdationx = new HashMap<>();
                                            userChatsUpdationx.put(group_creator_reference + "/metadata", "You created this group");
                                            rootReference.updateChildren(userChatsUpdationx, new DatabaseReference.CompletionListener() {
                                                @Override
                                                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                                                    if (databaseError != null) {
                                                        flag = 1;
                                                    }
                                                }
                                            });

                                            for (int i = 0; i < selectedChats.size(); i++) {
                                                String group_member_reference = "userChats/" + selectedChats.get(i).userId + "/public/" + group_id;
                                                HashMap<String, Object> userChatsUpdation = new HashMap<>();
                                                userChatsUpdation.put(group_member_reference + "/metadata", current_user_name + " added you");
                                                rootReference.updateChildren(userChatsUpdation, new DatabaseReference.CompletionListener() {
                                                    @Override
                                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                                                        if (databaseError != null) {
                                                            flag = 1;
                                                        }
                                                    }
                                                });
                                            }
                                            progressDialog.dismiss();
                                            if (flag == 0) {
                                                Intent intent = new Intent(CreateGroupChat_Activity.this, MainActivity.class);
                                                startActivity(intent);
                                                SweetToast.success(CreateGroupChat_Activity.this, "Group created");
                                            } else {
                                                SweetToast.error(CreateGroupChat_Activity.this, "Failed creating group. Please try again");
                                            }
                                        }

                                    }
                                });
                            }

                        }
                    });
                    builder.setView(v);
                    builder.show();
                } else { //if intent from already created group

                    progressDialog.setMessage("Please wait...");
                    progressDialog.show();
                    progressDialog.setCanceledOnTouchOutside(false);

                    final HashMap<String, Object> memberDetails = new HashMap<>();
                    memberDetails.put("added_on", date);
                    memberDetails.put("added_by_uId", current_user_id);
                    memberDetails.put("added_by_uName", current_user_name);

                    for (int i = 0; i < selectedChats.size(); i++) {
                        final DatabaseReference addReference = rootReference.child("groupChats").child(group_chat_id).child("members").child(selectedChats.get(i).userId);
                        final int finalI = i;
                        addReference.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (!snapshot.exists()){

                                    addReference.setValue(memberDetails);
                                    String group_member_reference = "userChats/" + selectedChats.get(finalI).userId + "/public/" + group_chat_id;
                                    HashMap<String, Object> userChatsUpdation = new HashMap<>();
                                    userChatsUpdation.put(group_member_reference + "/metadata", current_user_name + " added you");
                                    rootReference.updateChildren(userChatsUpdation, new DatabaseReference.CompletionListener() {
                                        @Override
                                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                            if (databaseError != null) {

                                            }
                                        }
                                    });

                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                    }

                        SweetToast.success(CreateGroupChat_Activity.this,"Members updated.");




                    progressDialog.dismiss();
                    onBackPressed();
                    finish();
                }

            }
        });
    }

    @Override
    public void onGroupChatAction(Boolean isSelected) {
        if (isSelected){
            GroupChatBtn.setVisibility(View.VISIBLE);
        } else {
            GroupChatBtn.setVisibility(View.GONE);
        }
    }


}