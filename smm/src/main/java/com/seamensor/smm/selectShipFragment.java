package com.seamensor.smm;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class selectShipFragment extends Fragment
{
    public static final String ITEM_NAME = "aa";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.select_ship_fragment_layout, container, false);
        getActivity().setTitle(getString(R.string.select_ship));
        return rootView;
    }




}
