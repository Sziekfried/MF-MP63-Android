package com.sziegui.mpostag424;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ManualCommands extends Fragment {


    public ManualCommands() {
        // Required empty public constructor
    }

    public static ManualCommands newInstance(String param1, String param2) {
        ManualCommands fragment = new ManualCommands();
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
        return inflater.inflate(R.layout.fragment_manual_commands, container, false);
    }
}