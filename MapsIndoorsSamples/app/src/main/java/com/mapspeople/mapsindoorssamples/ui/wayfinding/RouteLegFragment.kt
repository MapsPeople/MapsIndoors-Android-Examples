package com.mapspeople.mapsindoorssamples.ui.wayfinding

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapspeople.mapsindoorssamples.databinding.FragmentRouteLegBinding
import java.util.*
import java.util.concurrent.TimeUnit

class RouteLegFragment : Fragment() {

    private var _binding: FragmentRouteLegBinding? = null

    private val binding get() = _binding!!

    private var mStep: String? = null
    private var mDuration: Int? = null
    private var mDistance: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentRouteLegBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.stepTextView.text = mStep

        if (Locale.getDefault().country == "US") {
            binding.distanceTextView.text = (mDistance?.times(3.281))?.toInt().toString() + " feet"
        }else {
            binding.distanceTextView.text = mDistance?.toString() + " m"
        }
        mDuration?.let {
            if (it < 60) {
                binding.durationTextView.text = "$it sec"
            }else {
                binding.durationTextView.text = TimeUnit.MINUTES.convert(it.toLong(), TimeUnit.SECONDS).toString() + " min"
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(step: String, distance: Int?, duration: Int?) =
            RouteLegFragment().apply {
                mStep = step
                mDistance = distance
                mDuration = duration
            }
    }

}