package cmov1819.p2photo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

import cmov1819.p2photo.helpers.managers.LogManager;

public class ListUsersFragment extends Fragment {
    public static final String USERS_EXTRA = "users";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_users, container, false);

        ArrayList<String> usersList = new ArrayList<>();
        usersList.add("No users found.");
        ArrayList<String> users;
        if (getArguments() != null && (users = getArguments().getStringArrayList(USERS_EXTRA)) != null) {
             usersList = users;
        }

        ListView usersListView = view.findViewById(R.id.usersList);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, usersList);
        usersListView.setAdapter(adapter);

        LogManager.logListUsers();
        return view;
    }
}
