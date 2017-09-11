/*
 * Copyright (C) 2017 PetuniaTech LLC.
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/4.0/
 * or send a letter to Creative Commons, PO Box 1866, Mountain View, CA 94042, USA.
 */

package com.petuniatech.wiloader.network;

import com.petuniatech.wiloader.main.MainActivity;
import com.petuniatech.wiloader.tools.LogController;
import com.petuniatech.wiloader.main.StatusCode;
import com.petuniatech.wiloader.modules.WiLoaderModule;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by PetuniaTech on 2017-06-27.
 */

public class CommandTerminal {

    final static int COMMAND_PORT = 8080;

    private static final Logger LOGGER = new LogController().getLogger();

    static Socket clientSocket = new Socket();
    static InputStream inFromWiLoader;
    static DataOutputStream outToWiLoader;
    public static boolean renewSocket = false;
    static String boundedIP = "";

    static byte seqNo = 0;
    public static volatile boolean commandInProgress = false;
    private static final Object statusLock = new Object();



    public static  boolean setBusy() {

        LOGGER.log(Level.FINE, "outside synch block");
        synchronized (statusLock) {
            LOGGER.log(Level.FINE, "inside synch block commandInProgress: {0}", commandInProgress);
            if (commandInProgress) {
                return false;
            }

            commandInProgress = true;
            return true;
        }

    }

    public static void setIdle() {
        LOGGER.log(Level.FINE, "set Idle");
        commandInProgress = false;
    }

    public static void disconnect() {
        LOGGER.log(Level.FINE, "disconnect");
        try {
            clientSocket.close();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }
    }

    public static synchronized int sendCommand(WiLoaderModule WiLoader, CommandPacket command) {


        if (WiLoader.isValid == false) {
            return StatusCode.INVALID_WILOADER_ERROR;
        }

        String bindIP = MainActivity.IPtoBind;


        String connectedIP = "";
        if (clientSocket.getInetAddress() != null) {
            connectedIP = clientSocket.getInetAddress().getHostAddress();
        }



        if (!boundedIP.equalsIgnoreCase(bindIP) || renewSocket || clientSocket.isClosed() || connectedIP.equalsIgnoreCase(WiLoader.defaultConnectionIP) == false) {

            LOGGER.log(Level.FINEST, "send command: bindIP: {0} boundedIP: {1} WiLoaderIP: {2} renewSocket: {3} socketColsed: {4} conIP: {5}",
                    new Object[]{bindIP, boundedIP, WiLoader.defaultConnectionIP, renewSocket, clientSocket.isClosed(), connectedIP});

            try {
                clientSocket.close();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.toString(), ex);
            }

            clientSocket = new Socket();
            boundedIP = "";
            if (!"".equals(bindIP)) {
                try {
                    clientSocket.bind(new InetSocketAddress(bindIP, 0));
                    boundedIP = bindIP;
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, ex.toString(), ex);
                    boundedIP = "";
                    return StatusCode.IP_BIND_ERROR;
                }
            }
        }



        if (clientSocket.isConnected() == false) {
            try {
                LOGGER.log(Level.FINEST, "send command: connect");
                clientSocket.connect(new InetSocketAddress(WiLoader.defaultConnectionIP, COMMAND_PORT), 3000);
                outToWiLoader = new DataOutputStream(clientSocket.getOutputStream());
                inFromWiLoader = clientSocket.getInputStream();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.toString(), ex);
                renewSocket = true;
                return StatusCode.SOCKET_CONNECTION_ERROR;
            }

        }



        byte[] packet = command.preparePacket(++seqNo);
        LOGGER.log(Level.FINEST, "send command: packet: ", packet);
        try {
            clientSocket.setSoTimeout(command.receiveTimeOut);
            outToWiLoader.write(packet);

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
            renewSocket = true;
            return StatusCode.SOCKET_STREAM_WRITE_ERROR;
        }

        try {

            command.responseLength = inFromWiLoader.read(command.responseBytes);
            LOGGER.log(Level.FINEST, "received packet length: {0}", command.responseLength);
            if (command.responseLength <= 0) {
                renewSocket = true;
                return StatusCode.SOCKET_STREAM_READ_ERROR;
            }
            LOGGER.log(Level.FINEST, "received packet data: ", Arrays.copyOf(command.responseBytes, command.responseLength));
            if (command.responseLength <= 2) {
                return StatusCode.RECEIVED_PACKET_ERROR;
            }
            renewSocket = false;
            if (command.responseBytes[1] == seqNo) {
                return StatusCode.OK;
            } else {
                return StatusCode.RECEIVED_PACKET_ERROR;
            }
        } catch (SocketTimeoutException ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
            renewSocket = true;
            return StatusCode.READ_TIME_OUT;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
            renewSocket = true;
            return StatusCode.SOCKET_STREAM_READ_ERROR;
        }

    }

    public static int handleCommand(CommandPacket commandPacket, WiLoaderModule WiLoader) {

        LOGGER.log(Level.FINE, "handle command: command: {0} WiLoader Name: {1}", new Object[]{Integer.toHexString((int) commandPacket.command & 0xff), WiLoader});

        int result = sendCommand(WiLoader, commandPacket);
        if (result == StatusCode.INVALID_WILOADER_ERROR || result == StatusCode.IP_BIND_ERROR) {
            LOGGER.log(Level.FINEST, "handle command: result_0: {0}", result);
            return result;
        } else if (result != StatusCode.OK) { // retry
            renewSocket = true;
            result = sendCommand(WiLoader, commandPacket);
        }

        LOGGER.log(Level.FINEST, "handle command: result_1: {0}", result);
        return result;
    }
}
