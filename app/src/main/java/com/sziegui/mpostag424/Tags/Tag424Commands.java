package com.sziegui.mpostag424.Tags;

public enum Tag424Commands {
    ISO_SELECT_FILE("00A4040C07D276000085010100"),
    AUTH_EV_FIRST("9071000002000000");
    private final String command;

    Tag424Commands(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}
