/*
 * Copyright (C) 2017 PetuniaTech LLC.
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/4.0/
 * or send a letter to Creative Commons, PO Box 1866, Mountain View, CA 94042, USA.
 */

package com.petuniatech.wiloader.modules;

import com.petuniatech.wiloader.tools.LogController;

import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by PetuniaTech on 2017-07-02.
 */

public class WiFiAP {

    private static final Logger LOGGER = new LogController().getLogger();

    public String SSID, BSSID;

    public byte channel, rssi, authMode;
    public int SSIDLength;
    public boolean hiddenSSID;
    public byte[] BSSIDBytes = new byte[6];
    public byte[] SSIDBytes;

    public int[] APStartIndex;

    public WiFiAP() {
        hiddenSSID = false;
        SSID = "";
        BSSID = "";
    }

    public WiFiAP(byte[] response, int APStartIndex) {

        channel = response[APStartIndex++];
        rssi = response[APStartIndex++];
        authMode = response[APStartIndex++];

        for (int i = 0; i < 6; i++) {
            BSSIDBytes[i] = response[APStartIndex++];
        }

        int macByte = (int) BSSIDBytes[0] & 0xff;
        BSSID = String.format("%02x", macByte);
        for (int i = 1; i < 6; i++) {
            macByte = (int) BSSIDBytes[i] & 0xff;
            BSSID += ":" + String.format("%02x", macByte);
        }

        hiddenSSID = response[APStartIndex++] == 1;

        if (hiddenSSID) {
            SSIDLength = 0;
            SSID = "";
            SSIDBytes = new byte[1];
            SSIDBytes[0] = 0;
            return;
        }

        SSIDLength = (int) response[APStartIndex] & 0xff;
        if (SSIDLength == 0) {
            SSID = "";
            SSIDBytes = new byte[1];
            SSIDBytes[0] = 0;
            return;
        }

        SSIDBytes = new byte[SSIDLength];
        for (int i = APStartIndex + 1; i < APStartIndex + 1 + SSIDLength; i++) {
            SSIDBytes[i - APStartIndex - 1] = response[i];
        }

        SSID = "";
        try {
            SSID = new String(SSIDBytes, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
            SSID = "???";
        }

    }

    public boolean checkResponse(byte[] response, int responseLength) {

        if (responseLength < 6) {
            return false;
        }

        int APCount = (int) response[5] & 0xff;

        if (APCount == 0) {
            APStartIndex = null;
            return true;
        }
        APStartIndex = new int[APCount];

        int APIndex = 0;
        APStartIndex[0] = 6;
        APCount--;
        APIndex++;

        try {
            while (APCount > 0) {
                APStartIndex[APIndex] = APStartIndex[APIndex - 1] + 11 + ((int) response[APStartIndex[APIndex - 1] + 10] & 0xff);

                APCount--;
                APIndex++;
            }
            // check last AP info length
            if (APStartIndex[APIndex - 1] + 11 + ((int) response[APStartIndex[APIndex - 1] + 10] & 0xff) != responseLength) {
                return false;
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
            return false;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
            return false;
        }

        return true;

    }

    @Override
    public String toString() {
        if (hiddenSSID) {
            return String.valueOf(BSSID) + "   " + String.valueOf(rssi) + " dBm  #" + String.valueOf(channel);
        } else {
            return String.valueOf(SSID) + "   " + String.valueOf(rssi) + " dBm  #" + String.valueOf(channel);
        }
    }

    public String toSSIDString() {
        if (hiddenSSID) {
            return String.valueOf(BSSID);
        } else {
            return String.valueOf(SSID);
        }
    }
}
