package com.desapp.futbolplayerstokens.service;

public class PlayerOverwriteResult {
    private final int rosterPlayersFound;
    private final int modifiedRows;
    private final int insertedRows;

    public PlayerOverwriteResult(int rosterPlayersFound, int modifiedRows, int insertedRows) {
        this.rosterPlayersFound = rosterPlayersFound;
        this.modifiedRows = modifiedRows;
        this.insertedRows = insertedRows;
    }

    public int getRosterPlayersFound() {
        return rosterPlayersFound;
    }

    public int getModifiedRows() {
        return modifiedRows;
    }

    public int getInsertedRows() {
        return insertedRows;
    }
}
