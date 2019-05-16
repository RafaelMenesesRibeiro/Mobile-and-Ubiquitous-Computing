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

import org.json.JSONException;

import java.io.IOException;

import cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.CatalogOperations;

public class LimitStorageFragment extends Fragment {
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
        Button done = view.findViewById(R.id.done);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int seekBarValue = seekBar.getProgress();
                try {
                    CatalogOperations.setReplicationLimitInPhotos(activity, seekBarValue);
                }
                catch (IOException | JSONException ex) {
                    // TODO. //
                }
            }
        });
    }
}
