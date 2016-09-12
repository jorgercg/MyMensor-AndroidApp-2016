package com.seamensor.smm;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class configureSelectedShipFragment extends Fragment
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.select_ship_fragment_layout, container, false);
        getActivity().setTitle(getString(R.string.configure_selected_ship));
        return rootView;
    }

}
