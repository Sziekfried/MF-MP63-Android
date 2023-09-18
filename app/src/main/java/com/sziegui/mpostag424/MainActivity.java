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

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_BLUETOOTH_PERMISSION = 117;
    private static final int REQUEST_BLUETOOTH_ENABLE = 234;
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
        if (checkBluetoothPermission()) {
            // Permisos concedidos, continuar con la lógica de Bluetooth
            enableBluetooth();
        } else {
            // Solicitar permisos de Bluetooth
            requestBluetoothPermission();
        }
    }
    private boolean checkBluetoothPermission() {
        // Verificar si se tienen los permisos de Bluetooth
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestBluetoothPermission() {
        // Solicitar permisos de Bluetooth
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION}, REQUEST_BLUETOOTH_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_BLUETOOTH_PERMISSION);
        }
    }

    private void enableBluetooth() {
        // Verificar si el Bluetooth está activado
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            // El Bluetooth no está activado, solicitar activación
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_BLUETOOTH_ENABLE);
        } else {
            // El Bluetooth está activado, continuar con la lógica de Bluetooth
            // ...
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