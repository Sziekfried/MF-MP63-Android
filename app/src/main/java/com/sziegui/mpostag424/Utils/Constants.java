package com.sziegui.mpostag424.Utils;

import android.util.Log;

public class Constants {
    public static final byte CARD_RF = 0x02;
    public static final byte CARD_POWER_ON = 0x01;
    public static final byte CARD_POWER_OFF = 0x02;
    public static final String TAG_APDU_GET_UID = "APDU-GET-UID";

    public static final String LOGGER_TAG_DATA_SEND = "TAG_DATA_SEND";
    public static final String LOGGER_TAG_DATA_RECEIVED = "TAG_DATA_RECEIVED";
    public static final String LOGGER_INFORMATION = "INFORMATION";
    public static final String LOGGER_PROCESS_STATUS = "PROCESS_STATUS";
    public static final byte[] DEFAULT_424TAG_ZEROS_KEY = new byte[]{ 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
    public static final byte[] ZERO_INIT_VECTOR = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
    public static final byte[] ISO_DF_NAME_TAG424 = new byte[]{(byte) 0xD2, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x85, (byte) 0x01, (byte) 0x01 };
    public static final String AES_ALGORITHM_NO_PADDING = "AES/CBC/NoPadding";
    public static final byte[] KEY04_TAG_JOSE = "f/q0HRQsrJ5TfyP+".getBytes();

    public static void logCommandCommunication(TipoLog type, String data, String message){
        switch (type){
            case INFORMACION:
                Log.i(LOGGER_INFORMATION, message);
                break;
            case HEX_ENVIADO:
                Log.d(LOGGER_TAG_DATA_SEND, data + " -- " + message);
                break;
            case HEX_RECIBIDO:
                Log.d(LOGGER_TAG_DATA_RECEIVED, data + " -- " + message);
                break;
            case STRING_PROCESADO:
                Log.w(LOGGER_PROCESS_STATUS, message + " -- " + data);

        }
    }

    public enum TipoLog {
        HEX_ENVIADO("Hex enviado"),
        HEX_RECIBIDO("Hex recibido"),
        STRING_PROCESADO("String procesado"),
        INFORMACION("Información");

        private final String descripcion;

        TipoLog(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }
}
