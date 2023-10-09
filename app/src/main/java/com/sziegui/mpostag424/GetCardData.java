package com.sziegui.mpostag424;


import static com.sziegui.mpostag424.Tags.Tag424Commands.AUTH_EV_FIRST;
import static com.sziegui.mpostag424.Tags.Tag424Commands.ISO_SELECT_FILE;
import static com.sziegui.mpostag424.Utils.Constants.CARD_POWER_OFF;
import static com.sziegui.mpostag424.Utils.Constants.CARD_POWER_ON;
import static com.sziegui.mpostag424.Utils.Constants.CARD_RF;
import static com.sziegui.mpostag424.Utils.Constants.DEFAULT_424TAG_ZEROS_KEY;
import static com.sziegui.mpostag424.Utils.Constants.ISO_DF_NAME_TAG424;
import static com.sziegui.mpostag424.Utils.Constants.KEY04_TAG_JOSE;
import static com.sziegui.mpostag424.Utils.Constants.LOGGER_INFORMATION;
import static com.sziegui.mpostag424.Utils.Constants.LOGGER_PROCESS_STATUS;
import static com.sziegui.mpostag424.Utils.Constants.LOGGER_TAG_DATA_RECEIVED;
import static com.sziegui.mpostag424.Utils.Constants.LOGGER_TAG_DATA_SEND;
import static com.sziegui.mpostag424.Utils.Constants.logCommandCommunication;
import static com.sziegui.mpostag424.Utils.ProcessBytesFunctions.concatByteArrays;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mf.mpos.pub.Controler;
import com.mf.mpos.util.Misc;
import com.sziegui.mpostag424.Tags.PCDFunctions;
import com.sziegui.mpostag424.Tags.Tag424;
import com.sziegui.mpostag424.Utils.Constants;
import com.sziegui.mpostag424.Utils.UtilsUi;

import java.util.Arrays;
import java.util.Base64;


public class GetCardData extends Fragment {
    private TextView txtShow_BTDeviceName, txtShow_TagUID, txtShow_TagStatus, txtShow_TagResponse;
    private EditText txtInput_KeyNumber, txtInput_KeyBase64Value;
    private Button btnGetUID, btnTryAuth, btnReadFileData;
    private RadioGroup radioGroupKeys, radioGroupFiles;
    private RadioButton radioButton_defaultFile, radioButton_defaultKey;
    private UtilsUi uiUtils;
    private Tag424 tag424;
    private PCDFunctions pcd;
    private boolean TransactionInProgress = false;

    public GetCardData() {
        // Required empty public constructor
    }

    public static GetCardData newInstance() {
        GetCardData fragment = new GetCardData();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiUtils = UtilsUi.getInstance(getContext());
        tag424 = new Tag424();
        pcd = new PCDFunctions();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_get_card_data, container, false);
        attachComponents(v);
        verifyMPOSIsConnected();
        setListeners();
        // Inflate the layout for this fragment
        return v;
    }

    private void attachComponents(View v) {
        txtShow_BTDeviceName = v.findViewById(R.id.cd_txtShow_BtDeviceName);
        txtShow_TagUID = v.findViewById(R.id.cd_txtShow_TagUID);
        txtShow_TagStatus = v.findViewById(R.id.cd_txtShow_TagStatus);
        txtShow_TagResponse = v.findViewById(R.id.cd_txtShow_TagCommunicationResponse);

        txtInput_KeyNumber = v.findViewById(R.id.cd_txtInput_KeyNumber);
        txtInput_KeyBase64Value = v.findViewById(R.id.cd_txtInput_KeyB64Value);

        btnGetUID =v.findViewById(R.id.cd_btnGetUID);
        btnTryAuth = v.findViewById(R.id.cd_btnTryAuth);
        btnReadFileData = v.findViewById(R.id.cd_btnReadFile);

        radioGroupKeys = v.findViewById(R.id.cd_radioBtnGroup_keys);
        radioGroupFiles = v.findViewById(R.id.cd_radioBtnGroup_files);

        radioButton_defaultFile = v.findViewById(R.id.cd_radioBtn_file01);
        radioButton_defaultKey = v.findViewById(R.id.cd_radioBtn_defaultKey);
    }
    private void verifyMPOSIsConnected(){
        if (Controler.posConnected()) {
            txtShow_BTDeviceName.setText(MyApplication.getBluetoothName());
        } else {
            txtShow_BTDeviceName.setText("");
            uiUtils.showToastMsg("Para enviar comandos primero debes conectarte a un MPOS");
        }
    }
    private void setListeners() {
        if (!Controler.posConnected()) return;
        radioGroupKeys.clearCheck();
        radioGroupFiles.clearCheck();
        radioGroupKeys.setOnCheckedChangeListener((radioGroup, idChecked) -> {
            switch (idChecked){
                case R.id.cd_radioBtn_defaultKey:
                    setSelectedKey(DEFAULT_424TAG_ZEROS_KEY, false);
                    tag424.setSelectedKeyNumber(0);
                    break;
                case R.id.cd_radioBtn_settedKey:
                    setSelectedKey(KEY04_TAG_JOSE, false);
                    tag424.setSelectedKeyNumber(4);
                    break;
                case R.id.cd_radioBtn_newKey:
                    setSelectedKey(null, true);
                    break;
                default:
                    uiUtils.showSnackbarMsg(requireView(), "Error inesperado");
                    break;
            }
        });
        radioGroupFiles.setOnCheckedChangeListener((radioGroup, idChecked) -> {
            switch (idChecked){
                case R.id.cd_radioBtn_file01:
                    tag424.setFileID((byte)0x01);
                    break;
                case R.id.cd_radioBtn_file02:
                    tag424.setFileID((byte)0x02);
                    break;
                case R.id.cd_radioBtn_file03:
                    tag424.setFileID((byte)0x03);
                    break;
                default:
                    uiUtils.showSnackbarMsg(requireView(), "No se a que le diste clic");
                    break;
            }
        });
        radioButton_defaultFile.setChecked(true);
        radioButton_defaultKey.setChecked(true);

        btnGetUID.setOnClickListener((v) -> {
            if(!Controler.posConnected()) return;
            if(Controler.ic_ctrl(CARD_RF, CARD_POWER_ON) != null){
                toggleTransactionStatus(true);
                getTagUID();
                txtShow_TagUID.setText(Misc.hex2asc(tag424.getTagUID()));
            }
        });
        btnTryAuth.setOnClickListener((v) -> {
            if(!Controler.posConnected()) return;
            if(!validateSelectedKey()) return;
            tryAuthenticateTransaction();
        });
    }

    private void resetViews(){

    }
    private void updateTagStatus(String status){
        txtShow_TagStatus.setText(status);
    }
    @Override
    public void onResume() {
        super.onResume();
        verifyMPOSIsConnected();
    }
    private void setSelectedKey(byte[] key, boolean fromUI) {
        tag424.setSelectedKey(key);
        toggleEnableInputFields(fromUI);
    }
    private void toggleEnableInputFields(boolean enable) {
        txtInput_KeyBase64Value.setEnabled(enable);
        txtInput_KeyNumber.setEnabled(enable);
        if (!enable){
            txtInput_KeyBase64Value.setText("");
            txtInput_KeyNumber.setText("");
        }
    }
    private void toggleTransactionStatus(boolean activeTransaction){
        this.TransactionInProgress = activeTransaction;
        logCommandCommunication(Constants.TipoLog.INFORMACION, "", activeTransaction ? "Transaccion Iniciada" : "Transaccion finalizada");
        updateTagStatus(activeTransaction ? "En una transaccion" : "Sin ninguna transaccion en proceso");
    }

    private void getTagUID(){
        try {
            for (int i = 0; i < 3; i++) {
                logCommandCommunication(Constants.TipoLog.HEX_ENVIADO, Misc.hex2asc(tag424.getTagUIDCommands(i)), "");
                byte[] response = Controler.ic_cmd(CARD_RF, tag424.getTagUIDCommands(i));
                logCommandCommunication(Constants.TipoLog.HEX_RECIBIDO, Misc.hex2asc(response), "");
                String responseStr = Misc.hex2asc(response);
                if (responseStr.endsWith("91AF") && i < 2) {
                    continue; // Continue to the next iteration
                }
                if (responseStr.endsWith("9100")) {
                    logCommandCommunication(Constants.TipoLog.INFORMACION, "", "UID obtenido exitosamente");
                    tag424.setTagUID(Arrays.copyOfRange(response, 0, response.length - 2));
                } else {
                    throw new RuntimeException("La respuesta no fue la esperada");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            String result = Controler.ic_ctrl(CARD_RF, CARD_POWER_OFF);
            toggleTransactionStatus(result!=null);
            uiUtils.showSnackbarMsg(requireView(), result != null ? "Se ha terminado la comunicación inesperadamente, vuelve a intentar" : "Error, vuelve a intentar");
        }
    }
    private void tryAuthenticateTransaction(){
        try {
            if(!TransactionInProgress){
                if(Controler.ic_ctrl(CARD_RF, CARD_POWER_ON) != null){
                    toggleTransactionStatus(true);
                }else{
                    logCommandCommunication(Constants.TipoLog.INFORMACION, "", "NO se logro iniciar la transaccion");
                    toggleTransactionStatus(false);
                    return;
                }
            }
            byte[] response = Controler.ic_cmd(CARD_RF, Misc.asc2hex(ISO_SELECT_FILE.getCommand()));
            checkResponse(response, "ISO Select DF Application Name Successful");
            byte[] authCommand = getAuthCommandFromKey(getByteIdKeyFromIntId(tag424.getSelectedKeyNumber()));
            response = Controler.ic_cmd(CARD_RF, authCommand);
            checkResponse(response, "First auth command executed ");

            byte[] encryptedRandomBShifted = Arrays.copyOfRange(response, 0, 16);
            byte[] data = pcd.getFirstPCDStepData(encryptedRandomBShifted, tag424.getSelectedKey());
            byte[] secondCommand = pcd.getCommandForStepTwo(data);
            logCommandCommunication(Constants.TipoLog.STRING_PROCESADO, Misc.hex2asc(secondCommand), "Second command generated");

            response = Controler.ic_cmd(CARD_RF, secondCommand);
            checkResponse(response, "Successful authenticated");

            logCommandCommunication(Constants.TipoLog.INFORMACION,"", "Final response: " + Misc.hex2asc(response));
            pcd.execSecondPCDStep(response);
            updateTagStatus("Autenticacion completa");

        } catch (Exception e) {
            e.printStackTrace();
            String result = Controler.ic_ctrl(CARD_RF, CARD_POWER_OFF);
            if(result != null){
                toggleTransactionStatus(false);
                logCommandCommunication(Constants.TipoLog.INFORMACION, "","Communication finished");
            }else{
                Toast.makeText(requireContext(), "Error at end of communication", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private byte getByteIdKeyFromIntId(int selectedKeyNumber) {
        switch (selectedKeyNumber) {
            case 0:
                return (byte) 0x00;
            case 1:
                return (byte) 0x01;
            case 2:
                return (byte) 0x02;
            case 3:
                return (byte) 0x03;
            case 4:
                return (byte) 0x04;
            default:
                throw new IllegalArgumentException("No existe una llave para este ID");
        }
    }

    private byte[] getAuthCommandFromKey(byte keyNumber) {
        byte[] plainCommand = Misc.asc2hex(AUTH_EV_FIRST.getCommand());
        plainCommand[5] = keyNumber;
        return plainCommand;
    }

    private void checkResponse(byte[] response, String successLogMessage) {
        logCommandCommunication(Constants.TipoLog.HEX_RECIBIDO, Misc.hex2asc(response), "Datos recibidos");
        if (Misc.hex2asc(response).endsWith("9100") || Misc.hex2asc(response).endsWith("91AF") || Misc.hex2asc(response).endsWith("9000")) {
            logCommandCommunication(Constants.TipoLog.INFORMACION, "", successLogMessage);
        } else {
            throw new RuntimeException("La respuesta no fue la esperada: " + Misc.hex2asc(response));
        }
    }

    private boolean validateSelectedKey() {
        if (tag424.getSelectedKey() != null) return true;
        String base64Input = txtInput_KeyBase64Value.getText().toString();
        try {
            byte[] newKey = Base64.getDecoder().decode(base64Input);
            tag424.setSelectedKey(newKey);
            tag424.setSelectedKeyNumber(Integer.parseInt(txtInput_KeyNumber.getText().toString()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}