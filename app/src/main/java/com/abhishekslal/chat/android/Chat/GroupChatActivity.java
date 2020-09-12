package com.abhishekslal.chat.android.Chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.abhishekslal.chat.android.Adapter.MessageAdapter;
import com.abhishekslal.chat.android.Model.Message;
import com.abhishekslal.chat.android.Profile.GroupProfileActivity;
import com.abhishekslal.chat.android.R;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;
import xyz.hasnat.sweettoast.SweetToast;

public class GroupChatActivity extends AppCompatActivity {

    private String groupChatID;
    private String groupChatName;

    private Toolbar chatToolbar;
    private TextView groupNameTV;
    private TextView groupMembersTV, ChatConnectionTV, statusTV;
    private CircleImageView groupImageView;

    private DatabaseReference rootReference;

    // sending message
    private ImageView send_messageIVBtn, send_image;
    private EditText input_user_message;
    private FirebaseAuth mAuth;
    private String messageSenderId, download_url;

    private RecyclerView mMessagesList;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private final List<Message> messagesList = new ArrayList<>();
    private LinearLayoutManager mLinearLayoutManager;
    private MessageAdapter mMessageAdapter;

    public static final int TOTAL_ITEM_TO_LOAD = 10;
    private int mCurrentPage = 1;

    //Solution for descending list on refresh
    private int itemPos = 0;
    private String mLastKey = "";
    private String mPrevKey = "";
    private Bitmap thumb_Bitmap = null;
    private final static int GALLERY_PICK_CODE = 2;
    private StorageReference imageMessageStorageRef;

    private ConnectivityReceiver connectivityReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        rootReference = FirebaseDatabase.getInstance().getReference();

        mAuth = FirebaseAuth.getInstance();
        messageSenderId = mAuth.getCurrentUser().getUid();

        //-----GETING FROM INTENT----
        groupChatID = getIntent().getExtras().get("visitUserId").toString();
        groupChatName = getIntent().getExtras().get("userName").toString();

        imageMessageStorageRef = FirebaseStorage.getInstance().getReference().child("messages_image");

        // appbar / toolbar
        chatToolbar = findViewById(R.id.group_chats_appbar);
        setSupportActionBar(chatToolbar);
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater = (LayoutInflater)
                this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.appbar_chat, null);
        actionBar.setCustomView(view);

        chatToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        ChatConnectionTV = findViewById(R.id.group_ChatConnectionTV);
        statusTV = findViewById(R.id.group_StatusTV);
        groupNameTV = findViewById(R.id.chat_user_name);
        groupMembersTV = findViewById(R.id.chat_active_status);
        groupImageView = findViewById(R.id.chat_profile_image);

        // sending message declaration
        send_messageIVBtn = findViewById(R.id.group_c_send_message_BTN);
        send_image = findViewById(R.id.group_c_send_image_BTN);
        input_user_message = findViewById(R.id.group_c_input_message);

        // setup for showing messages
        mMessageAdapter = new MessageAdapter(messagesList);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.group_message_swipe_layout);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        //linearLayoutManager.setReverseLayout(true);
        mMessagesList = findViewById(R.id.group_message_list);
        mMessagesList.setLayoutManager(mLinearLayoutManager);
        mMessagesList.setHasFixedSize(true);
        mMessagesList.setAdapter(mMessageAdapter);

        loadMessages();

//        ADDING VALUES TO TOOLBAR
        groupNameTV.setText(groupChatName);
        rootReference.child("groupChats").child(groupChatID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            final String group_description = dataSnapshot.child("groupDescription").getValue().toString();
                            final String group_image = dataSnapshot.child("group_image").getValue().toString();

                            // show image on appbar
                            Picasso.get()
                                    .load(group_image)
                                    .networkPolicy(NetworkPolicy.OFFLINE) // for Offline
                                    .placeholder(R.drawable.default_profile_image)
                                    .into(groupImageView, new Callback() {
                                        @Override
                                        public void onSuccess() {
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            Picasso.get()
                                                    .load(group_image)
                                                    .placeholder(R.drawable.default_profile_image)
                                                    .into(groupImageView);
                                        }
                                    });


                            groupMembersTV.setText(group_description);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
        groupImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent profileIntent = new Intent(GroupChatActivity.this, GroupProfileActivity.class);
                profileIntent.putExtra("visitUserId", groupChatID);
                startActivity(profileIntent);
            }
        });


//       SEND TEXT MESSAGE BUTTON
        send_messageIVBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });


        /** SEND IMAGE MESSAGE BUTTON */
        send_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK);
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(galleryIntent, GALLERY_PICK_CODE);
            }
        });

        //----LOADING 10 MESSAGES ON SWIPE REFRESH----
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                itemPos = 0;
                mCurrentPage++;
                loadMoreMessages();
                ;

            }
        });

    }

    @Override // for gallery picking
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //  For image sending

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_PICK_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();

            CropImage.activity(imageUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .start(this);
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {

                final Uri resultUri = result.getUri();

                File thumb_filePath_Uri = new File(resultUri.getPath());


                /**
                 * compress image using compressor library
                 * link - https://github.com/zetbaitsu/Compressor
                 * */
                try {
                    thumb_Bitmap = new Compressor(this)
                            .setMaxWidth(200)
                            .setMaxHeight(200)
                            .setQuality(45)
                            .compressToBitmap(thumb_filePath_Uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // firebase storage for uploading the cropped image


                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                thumb_Bitmap.compress(Bitmap.CompressFormat.JPEG, 45, outputStream);
                final byte[] thumb_byte = outputStream.toByteArray();


                DatabaseReference user_message_key = rootReference.child("messages").child(groupChatID).push();
                final String message_push_id = user_message_key.getKey();

                final StorageReference file_path = imageMessageStorageRef.child(message_push_id + ".jpg");

                statusTV.setText("Sending Image...");
                statusTV.setTextColor(Color.WHITE);
                statusTV.setBackgroundColor(Color.BLUE);
                statusTV.setVisibility(View.VISIBLE);

                UploadTask uploadTask = file_path.putBytes(thumb_byte);
                Task<Uri> uriTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (!task.isSuccessful()) {
                            SweetToast.error(GroupChatActivity.this, "Error: " + task.getException().getMessage());
                        }
                        download_url = file_path.getDownloadUrl().toString();
                        return file_path.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            if (task.isSuccessful()) {
                                download_url = task.getResult().toString();
                                //Toast.makeText(ChatActivity.this, "From ChatActivity, link: " +download_url, Toast.LENGTH_SHORT).show();

                                String message_reference = "messages/" + groupChatID + "/" + message_push_id;
                                HashMap<String, Object> message_text_body = new HashMap<>();
                                message_text_body.put("message", download_url);
                                message_text_body.put("seen", false);
                                message_text_body.put("type", "image");
                                message_text_body.put("time", ServerValue.TIMESTAMP);
                                message_text_body.put("from", messageSenderId);
                                message_text_body.put("chat_type", "group");
                                message_text_body.put("chat_id", groupChatID);

                                HashMap<String, Object> messageBodyDetails = new HashMap<>();
                                messageBodyDetails.put(message_reference, message_text_body);

                                rootReference.updateChildren(messageBodyDetails, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        if (databaseError != null) {
                                            Log.e("from_image_chat: ", databaseError.getMessage());
                                        }
                                        input_user_message.setText("");
                                    }
                                });
                                SweetToast.success(GroupChatActivity.this, "Image sent successfully");
                                statusTV.setVisibility(View.GONE);
                            } else {
                                SweetToast.warning(GroupChatActivity.this, "Failed to send image. Try again");
                            }
                        }
                    }
                });
            }
        }
        if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            //Exception error = result.getError();
            // handling more event
            SweetToast.info(GroupChatActivity.this, "Image cropping failed.");
        }
    }

    //---FIRST 10 MESSAGES WILL LOAD ON START----
    private void loadMessages() {

        DatabaseReference messageRef = rootReference.child("messages").child(groupChatID);
        Query messageQuery = messageRef.limitToLast(mCurrentPage * TOTAL_ITEM_TO_LOAD);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.exists()) {
                    Message message = dataSnapshot.getValue(Message.class);

                    itemPos++;
                    if (itemPos == 1) {
                        String mMessageKey = dataSnapshot.getKey();

                        mLastKey = mMessageKey;
                        mPrevKey = mMessageKey;
                    }

                    messagesList.add(message);
                    mMessageAdapter.notifyDataSetChanged();
                    mMessagesList.scrollToPosition(messagesList.size() - 1);
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        messageQuery.keepSynced(true);
    }

    //---ON REFRESHING 10 MORE MESSAGES WILL LOAD----
    private void loadMoreMessages() {

        DatabaseReference messageRef = rootReference.child("messages").child(groupChatID);
        Query messageQuery = messageRef.orderByKey().endAt(mLastKey).limitToLast(10);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Message message = (Message) dataSnapshot.getValue(Message.class);
                String messageKey = dataSnapshot.getKey();

                if (!mPrevKey.equals(messageKey)) {
                    messagesList.add(itemPos++, message);
                } else {
                    mPrevKey = mLastKey;
                }

                if (itemPos == 1) {
                    String mMessageKey = dataSnapshot.getKey();
                    mLastKey = mMessageKey;
                }


                mMessageAdapter.notifyDataSetChanged();

                mSwipeRefreshLayout.setRefreshing(false);

                mLinearLayoutManager.scrollToPositionWithOffset(10, 0);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void sendMessage() {
        String message = input_user_message.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            SweetToast.info(GroupChatActivity.this, "Please type a message");
        } else {

            DatabaseReference user_message_key = rootReference.child("messages").child(groupChatID).push();
            String message_push_id = user_message_key.getKey();

            String message_reference = "messages/" + groupChatID + "/" + message_push_id;

            HashMap<String, Object> message_text_body = new HashMap<>();
            message_text_body.put("message", message);
            message_text_body.put("seen", false);
            message_text_body.put("type", "text");
            message_text_body.put("time", ServerValue.TIMESTAMP);
            message_text_body.put("from", messageSenderId);
            message_text_body.put("chat_type", "group");
            message_text_body.put("chat_id", groupChatID);

            HashMap<String, Object> messageBodyDetails = new HashMap<>();

            messageBodyDetails.put(message_reference, message_text_body);

            rootReference.updateChildren(messageBodyDetails, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                    if (databaseError != null) {
                        Log.e("Sending message", databaseError.getMessage());
                    }
                    input_user_message.setText("");
                }
            });
        }
    }


    // Broadcast receiver for network checking
    public class ConnectivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            ChatConnectionTV.setVisibility(View.GONE);
            if (networkInfo != null && networkInfo.isConnected()) {
//                ChatConnectionTV.setText("Internet connected");
//                ChatConnectionTV.setTextColor(Color.BLACK);
//                ChatConnectionTV.setBackgroundColor(Color.GREEN);
//                ChatConnectionTV.setVisibility(View.VISIBLE);
//
//                // LAUNCH activity after certain time period
//                new Timer().schedule(new TimerTask(){
//                    public void run() {
//                        ChatActivity.this.runOnUiThread(new Runnable() {
//                            public void run() {
//                                ChatConnectionTV.setVisibility(View.GONE);
//                            }
//                        });
//                    }
//                }, 1200);
            } else {
                ChatConnectionTV.setText("No internet connection! ");
                ChatConnectionTV.setTextColor(Color.WHITE);
                ChatConnectionTV.setBackgroundColor(Color.RED);
                ChatConnectionTV.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Register Connectivity Broadcast receiver
        connectivityReceiver = new ConnectivityReceiver();
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unregister Connectivity Broadcast receiver
        unregisterReceiver(connectivityReceiver);
    }


}

