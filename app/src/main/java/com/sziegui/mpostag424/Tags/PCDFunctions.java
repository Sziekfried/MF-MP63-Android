package com.sziegui.mpostag424.Tags;

import static com.sziegui.mpostag424.Utils.Constants.AES_ALGORITHM_NO_PADDING;
import static com.sziegui.mpostag424.Utils.Constants.ZERO_INIT_VECTOR;
import static com.sziegui.mpostag424.Utils.Constants.logCommandCommunication;
import static com.sziegui.mpostag424.Utils.ProcessBytesFunctions.concatByteArrays;

import com.mf.mpos.util.Misc;
import com.sziegui.mpostag424.Utils.Constants;
import com.sziegui.mpostag424.Utils.TagUtils;

import java.util.Arrays;
import java.util.MissingResourceException;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class PCDFunctions {
    private byte[] KeyForAuth;
    private byte[] SesAuthENCKey;
    private byte[] SesAuthMACKey;
    private byte[] TI;
    private byte[] SelectedKey; // Key selected for current transaction
    private byte[] RandomA, RandomB; //Created for generation of SessionAuthKeys
    private byte[] CommandCounter;

    public PCDFunctions(){

    }
    public byte[] getFirstPCDStepData(byte[] rndB, byte[] key){
        setRandomA(getRandomDataBytes());
        this.KeyForAuth = key;
        this.RandomB = decryptBytes(KeyForAuth, rndB);
        byte[] RndBShifted = shiftByteArray(RandomB);
        logCommandCommunication(Constants.TipoLog.STRING_PROCESADO, Misc.hex2asc(RandomB), "Random B data");
        byte[] plainResult = concatByteArrays(RandomA, RndBShifted, false);
        byte[] encryptedResult = encryptBytes(KeyForAuth, plainResult);
        logCommandCommunication(Constants.TipoLog.INFORMACION, "", "Random A y B procesados");
        return encryptedResult;
    }
    public byte[] getCommandForStepTwo(byte[] data){
        byte[] commandFirstSegment = new byte[]{
                (byte) 0x90, (byte) 0xAF, (byte) 0x00, (byte) 0x00, (byte) 0x20
        };
        return concatByteArrays(commandFirstSegment, data, true);
    }

    public void execSecondPCDStep(byte[] encryptedResponse) throws Exception {
        if(KeyForAuth == null) throw new RuntimeException("No hay ninguna llave pára realizar este paso");
        logCommandCommunication(Constants.TipoLog.HEX_RECIBIDO, Misc.hex2asc(encryptedResponse), "");
        byte[] decryptResult = decryptBytes(KeyForAuth, Arrays.copyOfRange( encryptedResponse, 0, 32));
        logCommandCommunication(Constants.TipoLog.STRING_PROCESADO, Misc.hex2asc(decryptResult),"decrypted response: ");
        setTI( Arrays.copyOfRange(decryptResult, 0 ,4));
        logCommandCommunication(Constants.TipoLog.INFORMACION, Misc.hex2asc(TI), "Transaction Identifier: " );
        byte[] shiftedRndADecrypted = Arrays.copyOfRange(decryptResult, 4, 20);
        logCommandCommunication(Constants.TipoLog.STRING_PROCESADO, Misc.hex2asc(shiftedRndADecrypted),"Shifted RandomA Encrypted: " );
        byte[] originalRndA = unshiftByteArray(shiftedRndADecrypted);
        if(Arrays.equals(RandomA, originalRndA)){
            setSessionKeys();
        }else{
            throw new Exception("Los valores en el PCD y el PICC no coinciden"); //PCD es el celular y MPOS juntos y el PICC es la tarjeta NFC
        }
    }
    private void setSessionKeys(){
        this.SesAuthENCKey = getSessionKey(new byte[]{(byte) 0xA5, (byte) 0x5A}, "Encrypted");
        this.SesAuthMACKey = getSessionKey(new byte[]{(byte) 0x5A, (byte) 0xA5}, "MACed");
    }
    private byte[] getSessionKey(byte[] label, String keyName) {
        byte[] header = new byte[6];
        System.arraycopy(label, 0, header, 0, 2);
        header[2] = (byte) 0x00;
        header[3] = (byte) 0x01;
        header[4] = (byte) 0x00;
        header[5] = (byte) 0x80;
        byte[] context = new byte[32];
        System.arraycopy(header, 0, context, 0,6);
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
            cmac.init(new SecretKeySpec(KeyForAuth, "AES"));
            // Calcular el CMAC
            byte[] sesAuthKeyCreated = cmac.doFinal(context);
            logCommandCommunication(Constants.TipoLog.STRING_PROCESADO , keyName, Misc.hex2asc(sesAuthKeyCreated ));
            return sesAuthKeyCreated;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
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
            IvParameterSpec ivParameterSpec = new IvParameterSpec(ZERO_INIT_VECTOR);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM_NO_PADDING);
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
            IvParameterSpec ivParameterSpec = new IvParameterSpec(ZERO_INIT_VECTOR);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM_NO_PADDING);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            return cipher.doFinal(encryptedRndBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public byte[] getTI() {
        return TI;
    }

    public void setTI(byte[] TI) {
        this.TI = TI;
    }

    public byte[] getSelectedKey() {
        return SelectedKey;
    }

    public void setSelectedKey(byte[] selectedKey) {
        SelectedKey = selectedKey;
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

    public void setRandomB(byte[] randomB) {
        RandomB = randomB;
    }
}
