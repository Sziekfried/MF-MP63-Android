package com.sziegui.mpostag424;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mf.mpos.pub.Controler;
import com.mf.mpos.pub.result.ConnectPosResult;
import com.sziegui.mpostag424.Adapters.BluetoothDeviceAdapter;
import com.sziegui.mpostag424.Models.Communication.BluetoothDevice;

import java.util.ArrayList;
import java.util.List;


public class ConectDevice extends Fragment {
    private Button btnSearchDevices, btnConnectDevice, btnDisconnectDevice, btnIsConnected;
    private TextView txtConnectedDeviceName;
    private RecyclerView deviceListView;
    private BluetoothDeviceAdapter adaptador;
    String bluetoothMac = "";
    private String bluetoothName = "";
    private BluetoothAdapter btAdapter;
    List<BluetoothDevice> deviceList;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 277;
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final int REQUEST_LOCATION_PERMISSION = 2;

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
        deviceList = new ArrayList<>();
        adaptador = new BluetoothDeviceAdapter(deviceList);
        checkBluetoothPermission();
        verifyMposConnected();
    }

    private void verifyMposConnected() {
        if(Controler.posConnected()){
            txtConnectedDeviceName.setText(MyApplication.getBluetoothName());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkBluetoothPermission();
        verifyMposConnected();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_conect_device, container, false);
        //if(!checkBluetoothPermission()) requestBluetoothPermission();
        deviceListView = v.findViewById(R.id.device_list);
        btnSearchDevices = v.findViewById(R.id.connectDevice_btnSearch);
        btnConnectDevice = v.findViewById(R.id.connectDevice_btnConnect);
        btnDisconnectDevice = v.findViewById(R.id.connectDevice_btnDisconnect);
        btnIsConnected = v.findViewById(R.id.connectDevice_btnIsConected);
        txtConnectedDeviceName = v.findViewById(R.id.deviceInfo_Name);

        deviceListView.setLayoutManager(new LinearLayoutManager(requireContext()));
        deviceListView.setAdapter(adaptador);

        btnSearchDevices.setOnClickListener(v1 -> {
            scanBluetoothDevices();
        });
        btnConnectDevice.setOnClickListener(v1 ->{
            connectDevice();
        });
        btnDisconnectDevice.setOnClickListener(v1 ->{
            txtConnectedDeviceName.setText("");
            Controler.disconnectPos();
        });
        btnIsConnected.setOnClickListener(v1 -> {
            if(Controler.posConnected()){
                Toast.makeText(requireContext(), "Ya estas conectado a un dispositivo", Toast.LENGTH_SHORT).show();
                txtConnectedDeviceName.setText(MyApplication.getBluetoothName());
            }else{
                Toast.makeText(requireContext(), "Aun no te has conectado a ningun dispositivo", Toast.LENGTH_SHORT).show();
            }
        });
        btAdapter.enable();
        // Registrar el receptor de difusión para recibir los dispositivos Bluetooth encontrados
        IntentFilter filter = new IntentFilter(android.bluetooth.BluetoothDevice.ACTION_FOUND);
        requireActivity().registerReceiver(bluetoothReceiver, filter);

        return v;
    }

    private void connectDevice() {
        if (adaptador.getSelected() >= 0) {
            BluetoothManager bluetoothManager = (BluetoothManager) requireActivity().getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothManager.getAdapter().cancelDiscovery();
            bluetoothMac = ((BluetoothDevice) adaptador.getItem(adaptador.getSelected())).getMacAddress();
            bluetoothName = ((BluetoothDevice) adaptador.getItem(adaptador.getSelected())).getName();
        }else{
            bluetoothMac = MyApplication.getBluetoothMac();
            bluetoothName = MyApplication.getBluetoothName();
        }
        new Thread(() -> {
            if (Controler.posConnected()) {
                Controler.disconnectPos();
            }
            ConnectPosResult ret = Controler.connectPos(bluetoothMac);
            if (ret.bConnected) {
                requireActivity().runOnUiThread(() -> {
                    txtConnectedDeviceName.setText(bluetoothName);
                    MyApplication.setBluetoothMac(bluetoothMac);
                    MyApplication.setBluetoothName(bluetoothName);
                });
            } else {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "No se logro conectar al dispositivo", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void checkBluetoothPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            enableBluetooth();
        }
    }

    private void enableBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager) requireActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            btAdapter = bluetoothManager.getAdapter();
        }

        if (btAdapter == null || !btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            // Verificar si los permisos de Bluetooth fueron concedidos
            boolean permissionGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    permissionGranted = false;
                    break;
                }
            }

            if (permissionGranted) {
                // Permisos concedidos, continuar con la lógica de Bluetooth
                scanBluetoothDevices();
            } else {
                // Permisos no concedidos, mostrar un mensaje de error o tomar alguna acción
                Toast.makeText(requireContext(), "Los permisos de Bluetooth no fueron concedidos.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void scanBluetoothDevices() {
        deviceList.clear();
        adaptador.notifyDataSetChanged();

        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "No tengo permiso pa escanear", Toast.LENGTH_SHORT).show();
            return;
        }
        btAdapter.cancelDiscovery();
        btAdapter.startDiscovery();
        Toast.makeText(getContext(), "Discovering nearby Bluetooth devices...", Toast.LENGTH_SHORT).show();
    }

    // Receptor de difusión para recibir los dispositivos Bluetooth encontrados
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (android.bluetooth.BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Un nuevo dispositivo Bluetooth ha sido encontrado
                android.bluetooth.BluetoothDevice device = intent.getParcelableExtra(android.bluetooth.BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                deviceList.add(new BluetoothDevice(device.getName(), device.getAddress(), false));
                adaptador.notifyDataSetChanged();
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        btAdapter.cancelDiscovery();
    }
}