/*
 * Copyright (C) 2017 PetuniaTech LLC.
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/4.0/
 * or send a letter to Creative Commons, PO Box 1866, Mountain View, CA 94042, USA.
 */

package com.petuniatech.wiloader.network;

import com.petuniatech.wiloader.modules.WiLoaderModule;

import java.util.Arrays;

/**
 * Created by PetuniaTech on 2017-06-27.
 */

public class CommandPacket {

    public String mcuID, WiLoaderName, APPass, SSID, password;
    public byte fuseHighByte, fuseLowByte, fuseExtByte, lockBits;
    public byte[] signature = new byte[3];
    public byte[] staticIP;
    public byte[] subnetMask;
    public byte[] gateway;
    public byte[] WiLoaderNameBytes;
    public byte[] UARTBaudBytes = new byte[3];
    public byte[] APPassBytes;
    public byte[] SSIDBytes;
    public byte[] SSIDPassBytes;
    public byte[] BSSIDBytes = new byte[6];

    public byte BSSIDEnable = 0;

    public byte[] responseBytes = new byte[4096];
    public int responseLength = 0;

    public byte[] ISPCommandParams;

    public int UARTBaud, receiveTimeOut = 2000;
    public boolean updateWiLoaderName, updateAPPass, updateUARTBaud, updateIP, DHCP;

    public byte APChannel;
    public WiLoaderModule WiLoader;
    public boolean keepLevelTranslatorsActiveAfterReset = false;
    public boolean mainResetButton = true;
    public byte command = 0;

    // WiLoader setting commands
    public final static byte SETTING_MESSAGE_START = 0x69;

    public final static byte SAVE_SETTING_CMD = 0x01;
    public final static byte LOAD_SETTING_CMD = 0x02;
    public final static byte RESET_WILOADER_CMD = 0x03;
    public final static byte CONNECT_CMD = 0x04;
    public final static byte SCAN_CMD = 0x05;
    public final static byte RESET_TARGET_CMD = 0x06;
    public final static byte UPDATE_UART_BAUD_TEMP = 0x07;
    public final static byte GET_CONNECTION_STATE = 0x08;
    public final static byte CHANGE_UART_BAUD_AND_SAVE = 0x09;

    // AVR ISP commands
    public final static byte AVRISP_MESSAGE_START = 0x79;

    public final static byte AVRISP_COMMANDS_START = 0x40;


    public byte[] preparePacket(byte seqNo) {

        byte[] packet = {0};

        if (command == SAVE_SETTING_CMD) {
            packet = new byte[132];
            Arrays.fill(packet, (byte) 0);


            packet[0] = SETTING_MESSAGE_START;
            packet[1] = seqNo;
            packet[2] = (byte) packet.length;
            packet[3] = SAVE_SETTING_CMD;
            if (updateWiLoaderName) {
                packet[4] = 1;
            } else {
                packet[4] = 0;
            }

            if (updateAPPass) {
                packet[5] = 1;
            } else {
                packet[5] = 0;
            }

            if (updateUARTBaud) {
                packet[6] = 1;
            } else {
                packet[6] = 0;
            }

            if (updateIP) {
                packet[7] = 1;
            } else {
                packet[7] = 0;
            }

            if (DHCP) {
                packet[8] = 1;
            } else {
                packet[8] = 0;
            }

            if (updateWiLoaderName) {
                for (int i = 9; i < 9 + WiLoaderNameBytes.length; i++) {
                    packet[i] = WiLoaderNameBytes[i - 9];
                }
            }

            if (updateAPPass) {
                for (int i = 42; i < 42 + APPassBytes.length; i++) {
                    packet[i] = APPassBytes[i - 42];
                }
            }

            if (updateUARTBaud) {
                packet[110] = (byte) (UARTBaud >>> 16);
                packet[111] = (byte) (UARTBaud >>> 8);
                packet[112] = (byte) UARTBaud;
            }

            if (updateIP && DHCP == false) {
                packet[113] = staticIP[0];
                packet[114] = staticIP[1];
                packet[115] = staticIP[2];
                packet[116] = staticIP[3];

                packet[117] = subnetMask[0];
                packet[118] = subnetMask[1];
                packet[119] = subnetMask[2];
                packet[120] = subnetMask[3];

                packet[121] = gateway[0];
                packet[122] = gateway[1];
                packet[123] = gateway[2];
                packet[124] = gateway[3];

            }
        } else if (command == LOAD_SETTING_CMD) {
            packet = new byte[4];
            Arrays.fill(packet, (byte) 0);


            packet[0] = SETTING_MESSAGE_START;
            packet[1] = seqNo;
            packet[2] = (byte) packet.length;
            packet[3] = LOAD_SETTING_CMD;

        } else if (command == RESET_WILOADER_CMD) {
            packet = new byte[4];
            Arrays.fill(packet, (byte) 0);


            packet[0] = SETTING_MESSAGE_START;
            packet[1] = seqNo;
            packet[2] = (byte) packet.length;
            packet[3] = RESET_WILOADER_CMD;

        } else if (command == CONNECT_CMD) {
            packet = new byte[107];
            Arrays.fill(packet, (byte) 0);


            packet[0] = SETTING_MESSAGE_START;
            packet[1] = seqNo;
            packet[2] = (byte) packet.length;
            packet[3] = CONNECT_CMD;

            for (int i = 4; i < 4 + SSIDBytes.length; i++) {
                packet[i] = SSIDBytes[i - 4];
            }

            for (int i = 36; i < 36 + SSIDPassBytes.length; i++) {
                packet[i] = SSIDPassBytes[i - 36];
            }

            packet[100] = BSSIDEnable;

            for (int i = 101; i < 101 + 6; i++) {
                packet[i] = BSSIDBytes[i - 101];
            }



        } else if (command == SCAN_CMD) {
            packet = new byte[4];
            Arrays.fill(packet, (byte) 0);


            packet[0] = SETTING_MESSAGE_START;
            packet[1] = seqNo;
            packet[2] = (byte) packet.length;
            packet[3] = SCAN_CMD;

        } else if (command == RESET_TARGET_CMD) {
            packet = new byte[5];
            Arrays.fill(packet, (byte) 0);


            packet[0] = SETTING_MESSAGE_START;
            packet[1] = seqNo;
            packet[2] = (byte) packet.length;
            packet[3] = RESET_TARGET_CMD;
            if(keepLevelTranslatorsActiveAfterReset){
                packet[4] = 1;
            }else{
                packet[4] = 0;
            }

        } else if (command == UPDATE_UART_BAUD_TEMP) {
            packet = new byte[7];
            Arrays.fill(packet, (byte) 0);


            packet[0] = SETTING_MESSAGE_START;
            packet[1] = seqNo;
            packet[2] = (byte) packet.length;
            packet[3] = UPDATE_UART_BAUD_TEMP;

            packet[4] = (byte) (UARTBaud >>> 16);
            packet[5] = (byte) (UARTBaud >>> 8);
            packet[6] = (byte) UARTBaud;

        }else if (command == GET_CONNECTION_STATE) {
            packet = new byte[4];
            Arrays.fill(packet, (byte) 0);


            packet[0] = SETTING_MESSAGE_START;
            packet[1] = seqNo;
            packet[2] = (byte) packet.length;
            packet[3] = GET_CONNECTION_STATE;

        }else if (command == CHANGE_UART_BAUD_AND_SAVE) {
            packet = new byte[7];
            Arrays.fill(packet, (byte) 0);


            packet[0] = SETTING_MESSAGE_START;
            packet[1] = seqNo;
            packet[2] = (byte) packet.length;
            packet[3] = CHANGE_UART_BAUD_AND_SAVE;

            packet[4] = (byte) (UARTBaud >>> 16);
            packet[5] = (byte) (UARTBaud >>> 8);
            packet[6] = (byte) UARTBaud;

        }else if(command > AVRISP_COMMANDS_START){

            packet = new byte[ISPCommandParams.length +5];
            Arrays.fill(packet, (byte) 0);


            packet[0] = AVRISP_MESSAGE_START;
            packet[1] = seqNo;
            packet[2] = (byte) (packet.length / 256);
            packet[3] = (byte) (packet.length % 256);
            packet[4] = command;

            // copy params
            System.arraycopy(ISPCommandParams, 0, packet, 5, ISPCommandParams.length);
        }

        return packet;
    }
}
