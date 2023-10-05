package com.sziegui.mpostag424.Utils;

import android.util.Log;

import com.mf.mpos.util.Misc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class TagUtils {
    public static final String LOG_TAG_COMMAND = "APDU_TAG_COMMAND";
    public static final byte[] APDU_CMD_ISO_SELECT = new byte[]{
            (byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x0C, (byte) 0x07, (byte) 0xD2, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x85, (byte) 0x01, (byte) 0x01, (byte) 0x00
    };
    public static final byte[] APDU_CMD_START_AUTH = new byte[]{
            (byte) 0x90, (byte) 0x71, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };
    public static final byte[] DEFAULT_424TAG_KEY = new byte[]{
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };

    private final String AES_ALGORITHM = "AES/CBC/NoPadding";

    private final byte[] INIT_VECTOR = new byte[] {
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };
    private byte[] RandomA;
    private byte[] RandomB;
    private byte[] SessionAuthMACKey;
    private byte[] SessionAuthEncKey;
    private byte[] TransactionIdentifier;
    private final byte[] authKey;

    public TagUtils(byte[] key){
        this.authKey = key;
        printLogTagCommand("Setted key:" + Misc.hex2asc(key));
    }

    public byte[] getRandomA() {
        return RandomA;
    }

    public void setRandomA(byte[] randomA) {
        RandomA = randomA;
    }

    public byte[] getRandomB() {
        return RandomB;
    }

    public byte[] getSessionMACKey() {
        return SessionAuthMACKey;
    }

    private void setSessionMACKey(byte[] sessionMACKey) {
        SessionAuthMACKey = sessionMACKey;
    }

    public byte[] getSessionAuthEncKey() {
        return SessionAuthEncKey;
    }

    private void setSessionEncKey(byte[] sessionEncKey) {
        SessionAuthEncKey = sessionEncKey;
    }

    public byte[] getAuthKey() {
        return authKey;
    }


    public byte[] getFirstPCDStepData(byte[] rndB){
        setRandomA(getRandomDataBytes());
        this.RandomB = decryptBytes(authKey, rndB);
        byte[] RndBShifted = shiftByteArray(RandomB);
        byte[] plainResult = concatByteArrays(RandomA, RndBShifted);
        byte[] encryptedResult = encryptBytes(authKey, plainResult);
        printLogTagCommand("Random A Data: " + Misc.hex2asc(RandomA));
        printLogTagCommand("Random B Data: " + Misc.hex2asc(RandomB));
        printLogTagCommand("Before Encrypted result: " + Misc.hex2asc(plainResult));
        printLogTagCommand("After Encrypted result: " + Misc.hex2asc(encryptedResult));
        return encryptedResult;
    }
    public byte[] getCommandForStepTwo(byte[] data){
        byte[] commandFirstSegment = new byte[]{
                (byte) 0x90, (byte) 0xAF, (byte) 0x00, (byte) 0x00, (byte) 0x20
        };
        byte[] commandLastSegment = new byte[]{0x00};
        return concatByteArrays(commandFirstSegment, concatByteArrays(data, commandLastSegment));
    }
    public void execSecondPCDStep(byte[] encryptedResponse) throws Exception {
        byte[] decryptResult = decryptBytes(authKey, Arrays.copyOfRange( encryptedResponse, 0, 32));
        printLogTagCommand("decrypted response: " + Misc.hex2asc(decryptResult));
        TransactionIdentifier = Arrays.copyOfRange(decryptResult, 0 ,4);
        printLogTagCommand("Transaction Identifier: " + Misc.hex2asc(TransactionIdentifier));
        byte[] shiftedRndADencrypted = Arrays.copyOfRange(decryptResult, 4, 20);
        printLogTagCommand("Shifted RandomA Encrypted: " + Misc.hex2asc(shiftedRndADencrypted));
        byte[] originalRndA = unshiftByteArray(shiftedRndADencrypted);
        printLogTagCommand("Original Random A: " + Misc.hex2asc(originalRndA));
        if(Arrays.equals(RandomA, originalRndA)){
            setSessionKeys();
        }else{
            throw new Exception("El calculo no salio bien :'(");
        }
    }
    private void setSessionKeys() {
        setSessionMACKey(getSessionKey(SessionKeyTypes.MAC));
        setSessionEncKey(getSessionKey(SessionKeyTypes.ENCRYPTED));
    }

    private byte[] shiftByteArray(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return bytes;
        }
        byte[] shiftedBytes = new byte[bytes.length];
        shiftedBytes[shiftedBytes.length - 1] = bytes[0]; // Coloca el primer elemento al final
        System.arraycopy(bytes, 1, shiftedBytes, 0, bytes.length - 1);
        return shiftedBytes;
    }
    private byte[] unshiftByteArray(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return bytes;
        }
        byte[] shiftedBytes = new byte[bytes.length];
        shiftedBytes[0] = bytes[shiftedBytes.length - 1]; // Coloca el primer elemento al final
        System.arraycopy(bytes, 0, shiftedBytes, 1, bytes.length - 1);
        return shiftedBytes;
    }
    private byte[] getRandomDataBytes (){
        byte[] result = new byte[16];
        Random random = new Random();
        for(int i = 0; i<16; i++){
            result[i] =(byte) random.nextInt(255);
        }
        return result;
    }

    private byte[] encryptBytes (byte[] key, byte[] randomBytes){
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(INIT_VECTOR);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

            return cipher.doFinal(randomBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] decryptBytes(byte[] key, byte[] encryptedRndBytes){
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(INIT_VECTOR);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            return cipher.doFinal(encryptedRndBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private byte[] decryptBytesResponse(byte[] key, byte[] encryptedResponse, byte[] IV){
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(IV);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            return cipher.doFinal(encryptedResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public byte[] xor(byte[] array1, byte[] array2) throws IllegalArgumentException{
        if(array1.length != array2.length) throw new IllegalArgumentException("Los array deben tener la misma longitud");
        byte[] result = new byte[array1.length];
        for (int i = 0; i < array1.length; i++) {
            result[i] = (byte) (array1[i] ^ array2[i]);
        }
        return result;
    }

    private byte[] getSessionKey(SessionKeyTypes type) {
        byte[] label;
        String sessionKeyType = "";
        switch (type) {
            case ENCRYPTED:
                label = new byte[]{(byte) 0xA5, (byte) 0x5A, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x80};
                sessionKeyType = "ENC";
                break;
            case MAC:
                label = new byte[]{(byte) 0x5A, (byte) 0xA5, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x80};
                sessionKeyType = "MAC";
                break;
            default:
                return null;
        }
        byte[] context = new byte[32];
        System.arraycopy(label, 0, context, 0,6);
        System.arraycopy(RandomA, 0, context, 6, 2);
        System.arraycopy(RandomB, 0, context, 8, 16);
        System.arraycopy(RandomA, 8, context, 24, 8);
        for(int i = 0; i<6; i++){
            context[8 + i] ^= RandomA[i+2];
        }
        try {
            /* Crear una instancia de AES para cifrar
            Cipher cipher = Cipher.getInstance("AES");
            Key key = new SecretKeySpec(authKey, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
             */
            // Crear una instancia de CMAC
            Mac cmac = Mac.getInstance("AESCMAC");
            cmac.init(new SecretKeySpec(authKey, "AES"));
            // Calcular el CMAC
            byte[] authMACENCKey = cmac.doFinal(context);
            printLogTagCommand("SessionKey created - " + sessionKeyType+": " + Misc.hex2asc(authMACENCKey ));
            return authMACENCKey;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

    }

    private byte[] concatByteArrays(byte[] byteArray1, byte[] byteArray2) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            if (byteArray1 != null) {
                outputStream.write(byteArray1);
            }

            if (byteArray2 != null) {
                outputStream.write(byteArray2);
            }

            return outputStream.toByteArray();
        } catch (IOException e) {
            // Manejar excepciones de E/S si es necesario
            e.printStackTrace();
            return null;
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                // Manejar excepciones de E/S si es necesario
                e.printStackTrace();
            }
        }
    }

    public byte[] getReadFileDataFirstCommand(byte idFile){
        if(SessionAuthEncKey.length == 0 || SessionAuthMACKey.length == 0){
            throw new IllegalArgumentException("No se han creado las llaves de sesion");
        }
        byte[] commandToRead = new byte[]{
                (byte) 0x90, (byte) 0xAD, (byte) 0x00, (byte) 0x00, (byte) 0x0F, idFile, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x30, (byte) 0x00, (byte) 0x00
        };
        byte[] mac = getCMACt(commandToRead[1], new byte[]{(byte) 0x01, 0x00}, Arrays.copyOfRange(commandToRead, 5, 12));
        printLogTagCommand("MAC: " + Misc.hex2asc(mac));
        byte[] resultCommand = concatByteArrays(commandToRead, concatByteArrays(mac, new byte[]{0x00}));
        //byte[] resultCommand = concatByteArrays(commandToRead, new byte[]{0x00});
        printLogTagCommand("Commando para iniciar la lectura del archivo: " + Misc.hex2asc(resultCommand));
        createMACForAPDUExample();
        return resultCommand;
    }
    public void processReadDataResponse(byte[] encryptedResponse){
        printLogTagCommand("Encrypted data" + Misc.hex2asc(encryptedResponse));
        byte [] responseMAC = Arrays.copyOfRange(encryptedResponse, encryptedResponse.length-8, encryptedResponse.length);
        printLogTagCommand("Response MAC: " + Misc.hex2asc(responseMAC));
        byte [] encryptedData = Arrays.copyOfRange(encryptedResponse, 0, encryptedResponse.length-8);
        printLogTagCommand("Encrypted Data: " + Misc.hex2asc(encryptedData));
        byte[] IV_Response = getIVResponse((byte) 0x02);
        printLogTagCommand("IV for decrypt: " + Misc.hex2asc(IV_Response));
        byte[] decryptedData = decryptBytesResponse(SessionAuthEncKey, encryptedData, IV_Response);
        printLogTagCommand("Decrypted data: " + Misc.hex2asc(decryptedData));

    }

    private byte[] getIVResponse(byte commandCounterB) {
        byte[] label = new byte[]{ (byte) 0x5A, (byte) 0xA5 };
        byte[] commandCounter = new byte[]{ commandCounterB, 0x00};
        byte[] padding = new byte[]{ 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        byte[] plainIV = concatByteArrays(label, concatByteArrays(TransactionIdentifier, concatByteArrays(commandCounter, padding)));
        return encryptBytes(SessionAuthEncKey, plainIV);
    }

    private byte[] getCMACt(byte ins, byte[] commandCounter, byte[] commandHeader){
        byte[] MAC_Input = new byte[14];
        MAC_Input[0] = ins;
        System.arraycopy(commandCounter, 0, MAC_Input, 1,2);
        System.arraycopy(TransactionIdentifier, 0, MAC_Input, 3,4);
        System.arraycopy(commandHeader, 0, MAC_Input, 7,7);
        printLogTagCommand("MAC_Input value: " + Misc.hex2asc(MAC_Input));
        try{
            Mac cmac = Mac.getInstance("AESCMAC");
            cmac.init(new SecretKeySpec(SessionAuthMACKey, "AES"));
            // Calcular el CMAC
            byte[] macGenerated = cmac.doFinal(MAC_Input);
            byte[] resultCMAC = new byte[8];
            printLogTagCommand("CMAC created " + Misc.hex2asc(macGenerated ));
            for(int i = 0; i < resultCMAC.length; i++){
                resultCMAC[i] = macGenerated[(i*2)+1];
            }
            printLogTagCommand("MAC Created: " + Misc.hex2asc(resultCMAC));
            return resultCMAC;
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("Error al calcular el mac");
        }
    }

    public void printLogTagCommand(String message){
        Log.i(LOG_TAG_COMMAND, message);
    }

    private enum SessionKeyTypes {
        ENCRYPTED,
        MAC
    }

    public List<byte[]> getSessionKeysManual() {
        List<byte[]> result = new ArrayList<>();
        byte[] sesMACKey = new byte[32];
        byte[] sesENCKey= new byte[32];
        byte[] randomA = new byte[]{
                (byte) 0xB0, (byte) 0x4D, (byte) 0x07, (byte) 0x87, (byte) 0xC9, (byte) 0x3E, (byte) 0xE0, (byte) 0xCC,
                (byte) 0x8C, (byte) 0xAC, (byte) 0xC8, (byte) 0xE8, (byte) 0x6F, (byte) 0x16, (byte) 0xC6, (byte) 0xFE
        };
        byte[] randomB = new byte[]{
                (byte) 0xFA, (byte) 0x65, (byte) 0x9A, (byte) 0xD0, (byte) 0xDC, (byte) 0xA7, (byte) 0x38, (byte) 0xDD,
                (byte) 0x65, (byte) 0xDC, (byte) 0x7D, (byte) 0xC3, (byte) 0x86, (byte) 0x12, (byte) 0xAD, (byte) 0x81
        };
        byte[] MACBytes = new byte[]{
                (byte) 0x5A, (byte) 0xA5, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x80
        };
        byte[] ENCBytes = new byte[]{
                (byte) 0xA5, (byte) 0x5A, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x80
        };
        System.arraycopy(MACBytes, 0, sesMACKey, 0,6);
        System.arraycopy(randomA, 0, sesMACKey, 6, 2);
        System.arraycopy(randomB, 0, sesMACKey, 8, 16);
        System.arraycopy(randomA, 8, sesMACKey, 24, 8);

        System.arraycopy(ENCBytes, 0, sesENCKey, 0,6);
        System.arraycopy(randomA, 0, sesENCKey, 6, 2);
        System.arraycopy(randomB, 0, sesENCKey, 8, 16);
        System.arraycopy(randomA, 8, sesENCKey, 24, 8);

        for(int i = 0; i<6; i++){
            sesMACKey[8 + i] ^= randomA[i+2];
            sesENCKey[8 + i] ^= randomA[i+2];
        }
        try {
           //Crear una instancia de AES para cifrar
            Cipher cipher = Cipher.getInstance("AES");
            Key key = new SecretKeySpec(DEFAULT_424TAG_KEY, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            //Crear una instancia de CMAC
            Mac cmac = Mac.getInstance("AESCMAC");
            cmac.init(new SecretKeySpec(DEFAULT_424TAG_KEY, "AES"));
            //Calcular el CMAC
            byte[] MAC_key = cmac.doFinal(sesMACKey);
            byte[] ENC_key = cmac.doFinal(sesENCKey);
            printLogTagCommand("SessionAuthENCKey: " + Misc.hex2asc(ENC_key));
            printLogTagCommand("SessionAuthMACKey: " + Misc.hex2asc(MAC_key));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public void createMACForAPDUExample(){
        byte[] TI = new byte[]{
                (byte) 0xCD, (byte) 0x73, (byte) 0xD8, (byte) 0xE5
        };
        byte[] cmdCtr = new byte[]{ (byte) 0x01, 0x00};
        byte[] sesAuthMacKey = new byte[]{
                (byte) 0x37, (byte) 0xE7, (byte) 0x23, (byte) 0x4B, (byte) 0x11, (byte) 0xBE, (byte) 0xBE, (byte) 0xFD,
                (byte) 0xE4, (byte) 0x1A, (byte) 0x8F, (byte) 0x29, (byte) 0x00, (byte) 0x90, (byte) 0xEF, (byte) 0x80
        };
        byte[] sesAuthEncKey = new byte[]{
                (byte) 0xFF, (byte) 0xBC, (byte) 0xFE, (byte) 0x1F, (byte) 0x41, (byte) 0x84, (byte) 0x0A, (byte) 0x09,
                (byte) 0xC9, (byte) 0xA8, (byte) 0x8D, (byte) 0x0A, (byte) 0x4B, (byte) 0x10, (byte) 0xDF, (byte) 0x05
        };
        byte[] cmdHeader = new byte[]{
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x30, (byte) 0x00, (byte) 0x00
        };
        byte[] MAC_Input = new byte[14];
        MAC_Input[0] = (byte) 0xAD;
        System.arraycopy(cmdCtr, 0, MAC_Input, 1,2);
        System.arraycopy(TI, 0, MAC_Input, 3,4);
        System.arraycopy(cmdHeader, 0, MAC_Input, 7,7);
        printLogTagCommand("MAC_Input value: " + Misc.hex2asc(MAC_Input));
        try{
            Mac cmac = Mac.getInstance("AESCMAC");
            cmac.init(new SecretKeySpec(sesAuthMacKey, "AES"));
            // Calcular el CMAC
            byte[] macGenerated = cmac.doFinal(MAC_Input);
            byte[] resultCMAC = new byte[8];
            printLogTagCommand("Example - Mac created " + Misc.hex2asc(macGenerated ));
            for(int i = 0; i < resultCMAC.length; i++){
                resultCMAC[i] = macGenerated[(i*2)+1];
            }
            printLogTagCommand("Example - CMAC Created: " + Misc.hex2asc(resultCMAC));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
