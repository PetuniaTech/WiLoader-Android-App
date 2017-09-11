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
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by PetuniaTech on 2017-06-27.
 */

public class SerialTerminal {

    final static int SERIAL_TERMINAL_PORT = 80;
    final static int ACK_PORT = 8012;

    public final static int MAX_LENGTH = 1400;
    public final static int UART_MAX_BAUD = 4608000;

    private static final Logger LOGGER = new LogController().getLogger();
    Socket clientSocket = new Socket();
    Socket ackSocket = new Socket();
    boolean enableReceive = true;
    boolean renewSocket = false;
    String boundIP = "";
    Thread receiveThread, ackReceiveThread;
    DataOutputStream outToWiLoader;
    byte[] readBuffer = new byte[4096];
    byte[] ackReadBuffer = new byte[2048];
    int responseLength;
    CountDownLatch latch = new CountDownLatch(1);
    public long time;
    public WiLoaderModule connectedWiLoader = new WiLoaderModule();

    SerialInterface source;

    public volatile boolean commandInProgress = false;
    private final Object statusLock = new Object();

    public boolean setBusy() {

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

    public void setIdle() {
        LOGGER.log(Level.FINE, "set Idle");
        commandInProgress = false;
    }

    public void setSource(SerialInterface src) {
        LOGGER.log(Level.FINE, "set source: {0}", src);
        source = src;
    }

    public synchronized int sendData(byte[] data) {

        LOGGER.log(Level.FINE, "send data: receiveThreadIsAlive: {0} ackReceiveThreadIsAlive: {1}", new Object[]{receiveThread.isAlive(),ackReceiveThread.isAlive()});

        if (receiveThread.isAlive() == false || ackReceiveThread.isAlive() == false) {
            return StatusCode.RECEIVE_THREAD_ERROR;
        }

        latch = new CountDownLatch(1);
        try {
            LOGGER.log(Level.FINEST, "send data bytes: ", data);
            time = System.currentTimeMillis();
            outToWiLoader.write(data);

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
            return StatusCode.SOCKET_STREAM_WRITE_ERROR;
        }


        // wait for Ack
        boolean dataReceived;
        try {
            dataReceived = latch.await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
            dataReceived = false;
        }

        if (dataReceived) {
            if (ackReadBuffer.length != 5) {
                return StatusCode.SERIAL_TERMINAL_INVALID_ACK;
            } else {
                int length = (int) (ackReadBuffer[1] & 0xff) * 256 + (int) (ackReadBuffer[2] & 0xff);
                if (length != data.length) {
                    return StatusCode.SERIAL_TERMINAL_INVALID_ACK;
                }

                if (ackReadBuffer[3] != data[0] || ackReadBuffer[4] != data[data.length - 1]) {
                    return StatusCode.SERIAL_TERMINAL_INVALID_ACK;
                }

                switch (ackReadBuffer[0]) {
                    case 0:
                        return StatusCode.OK;
                    case 1:
                        return StatusCode.SERIAL_TERMINAL_BUFFER_OVERFLOW;
                    case 2:
                        return StatusCode.SERIAL_TERMINAL_IN_USE_BY_BOOTLOADER;
                    case 3:
                        return StatusCode.SERIAL_TERMINAL_UART_DISABLED;
                    case 4:
                        return StatusCode.SERIAL_TERMINAL_DATA_LENGTH_ERROR;
                    default:
                        return StatusCode.SERIAL_TERMINAL_INVALID_ACK;

                }

            }

        } else {
            return StatusCode.SERIAL_TERMINAL_ACK_TIMEOUT;
        }

    }

    public synchronized int connect(WiLoaderModule WiLoader) {

        LOGGER.log(Level.FINE, "connect: WiLoaderIsValid: {0} WiLoaderName: {1}", new Object[]{WiLoader.isValid, WiLoader});

        connectedWiLoader.update(WiLoader);

        if (WiLoader.isValid == false) {
            return StatusCode.INVALID_WILOADER_ERROR;
        }

        try {
            clientSocket.close();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }

        try {
            ackSocket.close();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }

        clientSocket = new Socket();
        ackSocket = new Socket();

        String bindIP = MainActivity.IPtoBind;


        LOGGER.log(Level.FINE, "connect: bindIP: {0} WiLoaderIP: {1} port: {2}", new Object[]{bindIP, WiLoader.defaultConnectionIP, SERIAL_TERMINAL_PORT});

        if (!"".equals(bindIP)) {
            try {
                clientSocket.bind(new InetSocketAddress(bindIP, 0));

            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, ex.toString(), ex);
                return StatusCode.IP_BIND_ERROR;
            }

            try {
                ackSocket.bind(new InetSocketAddress(bindIP, 0));

            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, ex.toString(), ex);
                return StatusCode.IP_BIND_ERROR;
            }
        }

        try {
            clientSocket.connect(new InetSocketAddress(WiLoader.defaultConnectionIP, SERIAL_TERMINAL_PORT), 3000);
            outToWiLoader = new DataOutputStream(clientSocket.getOutputStream());

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
            return StatusCode.SOCKET_CONNECTION_ERROR;
        }

        DataOutputStream ackOutToWiLoader;
        try {
            ackSocket.connect(new InetSocketAddress(WiLoader.defaultConnectionIP, ACK_PORT), 3000);
            ackOutToWiLoader = new DataOutputStream(ackSocket.getOutputStream());

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
            disconnect();
            return StatusCode.SOCKET_CONNECTION_ERROR;
        }

        // send data socket local port number via ack socket to WiLoader
        int port = clientSocket.getLocalPort();
        if (port > 0) {
            byte[] portBytes = new byte[]{(byte) (port / 256), (byte) (port % 256)};
            try {
                LOGGER.log(Level.FINEST, "send port bytes: ", portBytes);
                ackOutToWiLoader.write(portBytes);

            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, ex.toString(), ex);
                disconnect();
                return StatusCode.SOCKET_STREAM_WRITE_ERROR;
            }

        }

        startReceiveData();
        startReceiveAck();

        return StatusCode.OK;
    }

    public void disconnect() {

        try {
            LOGGER.log(Level.FINE, "data disconnect");
            clientSocket.close();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }

        try {
            LOGGER.log(Level.FINE, "ack disconnect");
            ackSocket.close();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }

    }

    void startReceiveData() {

        Runnable receiveTask = new Runnable() {
            @Override
            public void run() {

                InputStream inFromWiLoader;
                try {
                    LOGGER.log(Level.FINE, "start receive data thread");
                    inFromWiLoader = clientSocket.getInputStream();
                    while (enableReceive) {

                        responseLength = inFromWiLoader.read(readBuffer);
                        LOGGER.log(Level.FINEST, "received data packet length: {0}", responseLength);

                        if (responseLength > 0) {
                            byte data[] = Arrays.copyOf(readBuffer, responseLength);
                            LOGGER.log(Level.FINEST, "received data packet data: ", data);
                            source.receiveSerialData(data);

                        } else {
                            return;
                        }

                    }

                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, ex.toString(), ex);
                }

            }
        };

        receiveThread = new Thread(receiveTask);
        receiveThread.start();
    }

    void startReceiveAck() {

        Runnable ackReceiveTask = new Runnable() {
            @Override
            public void run() {

                InputStream inFromWiLoader;
                try {
                    LOGGER.log(Level.FINE, "start receive ack thread");
                    inFromWiLoader = ackSocket.getInputStream();
                    int length;
                    byte[] buffer = new byte[2048];
                    while (enableReceive) {

                        length = inFromWiLoader.read(buffer);
                        LOGGER.log(Level.FINEST, "received ack packet length: {0}", length);

                        if (length > 0) {
                            byte data[] = Arrays.copyOf(buffer, length);
                            LOGGER.log(Level.FINEST, "received ack packet data: ", data);
                            if (latch.getCount() > 0) {
                                ackReadBuffer = data;
                                latch.countDown();
                            }
                        } else {
                            return;
                        }

                    }

                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, ex.toString(), ex);
                }

            }
        };

        ackReceiveThread = new Thread(ackReceiveTask);
        ackReceiveThread.start();
    }
}
