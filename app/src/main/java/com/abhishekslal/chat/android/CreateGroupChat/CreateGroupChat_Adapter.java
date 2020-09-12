package com.abhishekslal.chat.android.CreateGroupChat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.abhishekslal.chat.android.R;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CreateGroupChat_Adapter extends  RecyclerView.Adapter<CreateGroupChat_Adapter.NewChatViewHolder>{

    private List<CreateGroupChat_ItemModel> createGroupChatItemList;
    private CreateGroupChat_Listener createGroupChatListener;

    public CreateGroupChat_Adapter(List<CreateGroupChat_ItemModel> createGroupChatItemList, CreateGroupChat_Listener createGroupChatListener) {
        this.createGroupChatItemList = createGroupChatItemList;
        this.createGroupChatListener = createGroupChatListener;
    }

    @NonNull
    @Override
    public NewChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NewChatViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_creategroupchat,parent,false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull NewChatViewHolder holder, int position) {
        holder.bindNewChat(createGroupChatItemList.get(position));
    }

    @Override
    public int getItemCount() {
        return createGroupChatItemList.size();
    }

    public  List<CreateGroupChat_ItemModel> getSelectedNewChats(){
        List<CreateGroupChat_ItemModel> selectedCreateGroupChatItemModels = new ArrayList<>();
        for (CreateGroupChat_ItemModel createGroupChatItemModel : createGroupChatItemList){
            if (createGroupChatItemModel.isSelected){
                selectedCreateGroupChatItemModels.add(createGroupChatItemModel);
            }
        }
        return selectedCreateGroupChatItemModels;
    }

    class NewChatViewHolder extends RecyclerView.ViewHolder{

        LinearLayout layoutNewChats;
        TextView userName;
        TextView userStatus;
        CircleImageView userProPic;
        ImageView imageSelected;

        NewChatViewHolder(@NonNull View itemView){
            super(itemView);
            layoutNewChats = itemView.findViewById(R.id.layoutNewChat);
            userName = itemView.findViewById(R.id.friend_user_name);
            userStatus = itemView.findViewById(R.id.friend_status);
            userProPic = itemView.findViewById(R.id.friend_profile_img);
            imageSelected = itemView.findViewById(R.id.friendSelected);
        }

        void bindNewChat(final CreateGroupChat_ItemModel createGroupChatItemModel){
            userName.setText(createGroupChatItemModel.userName);
            userStatus.setText(createGroupChatItemModel.status);
            Picasso.get()
                    .load(createGroupChatItemModel.Image)
                    .networkPolicy(NetworkPolicy.OFFLINE) // for Offline
                    .placeholder(R.drawable.default_profile_image)
                    .into(userProPic);

            if (createGroupChatItemModel.isSelected){
                imageSelected.setVisibility(View.VISIBLE);
            }
            else {
                imageSelected.setVisibility(View.GONE);
            }
            layoutNewChats.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (createGroupChatItemModel.isSelected){
                        imageSelected.setVisibility(View.GONE);
                        createGroupChatItemModel.isSelected = false;
                        if (getSelectedNewChats().size() == 0){
                            createGroupChatListener.onGroupChatAction(false);
                        }
                    } else {
                        imageSelected.setVisibility(View.VISIBLE);
                        createGroupChatItemModel.isSelected = true;
                        createGroupChatListener.onGroupChatAction(true);
                    }
                }
            });

        }
    }

}
