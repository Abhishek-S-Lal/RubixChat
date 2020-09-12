package com.abhishekslal.chat.android.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.abhishekslal.chat.android.Profile.GroupProfileActivity;
import com.abhishekslal.chat.android.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;
import xyz.hasnat.sweettoast.SweetToast;

public class GroupEditActivity extends AppCompatActivity {

    private ProgressDialog progressDialog;
    private String groupId, profile_thumb_download_url;
    private EditText groupName, groupDescription;
    private CircleImageView proPic;
    private ImageView editPhotoIcon;
    private Button saveBtn;
    private DatabaseReference groupChatReference;
    private final static int GALLERY_PICK_CODE = 1;
    private Bitmap thumb_Bitmap = null;
    private StorageReference groupThumbImgRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_edit);

        groupId = getIntent().getStringExtra("groupId");

        Toolbar toolbar = findViewById(R.id.edit_groups_appbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Edit Group Details");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        progressDialog = new ProgressDialog(this);

        groupName = findViewById(R.id.groupEditName);
        groupDescription = findViewById(R.id.groupEditDescription);
        proPic = findViewById(R.id.groupEditProfileImg);
        editPhotoIcon = findViewById(R.id.editGroupPhotoIcon);
        saveBtn = findViewById(R.id.groupEditSaveInfoBtn);

        groupThumbImgRef = FirebaseStorage.getInstance().getReference().child("group_thumb_image");

        groupChatReference = FirebaseDatabase.getInstance().getReference().child("groupChats").child(groupId);
        groupChatReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String group_name = snapshot.child("groupName").getValue().toString();
                String group_description = snapshot.child("groupDescription").getValue().toString();
                String group_pro_pic = snapshot.child("group_image").getValue().toString();

                groupName.setText(group_name);
                groupName.setSelection(groupName.getText().length());

                groupDescription.setText(group_description);
                groupDescription.setSelection(groupDescription.getText().length());

                if (!group_pro_pic.equals("default_image")) { // default image condition for new user
                    Picasso.get()
                            .load(group_pro_pic)
                            .networkPolicy(NetworkPolicy.OFFLINE) // for offline
                            .placeholder(R.drawable.default_profile_image)
                            .error(R.drawable.default_profile_image)
                            .into(proPic);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        /** Change profile photo from GALLERY */
        editPhotoIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // open gallery
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(galleryIntent, GALLERY_PICK_CODE);
            }
        });

        /** Edit information */
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String gName = groupName.getText().toString();
                String gDescription = groupDescription.getText().toString();

                saveInformation(gName, gDescription);
            }
        });

    }

    private void saveInformation(String gName, String gDescription) {
        if (TextUtils.isEmpty(gName)) {
            SweetToast.error(this, "Oops! group name can't be empty");
        } else if (TextUtils.isEmpty(gDescription)) {
            SweetToast.warning(this, "Oops! group description can't be empty");
        } else {
            groupChatReference.child("groupName").setValue(gName);
            groupChatReference.child("groupDescription").setValue(gDescription);
            SweetToast.success(GroupEditActivity.this,"Saved successfully");
            Intent intent = new Intent(GroupEditActivity.this, GroupProfileActivity.class);
            intent.putExtra("visitUserId",groupId);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /** Cropping image functionality
         * Library Link- https://github.com/ArthurHub/Android-Image-Cropper
         * */
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_PICK_CODE && resultCode == RESULT_OK && data != null) {
           Uri imageUri = data.getData();
            // start picker to get image for cropping and then use the image in cropping activity
            CropImage.activity(imageUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(this);
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {
                progressDialog.setMessage("Please wait...");
                progressDialog.show();

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

                // firebase storage for uploading the cropped and compressed image
                final StorageReference thumb_filePath = groupThumbImgRef.child(groupId + "jpg");
                UploadTask thumb_uploadTask = thumb_filePath.putBytes(thumb_byte);

                Task<Uri> thumbUriTask = thumb_uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (!task.isSuccessful()) {
                            SweetToast.error(GroupEditActivity.this, "Thumb Image Error: " + task.getException().getMessage());
                        }
                        profile_thumb_download_url = thumb_filePath.getDownloadUrl().toString();
                        return thumb_filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        profile_thumb_download_url = task.getResult().toString();

                        if (task.isSuccessful()) {
                            //thumb profile updated
                            HashMap<String, Object> update_user_data = new HashMap<>();
                            update_user_data.put("group_image", profile_thumb_download_url);

                            groupChatReference.updateChildren(new HashMap<String, Object>(update_user_data))
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.e("tag", "thumb profile updated");
                                            progressDialog.dismiss();
                                            SweetToast.success(GroupEditActivity.this, "Image updated successfully.");
                                            Intent intent = new Intent(GroupEditActivity.this, GroupProfileActivity.class);
                                            intent.putExtra("visitUserId",groupId);
                                            startActivity(intent);
                                            finish();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e("tag", "for thumb profile: " + e.getMessage());
                                    progressDialog.dismiss();
                                }
                            });
                        }

                    }
                });

            }

        }
        if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            //Exception error = result.getError();
            // handling more event
            SweetToast.info(GroupEditActivity.this, "Image cropping failed.");
        }
    }
}

