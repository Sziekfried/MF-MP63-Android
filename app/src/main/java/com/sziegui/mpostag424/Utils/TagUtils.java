package com.sziegui.mpostag424.Utils;

import android.util.Log;

import com.mf.mpos.util.Misc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import javax.crypto.Cipher;
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
    private byte[] SessionMACKey;
    private byte[] SessionEncKey;
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
        return SessionMACKey;
    }

    private void setSessionMACKey(byte[] sessionMACKey) {
        SessionMACKey = sessionMACKey;
    }

    public byte[] getSessionEncKey() {
        return SessionEncKey;
    }

    private void setSessionEncKey(byte[] sessionEncKey) {
        SessionEncKey = sessionEncKey;
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
        switch (type) {
            case ENCRYPTED:
                label = new byte[]{(byte) 0xA5, (byte) 0x5A, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x80};
                break;
            case MAC:
                label = new byte[]{(byte) 0x5A, (byte) 0xA5, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x80};
                break;
            default:
                return null;
        }
        byte[] context = new byte[32];
        System.arraycopy(RandomA, 0, context, 0, 8);
        System.arraycopy(RandomB, 0, context, 8, 10);
        byte[] xorRndARndB = xor(Arrays.copyOfRange(RandomA, 8, 14), Arrays.copyOfRange(RandomB, 10, 16));
        System.arraycopy(xorRndARndB, 0, context, 18, 6);
        System.arraycopy(RandomA, 14, context, 24, 2);
        System.arraycopy(label, 0, context, 26, 6);
        return encryptBytes(authKey, context);
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

    public void printLogTagCommand(String message){
        Log.i(LOG_TAG_COMMAND, message);
    }

    private enum SessionKeyTypes {
        ENCRYPTED,
        MAC
    }

    public List<byte[]> getSessionKeysManual(){
        List<byte[]> result = new ArrayList<>();
        byte[] sesMACKey;
        byte[] sesENCKey;
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
        byte[] contextBytes = new byte[26];
        System.arraycopy(randomA, 0, contextBytes, 0, 8);
        System.arraycopy(randomB, 0, contextBytes, 8, 10);
        byte[] xorRndARndB = xor(Arrays.copyOfRange(randomA, 8, 14), Arrays.copyOfRange(randomB, 10, 16));
        System.arraycopy(xorRndARndB, 0, contextBytes, 18, 6);
        System.arraycopy(randomA, 14, contextBytes, 24, 2);
        sesMACKey = concatByteArrays(contextBytes, MACBytes);
        sesENCKey = concatByteArrays(contextBytes, ENCBytes);
        result.add(sesMACKey);
        result.add(sesENCKey);
        return result;
    }
}
