package com.sziegui.mpostag424.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView macAddressTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.device_name);
            macAddressTextView = itemView.findViewById(R.id.device_mac_address );
        }
    }
}
