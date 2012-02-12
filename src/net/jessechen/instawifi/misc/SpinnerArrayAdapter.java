package net.jessechen.instawifi.misc;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class SpinnerArrayAdapter<T> extends ArrayAdapter<T> {
	private Context c;

	public SpinnerArrayAdapter(Context ctx, T[] objects) {
		super(ctx, android.R.layout.simple_spinner_item, objects);
		c = ctx;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return createViewFromResource(position, convertView, parent, android.R.layout.simple_spinner_item);
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return createViewFromResource(position, convertView, parent,
				android.R.layout.simple_spinner_dropdown_item);
	}

	// overriding private method to force black text
	private View createViewFromResource(int position, View convertView,
			ViewGroup parent, int resource) {
		View view;
		if (convertView == null) {
			LayoutInflater mInflater = (LayoutInflater) c
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = mInflater.inflate(resource, parent, false);
		} else {
			view = convertView;
		}

		TextView text = (TextView) view.findViewById(android.R.id.text1);
		text.setTextColor(Color.BLACK);

		T item = getItem(position);
		if (item instanceof CharSequence) {
			text.setText((CharSequence) item);
		} else {
			text.setText(item.toString());
		}

		return view;
	}
}
