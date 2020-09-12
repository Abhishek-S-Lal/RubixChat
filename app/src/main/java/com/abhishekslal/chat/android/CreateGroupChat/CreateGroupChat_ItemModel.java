package com.abhishekslal.chat.android.CreateGroupChat;

public class CreateGroupChat_ItemModel {
    String userId;
    String userName,status;
    String Image;
    Boolean isSelected;

    public CreateGroupChat_ItemModel() {
    }

    public CreateGroupChat_ItemModel(String userId, String userName, String status, String image, Boolean isSelected) {
        this.userId = userId;
        this.userName = userName;
        this.status = status;
        Image = image;
        this.isSelected = isSelected;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImage() {
        return Image;
    }

    public void setImage(String image) {
        Image = image;
    }

    public Boolean getSelected() {
        return isSelected;
    }

    public void setSelected(Boolean selected) {
        isSelected = selected;
    }
}
