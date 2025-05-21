package com.example.pdfsignergui;

import java.io.File;

public class SharedState {
    private static SharedState instance;
    private boolean driveFound;
    private File privateKeyFile;

    private SharedState() {}

    public static synchronized SharedState getInstance() {
        if (instance == null) {
            instance = new SharedState();
        }
        return instance;
    }

    public boolean isDriveFound() {
        return driveFound;
    }

    public File getPrivateKey() {
        return privateKeyFile;
    }

    public void setDriveFound(boolean driveFound) {
        this.driveFound = driveFound;
    }

    public void setPrivateKey(File privateKey) {
        this.privateKeyFile = privateKey;
    }
}