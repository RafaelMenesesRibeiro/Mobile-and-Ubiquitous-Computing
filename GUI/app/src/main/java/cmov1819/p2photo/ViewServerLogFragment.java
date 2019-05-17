package cmov1819.p2photo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.mediators.P2PWebServerMediator;
import cmov1819.p2photo.msgtypes.BasicResponse;

import static cmov1819.p2photo.dataobjects.RequestData.RequestType.GET_SERVER_LOGS;

public class ViewServerLogFragment extends Fragment {
    private Activity activity;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = getActivity();
        View view = inflater.inflate(R.layout.fragment_view_server_log, container, false);
        populate(view);
        LogManager.logGetServerLog();
        return view;
    }

    private void populate(View view) {
        final TextView textBox = view.findViewById(R.id.serverLogTextBox);
        Button refreshButton = view.findViewById(R.id.done);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String log = getServerLog();
                textBox.setText(log);
            }
        });
        String log = getServerLog();
        textBox.setText(log);
    }

    private String getServerLog() {
        String url = getString(R.string.p2photo_host) + getString(R.string.get_server_log);
        String serverLog = "";
        try {
            RequestData requestData = new RequestData(activity, GET_SERVER_LOGS, url);
            ResponseData responseData = new P2PWebServerMediator().execute(requestData).get();
            if (responseData.getServerCode() == HttpURLConnection.HTTP_OK) {
                BasicResponse payload = responseData.getPayload();
                serverLog = payload.getMessage();
            }
        }
        catch (ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            LogManager.logError(LogManager.GET_SERVER_LOG, ex.getMessage());
        }
        return serverLog;
    }
}
