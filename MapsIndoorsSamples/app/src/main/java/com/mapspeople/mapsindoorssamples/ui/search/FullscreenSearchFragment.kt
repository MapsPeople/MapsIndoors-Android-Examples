package com.mapspeople.mapsindoorssamples.ui.search

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.mapsindoors.core.MPQuery
import com.mapsindoors.core.MapsIndoors
import com.mapspeople.mapsindoorssamples.R
import com.mapspeople.mapsindoorssamples.databinding.FragmentFullscreenSearchBinding

/**
 * An example full-screen fragment that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class FullscreenSearchFragment : Fragment() {
    private var _binding: FragmentFullscreenSearchBinding? = null

    private val binding get() = _binding!!
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mLinearLayoutManager: LinearLayoutManager
    private val mAdapter: MPSearchItemRecyclerViewAdapter = MPSearchItemRecyclerViewAdapter()

    private lateinit var searchInputTextView: TextInputEditText

    private var searchHandler: Handler? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentFullscreenSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mRecyclerView = binding.searchList
        mLinearLayoutManager = LinearLayoutManager(requireContext())
        mRecyclerView.apply {
            layoutManager = mLinearLayoutManager
            adapter = mAdapter
        }

        searchInputTextView = binding.searchInputEditText

        val imm = requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        searchInputTextView.addTextChangedListener {
            searchHandler = Handler(Looper.myLooper()!!)
            searchHandler!!.postDelayed(searchRunner, 1000)
        }
        searchInputTextView.setOnEditorActionListener { textView, i, keyEvent ->
            if (i == EditorInfo.IME_ACTION_DONE || i == EditorInfo.IME_ACTION_SEARCH) {
                if (textView.text.isNotEmpty()) {
                    search(textView.text.toString())
                }
                //Making sure keyboard is closed.
                imm.hideSoftInputFromWindow(textView.windowToken, 0)

                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }


        MapsIndoors.getLocationsAsync(null, null) { locations, miError ->
            if (miError == null)  {
                if (!locations.isNullOrEmpty()) {
                    mAdapter.setLocations(locations)
                    mAdapter.notifyDataSetChanged()
                }
            }
        }

        mAdapter.setOnLocationSelectedListener { location ->
            if (location != null) {
                val bundle = Bundle()
                bundle.putString("locationId", location.locationId)
                findNavController().navigate(R.id.action_nav_search_fullscreen_to_nav_search, bundle)
                return@setOnLocationSelectedListener true
            }
            return@setOnLocationSelectedListener false
        }
    }

    private val searchRunner: Runnable = Runnable {
        val text = searchInputTextView.text
        if (text?.length!! >= 2) {
            search(text.toString())
        }
    }

    private fun search(searchText: String) {
        val query = MPQuery.Builder().setQuery(searchText).build()
        MapsIndoors.getLocationsAsync(query, null) { locations, miError ->
            if (miError == null)  {
                if (!locations.isNullOrEmpty()) {
                    mAdapter.setLocations(locations)
                    mAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }

    override fun onPause() {
        super.onPause()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        // Clear the systemUiVisibility flag
        activity?.window?.decorView?.systemUiVisibility = 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}