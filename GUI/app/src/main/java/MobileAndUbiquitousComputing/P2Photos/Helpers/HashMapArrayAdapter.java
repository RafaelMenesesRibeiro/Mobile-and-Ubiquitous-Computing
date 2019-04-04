package MobileAndUbiquitousComputing.P2Photos.Helpers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

@SuppressWarnings("ALL")
class HashMapArrayAdapter extends ArrayAdapter {

    private static class ViewHolder {
        TextView keyText;
        TextView valueText;
    }

    public HashMapArrayAdapter(Context context, int textViewResourceId, List<Map.Entry<String, String>> objects) {
        super(context, textViewResourceId, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.keyText = convertView.findViewById(android.R.id.text1);
            viewHolder.valueText = convertView.findViewById(android.R.id.text2);
            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();

        Map.Entry<String, String> entry = (Map.Entry<String, String>) this.getItem(position);

        viewHolder.keyText.setText(entry.getKey());
        viewHolder.valueText.setText(entry.getValue().toString());
        return convertView;
    }
}