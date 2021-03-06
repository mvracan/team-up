package hr.foi.teamup.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * all adapters extend the base adapter
 * Created by Tomislav Turek on 06.12.15..
 */
public abstract class BaseAdapter<T extends Serializable> extends ArrayAdapter<T> {

    private LayoutInflater inflater;
    private ArrayList<T> items;

    public BaseAdapter(Context context, int resource, ArrayList<T> items) {
        super(context, resource);
        this.items = items;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public T getItem(int position) {
        return items.get(position);
    }

    @Override
    public int getPosition(T item) {
        for(int i = 0; i < items.size(); i++) {
            if(items.get(i) == item) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public LayoutInflater getInflater() {
        return inflater;
    }

    public ArrayList<T> getItems() {
        return items;
    }
}
