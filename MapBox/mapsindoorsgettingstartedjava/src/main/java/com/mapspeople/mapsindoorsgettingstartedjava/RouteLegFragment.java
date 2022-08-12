package com.mapspeople.mapsindoorsgettingstartedjava;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mapsindoors.coresdk.MPRouteLeg;
import com.mapsindoors.coresdk.MPRouteStep;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RouteLegFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RouteLegFragment extends Fragment {
    private MPRouteLeg mRouteLeg;

    public static RouteLegFragment newInstance(MPRouteLeg routeLeg) {
        RouteLegFragment fragment = new RouteLegFragment();
        fragment.mRouteLeg = routeLeg;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_route_leg, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Assigning views
        TextView stepTxtView = view.findViewById(R.id.steps_text_view);
        String stepsString = "";
        //A loop to write what to do for each step of the leg.
        for (int i = 0; i < mRouteLeg.getSteps().size(); i++) {
            MPRouteStep routeStep = mRouteLeg.getSteps().get(i);
            stepsString += "Step " + (i + 1) + " " + routeStep.getManeuver() + "\n";
        }
        stepTxtView.setText(stepsString);
    }
}