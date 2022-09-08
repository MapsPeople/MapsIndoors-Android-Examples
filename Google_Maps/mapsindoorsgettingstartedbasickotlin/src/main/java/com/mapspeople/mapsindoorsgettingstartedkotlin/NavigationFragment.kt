package com.mapspeople.mapsindoorsgettingstartedkotlin

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.mapsindoors.core.MPRoute

class NavigationFragment : Fragment() {
    private var mRoute: MPRoute? = null
    private var mMapsActivity: MapsActivity? = null

    @Nullable
    override fun onCreateView(inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_navigation, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        val routeCollectionAdapter =
            RouteCollectionAdapter(this)
        val mViewPager: ViewPager2 = view.findViewById(R.id.view_pager)
        mViewPager.adapter = routeCollectionAdapter
        mViewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                //TODO: Add logic to switch between legs of the route
            }
        })

        //Assigning views
        val distanceTxtView = view.findViewById<TextView>(R.id.distanceTxt)
        val infoTxtView = view.findViewById<TextView>(R.id.infoTxt)
        val closeBtn = view.findViewById<ImageButton>(R.id.closeBtn)
        val nextBtn = view.findViewById<ImageButton>(R.id.arrow_next)
        val backBtn = view.findViewById<ImageButton>(R.id.arrow_back)

        //Button for closing the bottom sheet. Clears the route through directionsRenderer as well, and changes map padding.
        closeBtn.setOnClickListener {
            //TODO: Add logic to remove the route on the map and removing this fragment.
        }

        //Next button for going through the legs of the route.
        nextBtn.setOnClickListener {
            mViewPager.setCurrentItem(
                mViewPager.currentItem + 1,
                true
            )
        }

        //Back button for going through the legs of the route.
        backBtn.setOnClickListener {
            mViewPager.setCurrentItem(
                mViewPager.currentItem - 1,
                true
            )
        }

        //TODO: Add text to distanceTxtView and infoTxtView as shown in the tutorial.
    }

    inner class RouteCollectionAdapter(fragment: Fragment?) :
        FragmentStateAdapter(fragment!!) {
        override fun createFragment(position: Int): Fragment {
            return RouteLegFragment.newInstance(mRoute?.legs?.get(position))
        }

        override fun getItemCount(): Int {
            mRoute?.legs?.let { legs->
                return legs.size
            }
            return 0
        }
    }

    companion object {
        fun newInstance(route: MPRoute?, mapsActivity: MapsActivity?): NavigationFragment {
            val fragment = NavigationFragment()
            fragment.mRoute = route
            fragment.mMapsActivity = mapsActivity
            return fragment
        }
    }
}