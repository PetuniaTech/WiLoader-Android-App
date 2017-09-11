/*
 * Copyright (C) 2017 PetuniaTech LLC.
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/4.0/
 * or send a letter to Creative Commons, PO Box 1866, Mountain View, CA 94042, USA.
 */

package com.petuniatech.wiloader.modules;

import android.os.Parcel;
import android.os.Parcelable;

import com.petuniatech.wiloader.network.NetworkDiscovery;
import com.petuniatech.wiloader.tools.LogController;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by PetuniaTech on 2017-06-24.
 */

public class WiLoaderModule implements Parcelable {


    private static final Logger LOGGER = new LogController().getLogger();

    public String name, stationIP, softAPIP, firmwareVersion, defaultConnectionIP,
            stationMAC, connectedSSID, rssiText, targetVoltageText, mode, connectionStatus;
    public byte rssiByte;
    public double targetVoltage;
    public int seqNumber;
    public boolean isValid;
    public int noResponseCount =0;

    public final static int PACKET_LENGTH = 94;

    public WiLoaderModule() {
        isValid = false;
    }

    public WiLoaderModule(InetAddress address, int remotePort, byte[] response) {

        isValid = true;

        if (response.length != PACKET_LENGTH) {
            isValid = false;
            return;
        }
        if (((int) response[32] & 0xff) != PACKET_LENGTH) {
            isValid = false;
            return;
        }

        // packet sequence number
        seqNumber = (int) response[33] & 0xff;

        // WiLoader name
        int nameLength = 0;
        for (int i = 31; i >= 0; i--) {
            if (response[i] != 0) {
                nameLength = i + 1;
                break;
            }
        }

        if (nameLength == 0) {
            isValid = false;
            return;
        }

        byte[] wiName = new byte[nameLength];
        System.arraycopy(response, 0, wiName, 0, nameLength);

        name = "";
        try {
            name = new String(wiName, "UTF-8");// + String.valueOf((int) response[42] & 0xff);
        } catch (UnsupportedEncodingException ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
            isValid = false;
            return;
        }

        // RSSI
        rssiByte = response[34];
        if (rssiByte == 31) // invalid value
        {
            rssiText = "";
        } else {
            rssiText = String.valueOf(rssiByte);
        }

        // Target Voltage
        int tVoltADC = ((int) response[35] & 0xff) * 256 + ((int) response[36] & 0xff);
        targetVoltage = (double) (0.0074870 * tVoltADC);
        DecimalFormat df2 = new DecimalFormat(".00");
        targetVoltageText = df2.format(targetVoltage);

        // Firmware Version
        int fwvh = (int) response[37] & 0xff;
        int fwvl = (int) response[38] & 0xff;
        firmwareVersion = String.valueOf(fwvh) + "." + String.valueOf(fwvl);

        // Station IP
        int ipByte = (int) response[39] & 0xff;
        stationIP = String.valueOf(ipByte);
        ipByte = (int) response[40] & 0xff;
        stationIP += "." + String.valueOf(ipByte);
        ipByte = (int) response[41] & 0xff;
        stationIP += "." + String.valueOf(ipByte);
        ipByte = (int) response[42] & 0xff;
        stationIP += "." + String.valueOf(ipByte);

        // station MAC
        int macByte = (int) response[43] & 0xff;
        stationMAC = String.format("%02x", macByte);
        macByte = (int) response[44] & 0xff;
        stationMAC += ":" + String.format("%02x", macByte);
        macByte = (int) response[45] & 0xff;
        stationMAC += ":" + String.format("%02x", macByte);
        macByte = (int) response[46] & 0xff;
        stationMAC += ":" + String.format("%02x", macByte);
        macByte = (int) response[47] & 0xff;
        stationMAC += ":" + String.format("%02x", macByte);
        macByte = (int) response[48] & 0xff;
        stationMAC += ":" + String.format("%02x", macByte);

        // softAP IP
        ipByte = (int) response[49] & 0xff;
        softAPIP = String.valueOf(ipByte);
        ipByte = (int) response[50] & 0xff;
        softAPIP += "." + String.valueOf(ipByte);
        ipByte = (int) response[51] & 0xff;
        softAPIP += "." + String.valueOf(ipByte);
        ipByte = (int) response[52] & 0xff;
        softAPIP += "." + String.valueOf(ipByte);

        // WiFi mode
        switch (response[53]) {
            case 1:
                mode = "station";
                break;
            case 2:
                mode = "AP";
                break;
            case 3:
                mode = "station+AP";
                break;
            default:
                mode = "unknown";
                break;
        }

        // We don't use remote IP for WiLoader TCP connection as we may be behind a NAT (virtual box , ...).
        String remoteIPAddress;
        if(address == null){
            remoteIPAddress = "";
        }else{
            remoteIPAddress = address.getHostAddress();
        }


        if (mode.equalsIgnoreCase("station")) {
            defaultConnectionIP = stationIP;
        } else if (mode.equalsIgnoreCase("station+AP")) {
            if (stationIP.equalsIgnoreCase("0.0.0.0")) {
                defaultConnectionIP = softAPIP;
            } else if(remoteIPAddress.equalsIgnoreCase(softAPIP) && remotePort == NetworkDiscovery.WILOADER_UDP_PORT){
                defaultConnectionIP = softAPIP;
            }else{
                defaultConnectionIP = stationIP;
            }
        } else {
            defaultConnectionIP = "unknown";
        }

        // Connection Status
        switch (response[54]) {
            case 0:
                connectionStatus = "idle";
                break;
            case 1:
                connectionStatus = "connecting";
                break;
            case 2:
                connectionStatus = "wrong password";
                break;
            case 3:
                connectionStatus = "no AP found";
                break;
            case 4:
                connectionStatus = "connect failed";
                break;
            case 5:
                connectionStatus = "got IP";
                break;
            default:
                connectionStatus = "unknown";
                break;
        }

        // Connected network SSID
        nameLength = 0;
        for (int i = 86; i >= 55; i--) {
            if (response[i] != 0) {
                nameLength = i - 55 + 1;
                break;
            }
        }


        if (nameLength == 0) {
            if (response[87] == 0) { // BSSID set -> use MAC address as SSID
                connectedSSID = "";
            } else {
                macByte = (int) response[88] & 0xff;
                connectedSSID = String.format("%02x", macByte);
                macByte = (int) response[89] & 0xff;
                connectedSSID += ":" + String.format("%02x", macByte);
                macByte = (int) response[90] & 0xff;
                connectedSSID += ":" + String.format("%02x", macByte);
                macByte = (int) response[91] & 0xff;
                connectedSSID += ":" + String.format("%02x", macByte);
                macByte = (int) response[92] & 0xff;
                connectedSSID += ":" + String.format("%02x", macByte);
                macByte = (int) response[93] & 0xff;
                connectedSSID += ":" + String.format("%02x", macByte);
            }

        } else {
            byte[] SSIDName = new byte[nameLength];
            for (int i = 0; i < nameLength; i++) {
                SSIDName[i] = response[i + 55];
            }

            connectedSSID = "";
            try {
                connectedSSID = new String(SSIDName, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                LOGGER.log(Level.SEVERE, ex.toString(), ex);
                connectedSSID = "???";
            }

        }

        LOGGER.log(Level.FINEST, "WiLoader Module: seqNo: {0} name: {1} rssi: {2} voltage: {3} FW: {4} station IP: {5} station MAC: {6} softAPIP: {7} mode: {8} defaultConnectionIP: {9} status: {10} SSID: {11}",
                new Object[]{seqNumber, name, rssiText, targetVoltageText, firmwareVersion, stationIP, stationMAC, softAPIP, mode, defaultConnectionIP, connectionStatus, connectedSSID});
    }

    public WiLoaderModule(WiLoaderModule newModule) {
        isValid = newModule.isValid;
        seqNumber = newModule.seqNumber;
        name = newModule.name;
        rssiByte = newModule.rssiByte;
        rssiText = newModule.rssiText;
        targetVoltage = newModule.targetVoltage;
        targetVoltageText = newModule.targetVoltageText;
        firmwareVersion = newModule.firmwareVersion;
        stationIP = newModule.stationIP;
        stationMAC = newModule.stationMAC;
        softAPIP = newModule.softAPIP;
        defaultConnectionIP = newModule.defaultConnectionIP;
        mode = newModule.mode;
        connectionStatus = newModule.connectionStatus;
        connectedSSID = newModule.connectedSSID;
    }

    public void update(WiLoaderModule newModule) {
        isValid = newModule.isValid;
        seqNumber = newModule.seqNumber;
        name = newModule.name;
        rssiByte = newModule.rssiByte;
        rssiText = newModule.rssiText;
        targetVoltage = newModule.targetVoltage;
        targetVoltageText = newModule.targetVoltageText;
        firmwareVersion = newModule.firmwareVersion;
        stationIP = newModule.stationIP;
        stationMAC = newModule.stationMAC;
        softAPIP = newModule.softAPIP;
        defaultConnectionIP = newModule.defaultConnectionIP;
        mode = newModule.mode;
        connectionStatus = newModule.connectionStatus;
        connectedSSID = newModule.connectedSSID;
    }

    public boolean isEqualTo(WiLoaderModule wiModule) {
        if (isValid == false || wiModule.isValid == false) {
            return false;
        }

        return stationMAC.equalsIgnoreCase(wiModule.stationMAC);


    }



    @Override
    public String toString() {
        return String.valueOf(name);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.stationIP);
        dest.writeString(this.softAPIP);
        dest.writeString(this.firmwareVersion);
        dest.writeString(this.defaultConnectionIP);
        dest.writeString(this.stationMAC);
        dest.writeString(this.connectedSSID);
        dest.writeString(this.rssiText);
        dest.writeString(this.targetVoltageText);
        dest.writeString(this.mode);
        dest.writeString(this.connectionStatus);
        dest.writeByte(this.rssiByte);
        dest.writeDouble(this.targetVoltage);
        dest.writeInt(this.seqNumber);
        dest.writeByte(this.isValid ? (byte) 1 : (byte) 0);
        dest.writeInt(this.noResponseCount);
    }

    protected WiLoaderModule(Parcel in) {
        this.name = in.readString();
        this.stationIP = in.readString();
        this.softAPIP = in.readString();
        this.firmwareVersion = in.readString();
        this.defaultConnectionIP = in.readString();
        this.stationMAC = in.readString();
        this.connectedSSID = in.readString();
        this.rssiText = in.readString();
        this.targetVoltageText = in.readString();
        this.mode = in.readString();
        this.connectionStatus = in.readString();
        this.rssiByte = in.readByte();
        this.targetVoltage = in.readDouble();
        this.seqNumber = in.readInt();
        this.isValid = in.readByte() != 0;
        this.noResponseCount = in.readInt();
    }

    public static final Parcelable.Creator<WiLoaderModule> CREATOR = new Parcelable.Creator<WiLoaderModule>() {
        @Override
        public WiLoaderModule createFromParcel(Parcel source) {
            return new WiLoaderModule(source);
        }

        @Override
        public WiLoaderModule[] newArray(int size) {
            return new WiLoaderModule[size];
        }
    };
}
