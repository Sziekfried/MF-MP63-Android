package com.sziegui.mpostag424.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProcessBytesFunctions {
    public static byte[] concatByteArrays(byte[] byteArray1, byte[] byteArray2, boolean addEndZeroByte) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            if (byteArray1 != null) {
                outputStream.write(byteArray1);
            }

            if (byteArray2 != null) {
                outputStream.write(byteArray2);
            }
            if(addEndZeroByte) {
                outputStream.write((byte) 0x00);
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
}
