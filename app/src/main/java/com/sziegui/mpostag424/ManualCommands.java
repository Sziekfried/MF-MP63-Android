package com.sziegui.mpostag424;

import static com.sziegui.mpostag424.Utils.Constants.TAG_APDU_GET_UID;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mf.mpos.pub.Controler;
import com.mf.mpos.util.Misc;
import com.sziegui.mpostag424.Utils.UtilsUi;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ManualCommands extends Fragment {

    private final byte CARD_RF = 0x02;
    private final byte CARD_POWER_ON = 0x01;
    private final byte CARD_POWER_OFF = 0x02;
    private Button btnShowMessage, btnInitComm, btnEndComm, btnSendHexCommand, btnGetTagUID;
    private TextView txtShowMPOSResponse, txtShowMPOSName, txtShowTagUID;
    private EditText txtInputMessage, txtInputCommand;
    private UtilsUi uiUtils;
    private List<byte[]> APDU_UID_COMMANDS = new ArrayList<byte[]>(){
        {
            add(new byte[]{(byte) 0x90, 0x60, 0x00, 0x00, 0x00});
            add(new byte[]{(byte) 0x90, (byte) 0xAF, 0x00, 0x00, 0x00});
            add(new byte[]{(byte) 0x90, (byte) 0xAF, 0x00, 0x00, 0x00});
        }
    };

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
        View v = inflater.inflate(R.layout.fragment_manual_commands, container, false);
        uiUtils = UtilsUi.getInstance(requireContext());
        initViews(v);
        verifyMPOSConnected();
        addListeners();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        verifyMPOSConnected();
    }

    private void verifyMPOSConnected() {
        if(Controler.posConnected()){
            txtShowMPOSName.setText(MyApplication.getBluetoothName());
        }else{
            uiUtils.showToastMsg("Para enviar comandos primero debes conectarte a un MPOS");
        }
    }

    private void initViews(View v) {
        btnShowMessage = v.findViewById(R.id.btnSetMsgMPOS);
        btnInitComm = v.findViewById(R.id.btnInitCardComm);
        btnEndComm = v.findViewById(R.id.btnEndCardComm);
        btnSendHexCommand = v.findViewById(R.id.btnSetCommandMPOS);
        btnGetTagUID = v.findViewById(R.id.btnGetTagUID);

        txtShowMPOSResponse = v.findViewById(R.id.txtShowResponse);
        txtShowMPOSName = v.findViewById(R.id.txtMposName);
        txtShowTagUID = v.findViewById(R.id.txtShowTagUID);

        txtInputMessage = v.findViewById(R.id.txtMsgToMPOS);
        txtInputCommand = v.findViewById(R.id.txtCommandToMPOS);
    }
    private void addListeners() {
        btnShowMessage.setOnClickListener(v1 ->{
            if(Controler.posConnected()){
                String message = getValidMessage(txtInputMessage);
                showText(message);
            }
        });
        btnInitComm.setOnClickListener(v1 ->{
            String result = Controler.ic_ctrl(CARD_RF, CARD_POWER_ON);
            showResult(result != null ? "Listo para comenzar la comunicacion" : "No se logro iniciar la comunicacion");
        });
        btnEndComm.setOnClickListener(v1 -> {
           String result = Controler.ic_ctrl(CARD_RF, CARD_POWER_OFF);
            showResult(result != null ? "Se ha terminado la comunicacion" : "Error al finalizar la comunicacion vuelve a intentar");
        });
        btnSendHexCommand.setOnClickListener(v1 -> {
            byte[] apduCmd;
            if (txtInputCommand  != null && !txtInputCommand .getText().toString().isEmpty()) {
                apduCmd = Misc.asc2hex(txtInputCommand .getText().toString());
                Log.i("Marker APDU", "is this: " + txtInputCommand .getText().toString());
            } else {
                apduCmd = new byte[]{(byte) 0x90, (byte) 0x51, 0x00, 0x00};
                Log.i("Marker APDU", "is this");
            }
            byte[] response = Controler.ic_cmd(CARD_RF, apduCmd);
            showResult(Misc.hex2asc(response));
        });
        btnGetTagUID.setOnClickListener(v1 -> {
            uiUtils.showSnackbarMsg(requireView(), "Recuerda mantener el Tag pegado al lector (MPOS)");
            if(!Controler.posConnected()) return;
            if(Controler.ic_ctrl(CARD_RF, CARD_POWER_ON) != null){
                try{
                    byte[] response = Controler.ic_cmd(CARD_RF, APDU_UID_COMMANDS.get(0));
                    Log.i(TAG_APDU_GET_UID, "APDU_COMMAND_SENDED: "+ Misc.hex2asc(APDU_UID_COMMANDS.get(0)));
                    Log.i(TAG_APDU_GET_UID, "APDU_COMMAND_RECEIVED: "+ Misc.hex2asc(response));
                    if(Misc.hex2asc(response).endsWith("91AF")){
                        response = Controler.ic_cmd(CARD_RF, APDU_UID_COMMANDS.get(1));
                        Log.i(TAG_APDU_GET_UID, "APDU_COMMAND_SENDED: "+ Misc.hex2asc(APDU_UID_COMMANDS.get(1)));
                        Log.i(TAG_APDU_GET_UID, "APDU_COMMAND_RECEIVED: "+ Misc.hex2asc(response));
                        if(Misc.hex2asc(response).endsWith("91AF")){
                            response = Controler.ic_cmd(CARD_RF, APDU_UID_COMMANDS.get(2));
                            Log.i(TAG_APDU_GET_UID, "APDU_COMMAND_SENDED: "+ Misc.hex2asc(APDU_UID_COMMANDS.get(2)));
                            Log.i(TAG_APDU_GET_UID, "APDU_COMMAND_RECEIVED: "+ Misc.hex2asc(response));
                            if(Misc.hex2asc(response).endsWith("9100")){
                                showResultUID(Misc.hex2asc(response));
                            }else{
                                throw new RuntimeException("La respuesta no fue la esperada");
                            }
                        }else{
                            throw new RuntimeException("La respuesta no fue la esperada");
                        }
                    }else{
                        throw new RuntimeException("La respuesta no fue la esperada");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    String result = Controler.ic_ctrl(CARD_RF, CARD_POWER_OFF);
                    uiUtils.showSnackbarMsg(requireView(), result != null ? "Se ha terminado la comunicacion inesperadamente vuelve a intentar" : "Error vuelve a intentar");
                }
            }else{
                uiUtils.showToastMsg("Acerca la tarjeta para iniciar la comunicacion");
            }
        });
    }

    private void showResultUID(String UID) {
        requireActivity().runOnUiThread(() -> txtShowTagUID.setText(UID.substring(0,14)));
        showText("UID: "+UID.substring(0,14));
        Controler.ic_ctrl(CARD_RF, CARD_POWER_OFF);
    }

    private String getValidMessage(EditText input) {
        if(input.getText().length()<1){
            return "Mensaje de prueba";
        }else{
            return input.getText().toString();
        }
    }

    private void showText(String msg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Controler.CancelComm();
                Controler.ResetPos();
                Controler.screen_show(msg, 30, false, true);
            }
        }).start();
    }
    private byte[] createRandomByteHexArray(int size){
        byte[] result = new byte[size];
        Random random = new Random();
        for (int i = 0; i <= size; i++){
            result[i] = (byte)(random.nextInt(255));
        }
        return result;
    }

    @Override
    public void onPause() {
        super.onPause();
        Controler.CancelComm();
        Controler.ResetPos();
    }
    private void showResult(final String result) {
        requireActivity().runOnUiThread(() -> txtShowMPOSResponse.setText(result));
    }
}