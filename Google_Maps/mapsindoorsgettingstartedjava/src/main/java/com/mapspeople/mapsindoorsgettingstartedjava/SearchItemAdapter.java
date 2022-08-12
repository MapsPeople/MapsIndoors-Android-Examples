package com.mapspeople.mapsindoorsgettingstartedjava;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mapsindoors.coresdk.MPDisplayRule;
import com.mapsindoors.coresdk.MPLocation;
import com.mapsindoors.coresdk.MapsIndoors;

import java.util.List;

class SearchItemAdapter extends RecyclerView.Adapter<ViewHolder> {

    private final List<MPLocation> mLocations;
    private final MapsActivity mMapActivity;

    SearchItemAdapter(List<MPLocation> locationList, MapsActivity activity) {
        mLocations = locationList;
        mMapActivity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        //Setting the the text on the text view to the name of the location
        holder.text.setText(mLocations.get(position).getName());

        holder.itemView.setOnClickListener(view -> {
            mMapActivity.createRoute(mLocations.get(position));
            //Clearing map to remove the location filter from our search result
            mMapActivity.getMapControl().clearFilter();
        });

        if (mMapActivity != null) {
            //We start by checking if there is a specific Location icon assigned to the location
            MPDisplayRule locationDisplayRule = MapsIndoors.getDisplayRule(mLocations.get(position));

            if (locationDisplayRule != null) {
                locationDisplayRule.getIconAsync(bitmap -> mMapActivity.runOnUiThread(() -> {
                    holder.imageView.setImageBitmap(bitmap);
                }));
            }
        }
    }

    @Override
    public int getItemCount() {
        return mLocations.size();
    }

}

class ViewHolder extends RecyclerView.ViewHolder {

    final TextView text;
    final ImageView imageView;

    ViewHolder(LayoutInflater inflater, ViewGroup parent) {
        super(inflater.inflate(R.layout.fragment_search_list_item, parent, false));
        text = itemView.findViewById(R.id.text);
        imageView = itemView.findViewById(R.id.location_image);
    }
}