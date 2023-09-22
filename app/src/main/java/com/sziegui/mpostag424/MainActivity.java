package com.sziegui.mpostag424;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mf.mpos.pub.CommEnum;
import com.mf.mpos.pub.Controler;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_BLUETOOTH_PERMISSION = 117;
    private static final int REQUEST_BLUETOOTH_ENABLE = 234;
    private BluetoothAdapter btAdapter;
    private BottomNavigationView bottomNavigationView;

    private ConectDevice connect;
    private ManualCommands commands;
    private GetCardData card;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bottomNavigationView = findViewById(R.id.navMPOS);
        connect = new ConectDevice();
        commands = new ManualCommands();
        card = new GetCardData();
        setFragment(connect);
        Controler.Init(this, CommEnum.CONNECTMODE.BLUETOOTH);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.connect_menu:
                    setFragment(connect);
                    return true;
                case R.id.manual_commands:
                    setFragment(commands);
                    return true;
                case R.id.read_ntag424:
                    setFragment(card);
                    return true;
                default:
                    return false;
            }
        });
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // Pedir permiso de Bluetooth si es necesario
        if (btAdapter == null || !btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_BLUETOOTH_ENABLE);
        } else {
            Toast.makeText(this, "Ya esta activado el blutu", Toast.LENGTH_SHORT).show();
            // Bluetooth ya está activado
        }
    }

    @Override
    protected void onDestroy() {
        Controler.Destory();
        super.onDestroy();
    }

    private void enableBluetooth() {
        // Verificar si el Bluetooth está activado
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            // El Bluetooth no está activado, solicitar activación
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_BLUETOOTH_ENABLE);
        }
    }
    private void setFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.mpos_fragment_container, fragment);
        fragmentTransaction.commit();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
                enableBluetooth();
            } else {
                // Permisos no concedidos, mostrar un mensaje de error o tomar alguna acción
                Toast.makeText(this, "Los permisos de Bluetooth no fueron concedidos.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_BLUETOOTH_ENABLE) {
            // Verificar si el Bluetooth fue activado por el usuario
            if (resultCode == RESULT_OK) {
                // El Bluetooth fue activado, continuar con la lógica de Bluetooth
                // ...
            } else {
                // El Bluetooth no fue activado, mostrar un mensaje de error o tomar alguna acción
                Toast.makeText(this, "El Bluetooth no fue activado.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}