package com.mapspeople.mapsindoorsgettingstartedkotlin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import com.mapsindoors.core.MPRouteLeg

class RouteLegFragment : Fragment() {
    private var mRouteLeg: MPRouteLeg? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_route_leg, container, false)
    }

    override fun onViewCreated(
        view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Assigning views
        val stepsTxtView = view.findViewById<TextView>(R.id.steps_text_view)
        var stepsString = ""
        //A loop to write what to do for each step of the leg.
        for (i in mRouteLeg!!.steps.indices) {
            val routeStep = mRouteLeg!!.steps[i]
            stepsString += """
                Step ${i + 1}${routeStep.maneuver}
                """.trimIndent()
        }

        stepsTxtView.text = stepsString
    }

    companion object {
        fun newInstance(routeLeg: MPRouteLeg?): RouteLegFragment {
            val fragment = RouteLegFragment()
            fragment.mRouteLeg = routeLeg
            return fragment
        }
    }
}