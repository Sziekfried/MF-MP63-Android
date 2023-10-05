package com.sziegui.mpostag424;

import static com.sziegui.mpostag424.Utils.Constants.KEY04_TAG_JOSE;
import static com.sziegui.mpostag424.Utils.Constants.TAG_APDU_GET_UID;
import static com.sziegui.mpostag424.Utils.TagUtils.APDU_CMD_ISO_SELECT;
import static com.sziegui.mpostag424.Utils.TagUtils.APDU_CMD_START_AUTH;
import static com.sziegui.mpostag424.Utils.TagUtils.DEFAULT_424TAG_KEY;
import static com.sziegui.mpostag424.Utils.TagUtils.LOG_TAG_COMMAND;

import android.app.ProgressDialog;
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

import com.google.android.material.snackbar.Snackbar;
import com.mf.mpos.pub.Controler;
import com.mf.mpos.util.Misc;
import com.sziegui.mpostag424.Utils.TagUtils;
import com.sziegui.mpostag424.Utils.UtilsUi;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ManualCommands extends Fragment {

    private final byte CARD_RF = 0x02;
    private final byte CARD_POWER_ON = 0x01;
    private final byte CARD_POWER_OFF = 0x02;
    private Button btnShowMessage, btnInitComm, btnEndComm, btnSendHexCommand, btnGetTagUID, btnTryAuth, btnReadFile;
    private TextView txtShowMPOSResponse, txtShowMPOSName, txtShowTagUID, txtShowAuthResult, txtShowTagFileData;
    private EditText txtInputMessage, txtInputCommand;
    private UtilsUi uiUtils;
    private TagUtils tag424 = null;
    private ProgressDialog progressDialog;
    private List<byte[]> APDU_UID_COMMANDS = new ArrayList<byte[]>() {
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
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_manual_commands, container, false);
        uiUtils = UtilsUi.getInstance(requireContext());
        initViews(v);
        verifyMPOSConnected();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        verifyMPOSConnected();
    }

    private void verifyMPOSConnected() {
        if (Controler.posConnected()) {
            txtShowMPOSName.setText(MyApplication.getBluetoothName());
        } else {
            uiUtils.showToastMsg("Para enviar comandos primero debes conectarte a un MPOS");
        }
    }

    private void initViews(View v) {
        btnShowMessage = v.findViewById(R.id.btnSetMsgMPOS);
        btnInitComm = v.findViewById(R.id.btnInitCardComm);
        btnEndComm = v.findViewById(R.id.btnEndCardComm);
        btnSendHexCommand = v.findViewById(R.id.btnSetCommandMPOS);
        btnGetTagUID = v.findViewById(R.id.btnGetTagUID);
        btnTryAuth = v.findViewById(R.id.btnTryAuth);
        btnReadFile = v.findViewById(R.id.btnReadFileData);

        txtShowMPOSResponse = v.findViewById(R.id.txtShowResponse);
        txtShowMPOSName = v.findViewById(R.id.txtMposName);
        txtShowTagUID = v.findViewById(R.id.txtShowTagUID);
        txtShowAuthResult = v.findViewById(R.id.txtShowTagAuthResult);
        txtShowTagFileData = v.findViewById(R.id.txtShowTagFileData);

        txtInputMessage = v.findViewById(R.id.txtMsgToMPOS);
        txtInputCommand = v.findViewById(R.id.txtCommandToMPOS);

        addListeners();
    }

    private void addListeners() {
        btnShowMessage.setOnClickListener(v1 -> {
            if (Controler.posConnected()) {
                String message = getValidMessage(txtInputMessage);
                showText(message);
            }
        });
        btnInitComm.setOnClickListener(v1 -> {
            String result = Controler.ic_ctrl(CARD_RF, CARD_POWER_ON);
            showResult(result != null ? "Listo para comenzar la comunicacion" : "No se logro iniciar la comunicacion");
        });
        btnEndComm.setOnClickListener(v1 -> {
            String result = Controler.ic_ctrl(CARD_RF, CARD_POWER_OFF);
            showResult(result != null ? "Se ha terminado la comunicacion" : "Error al finalizar la comunicacion vuelve a intentar");
        });
        btnSendHexCommand.setOnClickListener(v1 -> {
            byte[] apduCmd;
            if (txtInputCommand != null && !txtInputCommand.getText().toString().isEmpty()) {
                apduCmd = Misc.asc2hex(txtInputCommand.getText().toString());
                Log.i("Marker APDU", "is this: " + txtInputCommand.getText().toString());
            } else {
                apduCmd = new byte[]{(byte) 0x90, (byte) 0x51, 0x00, 0x00};
                Log.i("Marker APDU", "is this");
            }
            byte[] response = Controler.ic_cmd(CARD_RF, apduCmd);
            showResult(Misc.hex2asc(response));
        });
        btnGetTagUID.setOnClickListener(v1 -> {
            if (!Controler.posConnected()) return;
            uiUtils.showToastMsg("Recuerda mantener la tarjeta pegada al MPOS");
            getUIDTag();
        });
        btnTryAuth.setOnClickListener(v1 -> {
            if (!Controler.posConnected()) return;
            uiUtils.showToastMsg("Recuerda mantener la tarjeta pegada al MPOS");
            tryAuthenticateTransaction();
        });

        btnReadFile.setOnClickListener(v1 -> {
            if(tag424 == null){
                uiUtils.showToastMsg("Primero debes realizar la autenticacion antes de leer el contenido del tag");
                return;
            }
            if (!Controler.posConnected()) return;
            uiUtils.showToastMsg("Recuerda mantener la tarjeta pegada al MPOS");
            readTagFileData((byte) 0x03);
        });
    }

    private void getUIDTag() {
        progressDialog.setMessage("Cargando...");
        progressDialog.show();
        if (Controler.ic_ctrl(CARD_RF, CARD_POWER_ON) != null) {
            Log.i(TAG_APDU_GET_UID, "Comunicacion iniciada");
            try {
                byte[] response = Controler.ic_cmd(CARD_RF, APDU_UID_COMMANDS.get(0));
                Log.i(TAG_APDU_GET_UID, "APDU_COMMAND_SENDED: " + Misc.hex2asc(APDU_UID_COMMANDS.get(0)));
                Log.i(TAG_APDU_GET_UID, "APDU_COMMAND_RECEIVED: " + Misc.hex2asc(response));
                if (Misc.hex2asc(response).endsWith("91AF")) {
                    response = Controler.ic_cmd(CARD_RF, APDU_UID_COMMANDS.get(1));
                    Log.i(TAG_APDU_GET_UID, "APDU_COMMAND_SENDED: " + Misc.hex2asc(APDU_UID_COMMANDS.get(1)));
                    Log.i(TAG_APDU_GET_UID, "APDU_COMMAND_RECEIVED: " + Misc.hex2asc(response));
                    if (Misc.hex2asc(response).endsWith("91AF")) {
                        response = Controler.ic_cmd(CARD_RF, APDU_UID_COMMANDS.get(2));
                        Log.i(TAG_APDU_GET_UID, "APDU_COMMAND_SENDED: " + Misc.hex2asc(APDU_UID_COMMANDS.get(2)));
                        Log.i(TAG_APDU_GET_UID, "APDU_COMMAND_RECEIVED: " + Misc.hex2asc(response));
                        if (Misc.hex2asc(response).endsWith("9100")) {
                            showResultUID(Misc.hex2asc(response));
                        } else {
                            throw new RuntimeException("La respuesta no fue la esperada");
                        }
                    } else {
                        throw new RuntimeException("La respuesta no fue la esperada");
                    }
                } else {
                    throw new RuntimeException("La respuesta no fue la esperada");
                }
            } catch (Exception e) {
                e.printStackTrace();
                String result = Controler.ic_ctrl(CARD_RF, CARD_POWER_OFF);
                uiUtils.showSnackbarMsg(requireView(), result != null ? "Se ha terminado la comunicacion inesperadamente vuelve a intentar" : "Error vuelve a intentar");
                if (progressDialog.isShowing()) progressDialog.dismiss();
            }
        } else {
            uiUtils.showToastMsg("Acerca la tarjeta para iniciar la comunicacion");

        }
    }

    private void tryAuthenticateTransaction() {
        if (Controler.ic_ctrl(CARD_RF, CARD_POWER_ON) != null) {
        tag424 = new TagUtils(KEY04_TAG_JOSE);
            tag424.printLogTagCommand("Comunicacion iniciada");
            try {
                byte[] response = Controler.ic_cmd(CARD_RF, APDU_CMD_ISO_SELECT);
                if (Misc.hex2asc(response).endsWith("9000")) {
                    tag424.printLogTagCommand("ISO Select DF Application Name Succesfull");
                    response = Controler.ic_cmd(CARD_RF, getCommandToInitAuth((byte) 0x04, APDU_CMD_START_AUTH));
                    if (Misc.hex2asc(response).endsWith("91AF")) {
                        tag424.printLogTagCommand("First auth command executed\n response: " + Misc.hex2asc(response));
                        byte[] encriptedRandomBShifted = Arrays.copyOfRange(response, 0, 16);
                        byte[] data = tag424.getFirstPCDStepData(encriptedRandomBShifted);
                        byte[] secondCommand = tag424.getCommandForStepTwo(data);
                        tag424.printLogTagCommand("Second Auth Command: " + Misc.hex2asc(secondCommand));
                        response = Controler.ic_cmd(CARD_RF, secondCommand);
                        if(Misc.hex2asc(response).endsWith("9100")){
                            tag424.printLogTagCommand("Te autenticaste correctamente:");
                            tag424.printLogTagCommand("La respuesta final fue: " + Misc.hex2asc(response));
                            tag424.execSecondPCDStep(response);
                            txtShowAuthResult.setText("Autenticacion completa");
                        }else{
                            throw new RuntimeException("La respuesta en la segunda parte de la solicitud de autenticacion: " + Misc.hex2asc(response));
                        }
                    } else {
                        throw new RuntimeException("La respuesta en la primera parte de la solicitud de autenticacion: " + Misc.hex2asc(response));
                    }
                } else {
                    throw new RuntimeException("La respuesta en la seleccion del DF AppName: " + Misc.hex2asc(response));
                }
            } catch (Exception e) {
                tag424.printLogTagCommand("Comunicacion finalizada");
                tag424 = null;
                e.printStackTrace();
                String result = Controler.ic_ctrl(CARD_RF, CARD_POWER_OFF);
                uiUtils.showSnackbarMsg(requireView(), result != null ? "Se ha terminado la comunicacion inesperadamente vuelve a intentar" : "Error vuelve a intentar");
                if (progressDialog.isShowing()) progressDialog.dismiss();
            }
        } else {
            uiUtils.showToastMsg("Acerca la tarjeta para iniciar la comunicacion");
        }
    }

    private byte[] getCommandToInitAuth(byte noKey, byte[] commandModel) {
        commandModel[5] = noKey;
        return commandModel;
    }
    private void readTagFileData(byte fileNo) {
        byte[] commandToRead = tag424.getReadFileDataFirstCommand(fileNo);
        byte[] response = Controler.ic_cmd(CARD_RF, commandToRead);
        if (Misc.hex2asc(response).endsWith("9100")) {
            tag424.processReadDataResponse(Arrays.copyOfRange(response, 0, response.length - 2));
        }else{
            tag424.printLogTagCommand("respuesta erronea: " + Misc.hex2asc(response));
            uiUtils.showToastMsg("Error al leer el contenido del tag");
        }
        /*
        if (Controler.ic_ctrl(CARD_RF, CARD_POWER_ON) != null) {
            byte[] commandToRead = tag424.getReadFileDataFirstCommand(fileNo);
            byte[] response = Controler.ic_cmd(CARD_RF, commandToRead);
            if (Misc.hex2asc(response).endsWith("9100")) {
                tag424.processReadDataResponse(Arrays.copyOfRange(response, 0, response.length - 2));
            }else{
                tag424.printLogTagCommand("respuesta erronea: " + Misc.hex2asc(response));
                uiUtils.showToastMsg("Error al leer el contenido del tag");
            }
        }else {
            uiUtils.showToastMsg("Acerca la tarjeta para iniciar la comunicacion");
        }
        */
    }
    private void getTag03FileSettings(){
        byte[] command = new byte[]{
                (byte) 0x90, (byte) 0xF5, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x03, (byte) 0x00
        };
    }

    private void showResultUID(String UID) {
        txtShowTagUID.setText(UID.substring(0, 14));
        if (progressDialog.isShowing()) progressDialog.dismiss();
        Controler.ic_ctrl(CARD_RF, CARD_POWER_OFF);
        Log.i(TAG_APDU_GET_UID, "Comunicacion finalizada");
    }

    private String getValidMessage(EditText input) {
        if (input.getText().length() < 1) {
            return "Mensaje de prueba";
        } else {
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