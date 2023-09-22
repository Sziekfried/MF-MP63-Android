package com.sziegui.mpostag424.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sziegui.mpostag424.Models.Communication.BluetoothDevice;
import com.sziegui.mpostag424.R;

import java.util.List;

public class BluetoothDeviceAdapter extends RecyclerView.Adapter<BluetoothDeviceAdapter.ViewHolder> {
    private List<BluetoothDevice> deviceList;
    public BluetoothDeviceAdapter(List<BluetoothDevice> deviceList) {
        this.deviceList = deviceList;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bluetooth_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BluetoothDeviceAdapter.ViewHolder holder, int position) {
        BluetoothDevice device = deviceList.get(position);
        holder.nameTextView.setText(device.getName());
        holder.macAddressTextView.setText(device.getMacAddress());
        holder.isSelectedCheck.setChecked(device.isSelected());
        holder.isSelectedCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){
                setSelected(position);
            }
        });
    }
    public void clear() {
        deviceList.clear();
    }
    public Object getItem(int position) {
        return deviceList.get(position);
    }
    @Override
    public int getItemCount() {
        return deviceList.size();
    }
    public void setSelected(int index) {
        int nSize = deviceList.size();
        for (int i = 0; i < nSize; ++i) {
            deviceList.get(i).setSelected(index == i);
        }
        notifyDataSetChanged();
    }

    public int getSelected() {
        int nSize = deviceList.size();

        for (int i = 0; i < nSize; ++i) {
            if (deviceList.get(i).isSelected()) {
                return i;
            }
        }

        return -1;
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox isSelectedCheck;
        TextView nameTextView;
        TextView macAddressTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.device_name);
            macAddressTextView = itemView.findViewById(R.id.device_mac_address );
            isSelectedCheck = itemView.findViewById(R.id.device_isSelected);
        }
    }

}
