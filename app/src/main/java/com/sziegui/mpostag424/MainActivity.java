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
import com.google.android.material.snackbar.Snackbar;
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
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBluetoothPermissions();
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

    }

    @Override
    protected void onDestroy() {
        Controler.Destory();
        super.onDestroy();
    }

    private void enableBluetooth() {
        if (btAdapter == null || !btAdapter.isEnabled()) {
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            boolean allPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                // Los permisos fueron concedidos, puedes proceder con las operaciones de Bluetooth
                Snackbar.make(findViewById(android.R.id.content), "Ahora si, ya tengo los permisos necesarios", Snackbar.LENGTH_SHORT).show();
            } else {
                // Los permisos no fueron concedidos, muestra un mensaje al usuario o toma una acción adecuada
                // Puedes mostrar un Snackbar o un Toast aquí
                Toast.makeText(this, "Los permisos de Bluetooth no fueron concedidos.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Verificar si los permisos ya están concedidos
            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                // Si los permisos no están concedidos, solicitarlos al usuario
                requestPermissions(new String[]{
                        android.Manifest.permission.BLUETOOTH,
                        android.Manifest.permission.BLUETOOTH_ADMIN,
                        android.Manifest.permission.BLUETOOTH_CONNECT,
                        android.Manifest.permission.BLUETOOTH_SCAN,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                }, REQUEST_BLUETOOTH_PERMISSION);
            } else {
                // Los permisos ya están concedidos, puedes proceder con las operaciones de Bluetooth
                Snackbar.make(findViewById(android.R.id.content), "Ya tengo los permisos necesarios", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BLUETOOTH_ENABLE) {
            // Verificar si el Bluetooth fue activado por el usuario
            if (resultCode != RESULT_OK) {
                // El Bluetooth no fue activado, mostrar un mensaje de error o tomar alguna acción
                Toast.makeText(this, "El Bluetooth no fue activado.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}