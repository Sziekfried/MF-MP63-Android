package com.sziegui.mpostag424.Models.Communication;

public class BluetoothDevice {
    String name;
    String macAddress;
    boolean selected;

    public BluetoothDevice(String name, String macAddress, boolean isSelected) {
        this.name = name;
        this.macAddress = macAddress;
        this.selected = isSelected;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
