package com.sziegui.mpostag424;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class ConectDevice extends Fragment {


    public ConectDevice() {
        // Required empty public constructor
    }


    public static ConectDevice newInstance() {
        ConectDevice fragment = new ConectDevice();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_conect_device, container, false);
        return v;
    }
}