package org.bvrit.evemythra.modules;

import android.support.v4.app.Fragment;

import org.bvrit.evemythra.fragments.MapsFragment;

/**
 * User: mohit
 * Date: 13/6/15
 */
public class GoogleMapModule implements MapModule {
    /**
     * This guy should not really cache anything
     */
    @Override
    public Fragment provideMapFragment() {
        return new MapsFragment();
    }
}
