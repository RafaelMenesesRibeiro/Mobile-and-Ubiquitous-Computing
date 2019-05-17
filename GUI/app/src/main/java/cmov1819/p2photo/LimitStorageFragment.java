package cmov1819.p2photo;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;

import android.support.annotation.NonNull;
import android.widget.TextView;

import org.json.JSONException;

import java.io.IOException;

import cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.CatalogOperations;

import static cmov1819.p2photo.helpers.managers.SessionManager.getMaxCacheImageSize;
import static cmov1819.p2photo.helpers.managers.SessionManager.setMaxCacheImageSize;

public class LimitStorageFragment extends Fragment {
    public static final int DEFAULT_CACHE_VALUE = 256;

    private View view;
    private Activity activity;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.activity = getActivity();
        final View inflaterView = view = inflater.inflate(R.layout.fragment_limit_storage, container, false);
        populate(inflaterView);
        return inflaterView;
    }

    private void populate(final View view) {
        final SeekBar seekBar = view.findViewById(R.id.seekBar);
        seekBar.setMax(256);
        seekBar.setProgress(getMaxCacheImageSize(activity));
        changeSeekText(seekBar.getProgress());
        Button done = view.findViewById(R.id.done);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int seekBarValue = seekBar.getProgress();
                try {
                    setMaxCacheImageSize(activity, seekBarValue);
                    CatalogOperations.setReplicationLimitInPhotos(activity, seekBarValue);
                }
                catch (IOException | JSONException ex) { /* Do nothing. */ }
            }
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                changeSeekText(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { /* Do nothing */ }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { /* Do nothing */ }
        });
    }

    private void changeSeekText(int progress) {
        TextView valueText = view.findViewById(R.id.limitStorageValue);
        valueText.setText("Chosen: " + progress);
    }
}
