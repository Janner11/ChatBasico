package com.example.chatbasico.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatbasico.R;
import com.example.chatbasico.models.User;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private Context context;

    public UserAdapter(List<User> userList, Context context) {
        this.userList = userList;
        this.context = context;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_select, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.tvUserName.setText(user.getName());
        holder.cbUser.setChecked(user.isSelected());

        holder.itemView.setOnClickListener(v -> {
            user.setSelected(!user.isSelected());
            holder.cbUser.setChecked(user.isSelected());
        });

        holder.cbUser.setOnCheckedChangeListener((buttonView, isChecked) -> {
            user.setSelected(isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public List<User> getSelectedUsers() {
        List<User> selectedUsers = new ArrayList<>();
        for (User user : userList) {
            if (user.isSelected()) {
                selectedUsers.add(user);
            }
        }
        return selectedUsers;
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbUser;
        TextView tvUserName;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            cbUser = itemView.findViewById(R.id.cbUser);
            tvUserName = itemView.findViewById(R.id.tvUserName);
        }
    }
}
