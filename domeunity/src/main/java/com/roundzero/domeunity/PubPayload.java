package com.roundzero.domeunity;


import java.util.ArrayList;

public class PubPayload {
    private ArrayList<PubData> messages = new ArrayList<PubData>();

    public PubPayload(PubData message) {
        this.messages.add(message);
    }
}
