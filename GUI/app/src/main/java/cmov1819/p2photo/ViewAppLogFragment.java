package cmov1819.p2photo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import cmov1819.p2photo.helpers.managers.LogManager;

public class ViewAppLogFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_app_log, container, false);
        populate(view);
        LogManager.logViewAppLog();
        return view;
    }

    private void populate(final View view) {    
        TextView logText = view.findViewById(R.id.appLogTextBox);
        logText.setMovementMethod(new ScrollingMovementMethod());
        final TextView textBox = view.findViewById(R.id.appLogTextBox);
        Button refreshButton = view.findViewById(R.id.done);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String log = getLogText();
                textBox.setText(log);
            }
        });
        String log = getLogText();
        textBox.setText(log);
    }

    private String getLogText() {
        return LogManager.getAppLog();
    }
}
