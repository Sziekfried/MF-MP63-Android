package com.sziegui.mpostag424.Tags;

import java.util.List;

public class Tag424 {
    private byte[] TagUID;
    private byte[] MasterKey; // 00 KEY
    private List<byte[]> Keys; // KEYS 01 To 04
    private byte[] SesAuthENCKey;
    private byte[] SesAuthMACKey;
    private byte[] TI; //Transaction Identifier
    private byte FileID;
    private int SelectedKeyNumber;
    private byte[] SelectedKey; // Key selected for current transaction
    private byte[] RandomA, RandomB; //Created for generation of SessionAuthKeys
    private byte[] CommandCounter;

    public Tag424() {

    }

    public byte[] getTagUIDCommands(int step){
        switch (step) {
            case 0:
                return new byte[]{(byte) 0x90, 0x60, 0x00, 0x00, 0x00};
            case 1:
            case 2:
                return new byte[]{(byte) 0x90, (byte) 0xAF, 0x00, 0x00, 0x00};
            default:
                throw new IllegalArgumentException("No existe tal paso para este comando");
        }
    }

    public byte[] getTagUID() {
        return TagUID;
    }

    public void setTagUID(byte[] tagUID) {
        TagUID = tagUID;
    }

    public byte[] getMasterKey() {
        return MasterKey;
    }

    public void setMasterKey(byte[] masterKey) {
        MasterKey = masterKey;
    }

    public byte[] getSelectedKey() {
        return SelectedKey;
    }

    public void setSelectedKey(byte[] selectedKey) {
        SelectedKey = selectedKey;
    }


    public int getSelectedKeyNumber() {
        return SelectedKeyNumber;
    }

    public void setSelectedKeyNumber(int selectedKeyNumber) {
        SelectedKeyNumber = selectedKeyNumber;
    }

    public byte getFileID() {
        return FileID;
    }

    public void setFileID(byte fileID) {
        FileID = fileID;
    }

    public byte[] getInitAuthCommand(){
        return new byte[]{
                (byte) 0x90, (byte) 0x71, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00
        };
    }

}
