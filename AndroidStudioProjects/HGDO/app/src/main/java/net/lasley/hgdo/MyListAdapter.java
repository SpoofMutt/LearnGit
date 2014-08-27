package net.lasley.hgdo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Kent on 8/26/2014.
 */
public class MyListAdapter
        extends ArrayAdapter<String> {
  private final Context           context;
  private final ArrayList<String> values;

  public MyListAdapter(Context context, ArrayList<String> values) {
    super(context, R.layout.list_row_layout, values);
    this.context = context;
    this.values = values;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View rowView = inflater.inflate(R.layout.list_row_layout, parent, false);
    TextView textView = (TextView) rowView.findViewById(R.id.mylist1);
    textView.setText(getItem(position));
    return rowView;
  }
}
