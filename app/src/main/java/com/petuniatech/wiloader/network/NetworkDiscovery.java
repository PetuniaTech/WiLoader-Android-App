/*
 * Copyright (C) 2017 PetuniaTech LLC.
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/4.0/
 * or send a letter to Creative Commons, PO Box 1866, Mountain View, CA 94042, USA.
 */

package com.petuniatech.wiloader.network;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.petuniatech.wiloader.main.SettingTabFragment;
import com.petuniatech.wiloader.tools.LogController;
import com.petuniatech.wiloader.main.UserInterface;

/**
 * Created by PetuniaTech on 2017-06-24.
 */

public class NetworkDiscovery {

    public final static int WILOADER_UDP_PORT = 9833;
    public final static String WILOADER_MULTICAST_IP = "224.31.31.39";
    public final static String WILOADER_AP_IP = "192.168.97.1";

    private static final Logger LOGGER = new LogController().getLogger();

    static byte udpSeqNo = 0;
    static boolean enableReceive = false;
    static Timer timer = new Timer();
    static Timer progressTimer = new Timer();
    static ScheduledDiscovery rsd = new ScheduledDiscovery(); // repeated discovery task
    static ScheduledDiscovery ssd = new ScheduledDiscovery(); // single discovery task
    static HideScanProgressTask hideProgressTask = new HideScanProgressTask();

    static DatagramSocket clientSocket = null;
    static InetAddress[] broadcastAddresses = new InetAddress[1000];
    static int broadcastAddressesCount = 0;

    static boolean refreshNetworkInterfaces = false;
    static boolean multicastEnabled = false;
    static boolean broadcastEnabled = true;
    static boolean initialized = false;

    static String specificIP = "";
    public static int scanInterval = 0;
    static Thread receiveThread;

    public static UserInterface ui = null;



    public static void setUI(UserInterface userInt) {
        ui = userInt;
    }

    public static class ScheduledDiscovery extends TimerTask {

        ScheduledDiscovery() {
            LOGGER.log(Level.FINE, "ScheduledDiscovery: constructor");
            if (clientSocket != null) {
                LOGGER.log(Level.FINE, "ScheduledDiscovery: client socket isn't null");
                return;
            }
            try {
                clientSocket = new DatagramSocket();
                clientSocket.setBroadcast(true);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.toString(), ex);
            }
        }

        // Add your task here
        @Override
        public synchronized void run() {

            DatagramPacket sendPacket;
            byte[] sendData;
            Enumeration<NetworkInterface> interfaces;
            InetAddress broadcast;
            int IPAddrCount =0;

            LOGGER.log(Level.FINE, "ScheduledDiscovery run: refreshNetInt: {0} specIP: {1} multicast: {2} broadcast: {3}", new Object[]{refreshNetworkInterfaces, specificIP, multicastEnabled, broadcastEnabled});

            try {

                ui.refreshWiLoaders();

                hideProgressTask.cancel(); // cancel previous task
                hideProgressTask = new HideScanProgressTask();
                progressTimer.schedule(hideProgressTask, 2000); // show progress for 3 seconds if no response received
                ui.showScanProgress(true);

                String sentence = "< >" + "<Are You a WiLoader?>"; // <udpReceivPort><seqNo><Are You a WiLoader?>
                sendData = sentence.getBytes();

                sendData[1] = udpSeqNo++;

                LOGGER.log(Level.FINEST, "UDP send data bytes: ", sendData);

                refreshNetworkInterfaces = true; // bypass check (always refresh interfaces)
                // refresh network interfaces list / get broadcast addresses
                if (refreshNetworkInterfaces) {

                    refreshNetworkInterfaces = false;
                    broadcastAddressesCount = 0;


                    LOGGER.log(Level.INFO, "Refreshing Net Interfaces ...");

                    interfaces = NetworkInterface.getNetworkInterfaces();

                    for (NetworkInterface netint : Collections.list(interfaces)) {

                        LOGGER.log(Level.FINEST, "net Interface: ", netint.getName());

                        if (netint.isLoopback() || !netint.isUp()) {
                            LOGGER.log(Level.FINEST, "net Interface is down/loopback: ", netint.getName());
                            continue;
                        }

                        for (InterfaceAddress interfaceAddress : netint.getInterfaceAddresses()) {
                            broadcast = interfaceAddress.getBroadcast();
                            LOGGER.log(Level.FINEST, "net Interface: {0} interface Add: {1} broadcast: {2}", new Object[]{netint.getName(), interfaceAddress, broadcast});

                            if(SettingTabFragment.isValidIP(interfaceAddress.getAddress().getHostAddress())){
                                IPAddrCount ++;
                            }

                            if (broadcast == null) {
                                continue;
                            }

                            if (broadcastAddressesCount < broadcastAddresses.length) {
                                broadcastAddresses[broadcastAddressesCount] = broadcast;
                                broadcastAddressesCount++;
                            }
                        }

                    }
                    LOGGER.log(Level.INFO, "Refreshing Net Interfaces done.");
                }// end of if


                // send to multicast address
                if (multicastEnabled && IPAddrCount >0) {
                    LOGGER.log(Level.FINEST, "send to multicast address: {0} port: {1}", new Object[]{WILOADER_MULTICAST_IP, WILOADER_UDP_PORT});
                    sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(WILOADER_MULTICAST_IP), WILOADER_UDP_PORT);
                    clientSocket.send(sendPacket);
                }

                // send to specific address
                if (!"".equals(specificIP) && IPAddrCount >0) {
                    LOGGER.log(Level.FINEST, "send to specific address: {0} port: {1}", new Object[]{specificIP, WILOADER_UDP_PORT});
                    sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(specificIP), WILOADER_UDP_PORT);
                    clientSocket.send(sendPacket);

                }

                if (broadcastEnabled == false) {
                    return;
                }

                // send to broadcast addresses
                for (int i = 0; i < broadcastAddressesCount; i++) {
                    LOGGER.log(Level.FINEST, "send to broadcast address #{0}: {1} port: {2}", new Object[]{i, broadcastAddresses[i], WILOADER_UDP_PORT});
                    sendPacket = new DatagramPacket(sendData, sendData.length, broadcastAddresses[i], WILOADER_UDP_PORT);
                    clientSocket.send(sendPacket);
                }

            } catch (BindException ex) {
                ui.displayAlert("Bind to IP Error", "Could not bind socket to the entered IP address.");
                initialized = false;
                LOGGER.log(Level.SEVERE, ex.toString(), ex);
            } catch (SocketException ex) {
                ui.displayAlert("UDP Transmission Error", "Could not transmit data through network socket.");
                initialized = false;
                LOGGER.log(Level.SEVERE, ex.toString(), ex);
            } catch (IOException ex) {
                ui.displayAlert("UDP Transmission Error", "Could not transmit data through network socket.");
                initialized = false;
                LOGGER.log(Level.SEVERE, ex.toString(), ex);
            }

        }
    }

    public static void initSocket(String bindIP, String specIP, boolean multicast, boolean broadcast) {

        LOGGER.log(Level.FINE, "init scoket: bindIP: {0} specIP: {1} multicast: {2} broadcast: {3}", new Object[]{bindIP, specIP, multicast, broadcast});

        specificIP = specIP;
        broadcastEnabled = broadcast;
        multicastEnabled = multicast;

        rsd.cancel();
        ssd.cancel();

        enableReceive = true;
        startReceive(bindIP);

        initialized = true;

    }

    public static void sendDiscovryPacket() {

        LOGGER.log(Level.FINE, "send discovery pkt: initialized: {0} receiveThreadIsAlive: {1}", new Object[]{initialized, receiveThread.isAlive()});

        if (!initialized || receiveThread.isAlive() == false) {
            initSocket("", "", false, true);
        }
        refreshNetworkInterfaces = true;
        ssd = new ScheduledDiscovery();
        timer.schedule(ssd, 10); // Create task for once

    }

    public static void sendRepititiveDiscovryPacket(int period) {

        LOGGER.log(Level.FINE, "send rpt discovery pkt: period: {0}", period);

        refreshNetworkInterfaces = true;
        rsd.cancel();
        scanInterval = period;

        if (period > 0) {

            rsd = new ScheduledDiscovery();
            timer.schedule(rsd, 200, period * 1000); // Create Repetitively task for every period secs
        }

    }

    static void startReceive(String bindIP) {

        final String fbindIP = bindIP;
        Runnable receiveTask = new Runnable() {
            @Override
            public void run() {
                try {

                    LOGGER.log(Level.FINE, "start receive thread: bindIP: {0}", fbindIP);

                    if (clientSocket != null) {
                        LOGGER.log(Level.FINE, "receive thread: client socket isn't null");
                        if (clientSocket.isClosed() == false) {
                            LOGGER.log(Level.FINE, "receive thread: closing client socket ...");
                            clientSocket.close();
                        }
                    }

                    if ("".equals(fbindIP)) {
                        clientSocket = new DatagramSocket();
                    } else {
                        clientSocket = new DatagramSocket(new InetSocketAddress(fbindIP, 0));
                    }
                    clientSocket.setBroadcast(true);

                    byte[] receiveData = new byte[1024];

                    DatagramPacket receivePacket;
                    while (enableReceive) {

                        receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        LOGGER.log(Level.FINE, "receive thread: calling blocking receive function ...");
                        clientSocket.receive(receivePacket);

                        byte[] response = Arrays.copyOf(receiveData, receivePacket.getLength());
                        LOGGER.log(Level.FINEST, "UDP packet received: ", response);

                        ui.updateWiLoaderList(receivePacket.getAddress(), receivePacket.getPort(), response);

                    }

                } catch (BindException ex) {
                    ui.displayAlert("Bind to IP Error", "Could not bind socket to the entered IP address.");
                    initialized = false;
                    LOGGER.log(Level.SEVERE, ex.toString(), ex);

                } catch (SocketException ex) {
                    LOGGER.log(Level.SEVERE, ex.toString(), ex);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, ex.toString(), ex);
                }
            }
        };

        receiveThread = new Thread(receiveTask);
        receiveThread.start();
    }


    public static ArrayList<String> getIPAddresses(){

        ArrayList<String> IPList = new ArrayList<>();
        IPList.add("select"); //  first entry in the drop down list

        try {
            String IP;

            for (NetworkInterface netint : Collections.list(NetworkInterface.getNetworkInterfaces())) {

                LOGGER.log(Level.FINEST, "net Interface: ", netint.getName());

                if (netint.isLoopback() || !netint.isUp()) {
                    LOGGER.log(Level.FINEST, "net Interface is down/loopback: ", netint.getName());
                    continue;
                }

                for (InterfaceAddress interfaceAddress : netint.getInterfaceAddresses()) {

                    LOGGER.log(Level.FINEST, "net Interface: {0} interface Add: {1} broadcast: {2}", new Object[]{netint.getName(), interfaceAddress, interfaceAddress.getBroadcast()});

                    IP = interfaceAddress.getAddress().getHostAddress();
                    if(SettingTabFragment.isValidIP(IP)){
                        IPList.add(IP);
                    }
                }

            }
        }catch(Exception ex){
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }

        return IPList;
    }

    static class HideScanProgressTask extends TimerTask {

        @Override
        public void run() {
            ui.showScanProgress(false);
        }
    }
}
