package com.example.android.earthreport.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.android.earthreport.R;


/**
 * A simple {@link Fragment} subclass.
 */
public class PastDayFragment extends Fragment {

    public final static String YESTERDAY = "Yesterday";

    public PastDayFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_past_day, container, false);
    }

}