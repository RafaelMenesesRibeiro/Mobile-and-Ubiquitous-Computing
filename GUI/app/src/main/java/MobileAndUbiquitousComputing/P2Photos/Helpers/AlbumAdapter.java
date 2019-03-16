package MobileAndUbiquitousComputing.P2Photos.Helpers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import MobileAndUbiquitousComputing.P2Photos.R;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {
    private ArrayList<Photo> albumContent;
    private Context context;

    public AlbumAdapter(Context appContext, ArrayList<Photo> photoList) {
        albumContent = photoList;
        context = appContext;
    }

    @Override
    public int getItemCount() {
        return albumContent.size();
    }

    @NonNull
    @Override
    public AlbumAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cell_layout, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumAdapter.ViewHolder viewHolder, int i) {
        viewHolder.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        viewHolder.image.setImageResource((albumContent.get(i).getId()));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView image;
        ViewHolder(View view) {
            super(view);
            image = view.findViewById(R.id.image);
        }
    }
}
