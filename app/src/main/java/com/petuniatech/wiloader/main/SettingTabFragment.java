/*
 * Copyright (C) 2017 PetuniaTech LLC.
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/4.0/
 * or send a letter to Creative Commons, PO Box 1866, Mountain View, CA 94042, USA.
 */

package com.petuniatech.wiloader.main;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.petuniatech.wiloader.R;
import com.petuniatech.wiloader.modules.WiFiAP;
import com.petuniatech.wiloader.modules.WiLoaderModule;
import com.petuniatech.wiloader.network.CommandPacket;
import com.petuniatech.wiloader.network.CommandTerminal;
import com.petuniatech.wiloader.network.NetworkDiscovery;
import com.petuniatech.wiloader.network.SerialTerminal;
import com.petuniatech.wiloader.tools.LogController;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by PetuniaTech on 2017-06-27.
 */

public class SettingTabFragment extends Fragment {

    private static final Logger LOGGER = new LogController().getLogger();
    static WiLoaderModule wiLoaderModule = new WiLoaderModule();


    List<WiFiAP> networkList = new ArrayList<WiFiAP>();
    ArrayAdapter<WiFiAP> networkAdapter;

    AlertDialog alertDialog;
    String alertMessage;

    TextView IPText, MACText, netText;
    EditText nameEditText, APPassEditText, staticIPEditText, subnetEditText, gatewayEditText, baudEditText;
    Spinner netSpinner;
    ProgressDialog progressdialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View rootView = inflater.inflate(R.layout.setting_layout, container, false);

        setRetainInstance(true);

        //Init Controls
        TextView wiLoaderNameLabel = (TextView) rootView.findViewById(R.id.nameText);
        wiLoaderNameLabel.setText(wiLoaderModule.toString());

        TextView firmwareVersionLabel = (TextView) rootView.findViewById(R.id.versionText);
        firmwareVersionLabel.setText("v" + wiLoaderModule.firmwareVersion);

        IPText = (TextView) rootView.findViewById(R.id.IPText);
        MACText = (TextView) rootView.findViewById(R.id.MACText);
        netText = (TextView) rootView.findViewById(R.id.NetText);

        final EditText SSIDEditText = (EditText) rootView.findViewById(R.id.SSIDEditText);
        final EditText passEditText = (EditText) rootView.findViewById(R.id.passEditText);
        nameEditText = (EditText) rootView.findViewById(R.id.nameEditText);
        APPassEditText = (EditText) rootView.findViewById(R.id.APPassEditText);
        staticIPEditText = (EditText) rootView.findViewById(R.id.staticIPEditText);
        subnetEditText = (EditText) rootView.findViewById(R.id.subnetEditText);
        gatewayEditText = (EditText) rootView.findViewById(R.id.gatewayEditText);
        baudEditText = (EditText) rootView.findViewById(R.id.baudEditText);


        updateLabels(wiLoaderModule);

        RadioGroup encodingRG = (RadioGroup) rootView.findViewById(R.id.encodingRG);
        encodingRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                if (checkedId == R.id.UTF8RB) {
                    TerminalTabFragment.messageFormat = TerminalTabFragment.MsgFormat.UTF8;
                } else if (checkedId == R.id.HexRB) {
                    TerminalTabFragment.messageFormat = TerminalTabFragment.MsgFormat.HEX;
                } else {
                    TerminalTabFragment.messageFormat = TerminalTabFragment.MsgFormat.DECIMAL;
                }

            }
        });

        CheckBox addCRCB = (CheckBox) rootView.findViewById(R.id.addCRcheckBox);
        CheckBox addLFCB = (CheckBox) rootView.findViewById(R.id.addLFcheckBox);
        CheckBox showPassCB = (CheckBox) rootView.findViewById(R.id.showPassCheckBox);

        showPassCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if(isChecked) {
                    passEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    APPassEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                } else {
                    passEditText.setInputType(129); //android:inputType="textPassword"
                    APPassEditText.setInputType(129);
                }
            }
        });

        addCRCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                TerminalTabFragment.appendCR = isChecked;
            }
        });

        addLFCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                TerminalTabFragment.appendLF = isChecked;
            }
        });


        netSpinner = (Spinner) rootView.findViewById(R.id.netSpinner);

        networkAdapter = new ArrayAdapter<WiFiAP>(rootView.getContext(), android.R.layout.simple_spinner_item, networkList);
        networkAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        netSpinner.setAdapter(networkAdapter);

        Button scanBtn = (Button) rootView.findViewById(R.id.scanBT);
        Button connectBtn = (Button) rootView.findViewById(R.id.connectBT);
        Button loadBtn = (Button) rootView.findViewById(R.id.loadBT);
        Button saveBtn = (Button) rootView.findViewById(R.id.saveBT);
        Button restartBtn = (Button) rootView.findViewById(R.id.restartBT);



        netSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                WiFiAP ap = (WiFiAP) parentView.getItemAtPosition(position);
                SSIDEditText.setText(ap.toSSIDString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }
        });

        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                LOGGER.log(Level.FINE, "scan wifi networks button");

                networkList.clear();
                networkAdapter.notifyDataSetChanged();
                SSIDEditText.setText("");

                CommandPacket commandPacket = new CommandPacket();
                commandPacket.receiveTimeOut = 8000;
                commandPacket.command = CommandPacket.SCAN_CMD;
                sendCommand(commandPacket, "Searching for WiFi Networks" );

            }
        });

        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                CommandPacket commandPacket = new CommandPacket();

                // SSID
                String ssid = SSIDEditText.getText().toString();

                if (ssid.isEmpty()) {
                    displayAlert("SSID Format Error", "Please check SSID field and try again.");
                    return;

                } else {
                    byte[] ssidBytes = null;
                    try {
                        ssidBytes = ssid.getBytes("UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        LOGGER.log(Level.SEVERE, ex.toString(), ex);
                        displayAlert("SSID Format Error", "Please check SSID field and try again.");
                        return;
                    }
                    if (ssidBytes == null) {
                        displayAlert("SSID Format Error", "Please check SSID field and try again.");
                        return;
                    }
                    if (ssidBytes.length > 32) {
                        displayAlert("SSID Length Error", "SSID should not be more than 32 bytes.");
                        return;
                    }

                    WiFiAP selectedAP = (WiFiAP) netSpinner.getSelectedItem();
                    if(selectedAP == null){
                        commandPacket.APChannel = 0; // unknown channel
                        commandPacket.BSSIDEnable = 0;
                        commandPacket.SSIDBytes = ssidBytes;

                    }else if(ssid.equalsIgnoreCase(selectedAP.toSSIDString())){
                        commandPacket.APChannel = selectedAP.channel;
                        if(selectedAP.hiddenSSID){
                            commandPacket.BSSIDEnable = 1;
                            commandPacket.BSSIDBytes = selectedAP.BSSIDBytes;
                            commandPacket.SSIDBytes = selectedAP.SSIDBytes;
                        }else{
                            commandPacket.BSSIDEnable = 0;
                            commandPacket.SSIDBytes = selectedAP.SSIDBytes;
                        }

                    }else{
                        commandPacket.APChannel = 0; // unknown channel
                        commandPacket.BSSIDEnable = 0;
                        commandPacket.SSIDBytes = ssidBytes;
                    }


                }
                // SSID Password
                String pass = passEditText.getText().toString();
                //check byte stream length
                if (pass.isEmpty()) {
                    displayAlert("Password Format Error", "Please check Password field and try again.");
                    return;
                } else {
                    byte[] passBytes = null;
                    try {
                        passBytes = pass.getBytes("UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        LOGGER.log(Level.SEVERE, ex.toString(), ex);
                        displayAlert("Password Format Error", "Please check Password field and try again.");
                        return;
                    }
                    if (passBytes == null) {
                        displayAlert("Password Format Error", "Please check Password field and try again.");
                        return;
                    }
                    if (passBytes.length < 8) {
                        displayAlert("Password Length Error", "Password should not be less than 8 bytes.");
                        return;
                    }
                    if (passBytes.length > 64) {
                        displayAlert("Password Length Error", "Password should not be more than 64 bytes.");
                        return;
                    }

                    commandPacket.SSIDPassBytes = passBytes;

                }

                commandPacket.receiveTimeOut = 2000;
                commandPacket.command = CommandPacket.CONNECT_CMD;
                sendCommand(commandPacket, "Connecting to " + ssid);
            }
        });

        loadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                LOGGER.log(Level.FINE, "load setting from WiLoader button");

                nameEditText.setText("");
                APPassEditText.setText("");
                baudEditText.setText("");
                staticIPEditText.setText("");
                subnetEditText.setText("");
                gatewayEditText.setText("");

                CommandPacket commandPacket = new CommandPacket();
                commandPacket.receiveTimeOut = 2000;
                commandPacket.command = CommandPacket.LOAD_SETTING_CMD;
                sendCommand(commandPacket, "Loading Settings from WiLoader");
            }
        });

        restartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                LOGGER.log(Level.FINE, "reset WiLoader button");

                CommandPacket commandPacket = new CommandPacket();
                commandPacket.receiveTimeOut = 2000;
                commandPacket.command = CommandPacket.RESET_WILOADER_CMD;
                sendCommand(commandPacket, "Restarting WiLoader");
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                LOGGER.log(Level.FINE, "save settings to WiLoader button: WiLoader Name: {0} AP Pass: {1} UART Baud: {2} static IP: {3} subnet: {4} gateway: {5}",
                        new Object[]{nameEditText.getText(), APPassEditText.getText(), baudEditText.getText(), staticIPEditText.getText(), subnetEditText.getText(), gatewayEditText.getText()});

                CommandPacket commandPacket = new CommandPacket();

                //check text fields
                // WiLoader Name
                String name = nameEditText.getText().toString();
                //check byte stream length
                if (name.isEmpty()) {
                    commandPacket.updateWiLoaderName = false;

                } else {
                    byte[] nameBytes = null;
                    try {
                        nameBytes = name.getBytes("UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        LOGGER.log(Level.SEVERE, ex.toString(), ex);
                        displayAlert("Name Format Error", "Please check WiLoader Name field and try again.");
                        return;
                    }
                    if (nameBytes == null) {
                        displayAlert("Name Format Error", "Please check WiLoader Name field and try again.");
                        return;
                    }
                    if (nameBytes.length > 32) {
                        displayAlert("Name Length Error", "WiLoader Name should not be more than 32 bytes.");
                        return;
                    }

                    commandPacket.WiLoaderNameBytes = nameBytes;
                    commandPacket.updateWiLoaderName = true;
                }

                // AP Password
                String pass = APPassEditText.getText().toString();
                //check byte stream length
                if (pass.isEmpty()) {
                    commandPacket.updateAPPass = false;

                } else {
                    byte[] passBytes = null;
                    try {
                        passBytes = pass.getBytes("UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        LOGGER.log(Level.SEVERE, ex.toString(), ex);
                        displayAlert("Password Format Error", "Please check AP Password field and try again.");
                        return;
                    }
                    if (passBytes == null) {
                        displayAlert("Password Format Error", "Please check AP Password field and try again.");
                        return;
                    }
                    if (passBytes.length < 8) {
                        displayAlert("Password Length Error", "AP password should not be less than 8 bytes.");
                        return;
                    }
                    if (passBytes.length > 64) {
                        displayAlert("Password Length Error", "AP password should not be more than 64 bytes.");
                        return;
                    }

                    commandPacket.APPassBytes = passBytes;
                    commandPacket.updateAPPass = true;
                }

                //UART Baud Rate
                String baudRate = baudEditText.getText().toString().replace(",", "");
                baudRate = baudRate.replaceAll("[^\\d.]", ""); // replace all non numeric characters
                baudRate = baudRate.replace(" ", "");
                if (baudRate.isEmpty()) {
                    commandPacket.updateUARTBaud = false;
                } else if (baudRate.equalsIgnoreCase("disabled") || baudRate.equalsIgnoreCase("disable")) {
                    commandPacket.UARTBaud = 0; // 0 means disable uart
                    commandPacket.updateUARTBaud = true;
                } else {
                    int baud;
                    try {
                        baud = Integer.parseInt(baudRate);
                    } catch (NumberFormatException ex) {
                        LOGGER.log(Level.SEVERE, ex.toString(), ex);
                        displayAlert("Number Format Error", "Please enter valid integer value in UART Baud textfield.");
                        return;
                    }
                    if (baud < 0) {
                        displayAlert("Number Range Error", "Please enter positive value in UART Baud textfield.");
                        return;
                    }
                    if (baud > SerialTerminal.UART_MAX_BAUD) {
                        displayAlert("Number Range Error", "Please enter value less than " + new DecimalFormat("#,###").format(SerialTerminal.UART_MAX_BAUD) + " bps in UART Baud textfield.");
                        return;
                    }
                    commandPacket.UARTBaud = baud;
                    commandPacket.updateUARTBaud = true;

                }

                //Static IP
                String staticIP = staticIPEditText.getText().toString();
                staticIP = staticIP.replace(" ", "");
                if (staticIP.isEmpty() || staticIP.equalsIgnoreCase("dhcpassigned")) {
                    commandPacket.updateIP = false;
                } else if (staticIP.equalsIgnoreCase("dhcp")) {
                    commandPacket.DHCP = true;
                    commandPacket.updateIP = true;
                } else {
                    if (isValidIP(staticIP) == false) {
                        displayAlert("IP Format Error", "Please enter valid IPv4 address in IP textfield.");
                        return;
                    }
                    String subnetMask = subnetEditText.getText().toString();
                    if (isValidIP(subnetMask) == false) {
                        displayAlert("IP Format Error", "Please enter valid IPv4 address in subnet mask textfield.");
                        return;
                    }
                    String gateway = gatewayEditText.getText().toString();
                    if (isValidIP(gateway) == false) {
                        displayAlert("IP Format Error", "Please enter valid IPv4 address in gateway textfield.");
                        return;
                    }

                    InetAddress ip;
                    try {
                        ip = InetAddress.getByName(staticIP);
                    } catch (UnknownHostException ex) {
                        LOGGER.log(Level.SEVERE, ex.toString(), ex);
                        displayAlert("IP Format Error", "Please enter valid IPv4 address in IP textfield.");
                        return;
                    }
                    commandPacket.staticIP = ip.getAddress();

                    try {
                        ip = InetAddress.getByName(subnetMask);
                    } catch (UnknownHostException ex) {
                        LOGGER.log(Level.SEVERE, ex.toString(), ex);
                        displayAlert("IP Format Error", "Please enter valid IPv4 address in subnet mask textfield.");
                        return;
                    }
                    commandPacket.subnetMask = ip.getAddress();

                    try {
                        ip = InetAddress.getByName(gateway);
                    } catch (UnknownHostException ex) {
                        LOGGER.log(Level.SEVERE, ex.toString(), ex);
                        displayAlert("IP Format Error", "Please enter valid IPv4 address in gateway textfield.");
                        return;
                    }
                    commandPacket.gateway = ip.getAddress();
                    commandPacket.updateIP = true;
                    commandPacket.DHCP = false;
                }// end of static IP

                if (commandPacket.updateWiLoaderName == false && commandPacket.updateAPPass == false
                        && commandPacket.updateUARTBaud == false && commandPacket.updateIP == false) {
                    displayAlert("No New Setting", "No new setting has been entered to save.");
                    return;
                }

                commandPacket.receiveTimeOut = 2000;
                commandPacket.command = CommandPacket.SAVE_SETTING_CMD;
                sendCommand(commandPacket, "Saving Settings to WiLoader");

            }
        });


        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wiLoaderModule = getArguments().getParcelable("WiLoader");
        if (wiLoaderModule == null) {
            wiLoaderModule = new WiLoaderModule();
            wiLoaderModule.name = "???";
            wiLoaderModule.firmwareVersion = "?.?";
        }

    }


    void updateLabels(WiLoaderModule wiloader) {

        MACText.setText(wiloader.stationMAC);

        if (wiloader.mode.equalsIgnoreCase("station")) {
            IPText.setText(wiloader.stationIP + " (ST)");
        } else if (wiloader.mode.equalsIgnoreCase("AP")) {
            IPText.setText(wiloader.softAPIP + " (AP)");
        } else if (wiloader.mode.equalsIgnoreCase("station+AP")) {
            if (wiloader.stationIP.equalsIgnoreCase("0.0.0.0")) {
                IPText.setText(wiloader.softAPIP + " (AP)");
            } else {
                IPText.setText(wiloader.stationIP + " (STAP)");
            }
        }

        if (wiloader.connectionStatus.equalsIgnoreCase("idle")) {
            netText.setText("not connected");
            netText.setTextColor(Color.RED);
        } else if (wiloader.connectionStatus.equalsIgnoreCase("connecting")) {
            netText.setText("connecting");
            netText.setTextColor(Color.BLUE);
        } else if (wiloader.connectionStatus.equalsIgnoreCase("wrong password")) {
            netText.setText("wrong password");
            netText.setTextColor(Color.RED);
        } else if (wiloader.connectionStatus.equalsIgnoreCase("no AP found")) {
            netText.setText("AP not found");
            netText.setTextColor(Color.RED);
        } else if (wiloader.connectionStatus.equalsIgnoreCase("connect failed")) {
            netText.setText("connect failed");
            netText.setTextColor(Color.RED);
        } else if (wiloader.connectionStatus.equalsIgnoreCase("got IP")) {
            netText.setText(wiloader.connectedSSID);
            netText.setTextColor(Color.argb(255,50,205,50)); // lime green
        } else {
            netText.setText("");
        }
    }


    void displayAlert(final String title, final String message) {

        getActivity().runOnUiThread((new Runnable() {
            public void run() {

                if (alertDialog != null) {
                    if (alertDialog.isShowing() && message.equalsIgnoreCase(alertMessage)) {
                        return;
                    }
                }

                alertMessage = message;
                alertDialog = new AlertDialog.Builder(getActivity()).create();
                alertDialog.setTitle(title);
                alertDialog.setMessage(message);
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        }));

    }


    public static boolean isValidIP(String ip) {
        try {

            if (ip == null || ip.isEmpty()) {
                return false;
            }

            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }

            for (String s : parts) {
                int i = Integer.parseInt(s);
                if ((i < 0) || (i > 255)) {
                    return false;
                }
            }
            if (ip.endsWith(".")) {
                return false;
            }

            return true;
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
            return false;
        }
    }

    void sendCommand(final CommandPacket commandPacket, final String progressMessage) {

        LOGGER.log(Level.FINE, "send command: WiLoader Name: {0} cmd: {1}", new Object[]{wiLoaderModule, Integer.toHexString((int) commandPacket.command & 0xff)});

        if (wiLoaderModule.isValid == false) {
            displayAlert("No WiLoader Selected", "Please select a WiLoader and try again.");
            return;
        }

        if (CommandTerminal.setBusy() == false) {
            displayAlert("Busy Doing Command", "Another command is in progress. Please try again later.");
            return;
        }

        final WiLoaderModule targetWiLoader = new WiLoaderModule(wiLoaderModule);
        progressdialog = new ProgressDialog(getActivity());

        Runnable handleCommand = new Runnable() {
            @Override
            public synchronized void run() {

                getActivity().runOnUiThread((new Runnable() {
                    public void run() {

                        progressdialog = new ProgressDialog(getActivity());
                        progressdialog.setMessage(progressMessage);
                        progressdialog.setCancelable(false);
                        progressdialog.show();
                    }
                }));


                int result = CommandTerminal.handleCommand(commandPacket, targetWiLoader);
                LOGGER.log(Level.FINE, "send command result: ", result);
                if (result == StatusCode.OK) {
                    processResponse(commandPacket);
                } else {
                    displayAlert("The operation failed", StatusCode.getErrorMessage(result));
                }

                getActivity().runOnUiThread((new Runnable() {
                    public void run() {

                        progressdialog.dismiss();
                    }
                }));

                CommandTerminal.setIdle();
            }
        };

        Thread commandThread = new Thread(handleCommand);
        commandThread.start();

    }

    void processResponse (final CommandPacket commandPacket){

        if(commandPacket.command == CommandPacket.SAVE_SETTING_CMD){

            displayAlert("Successful Operation", "To apply settings, please reset the WiLoader.");

        }else if (commandPacket.command == CommandPacket.RESET_WILOADER_CMD) {

            CommandTerminal.renewSocket = true;

        } else if (commandPacket.command == CommandPacket.LOAD_SETTING_CMD) {

            if (commandPacket.responseLength != 53) {
                displayAlert("Load Settings Result", "Invalid response received.");
                return;
            }

            // WiLoader name
            int nameLength = 0;
            for (int i = 36; i >= 5; i--) {
                if (commandPacket.responseBytes[i] != 0) {
                    nameLength = i + 1 - 5;
                    break;
                }
            }

            if (nameLength == 0) {
                return;
            }

            byte[] wiName = new byte[nameLength];
            for (int i = 0; i < nameLength; i++) {
                wiName[i] = (byte) commandPacket.responseBytes[i + 5];
            }

            String name = "";
            try {
                name = new String(wiName, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                LOGGER.log(Level.SEVERE, ex.toString(), ex);
                return;
            }

            // UART Baud Rate
            int baud = ((int) commandPacket.responseBytes[37] & 0xff) * 256 * 256 + ((int) commandPacket.responseBytes[38] & 0xff) * 256 + ((int) commandPacket.responseBytes[39] & 0xff);
            String baudString;
            if (baud == 0) {
                baudString = "Disabled";
            } else {
                DecimalFormat formatter = new DecimalFormat("#,###");
                baudString = formatter.format(baud);
            }

            // Static IP
            String staticIP;
            if (commandPacket.responseBytes[40] == 1) {
                if (commandPacket.responseBytes[41] == 0 && commandPacket.responseBytes[42] == 0
                        && commandPacket.responseBytes[43] == 0 && commandPacket.responseBytes[44] == 0) {
                    staticIP = "DHCP";
                } else {
                    staticIP = "DHCP assigned";
                }
            } else {
                staticIP = String.valueOf((int) commandPacket.responseBytes[41] & 0xff) + "."
                        + String.valueOf((int) commandPacket.responseBytes[42] & 0xff) + "."
                        + String.valueOf((int) commandPacket.responseBytes[43] & 0xff) + "."
                        + String.valueOf((int) commandPacket.responseBytes[44] & 0xff);
            }

            final String subnetMask = String.valueOf((int) commandPacket.responseBytes[45] & 0xff) + "."
                    + String.valueOf((int) commandPacket.responseBytes[46] & 0xff) + "."
                    + String.valueOf((int) commandPacket.responseBytes[47] & 0xff) + "."
                    + String.valueOf((int) commandPacket.responseBytes[48] & 0xff);

            final String gateway = String.valueOf((int) commandPacket.responseBytes[49] & 0xff) + "."
                    + String.valueOf((int) commandPacket.responseBytes[50] & 0xff) + "."
                    + String.valueOf((int) commandPacket.responseBytes[51] & 0xff) + "."
                    + String.valueOf((int) commandPacket.responseBytes[52] & 0xff);

            final String fname = name;
            final String fbaudString = baudString;
            final String fstaticIP = staticIP;
            getActivity().runOnUiThread((new Runnable() {
                public void run() {

                    // Update UI here.
                    LOGGER.log(Level.FINE, "process response: load settings: WiLoader Name: {0} UART Baud: {1} static ip: {2} subnet: {3} gateway: {4}",
                            new Object[]{fname, fbaudString, fstaticIP, subnetMask, gateway});

                    nameEditText.setText(fname);
                    baudEditText.setText(fbaudString);
                    staticIPEditText.setText(fstaticIP);
                    subnetEditText.setText(subnetMask);
                    gatewayEditText.setText(gateway);

                }
            }));

        }else if (commandPacket.command == CommandPacket.SCAN_CMD) {

            final WiFiAP ap = new WiFiAP();
            if (ap.checkResponse(commandPacket.responseBytes, commandPacket.responseLength)) {
                if (ap.APStartIndex != null) {
                    getActivity().runOnUiThread((new Runnable() {
                        public void run() {
                            // Update UI here.
                            WiFiAP samp;
                            byte maxRSSI = -120;
                            int closestAPIndex = 0;
                            for (int i = 0; i < ap.APStartIndex.length; i++) {
                                samp = new WiFiAP(commandPacket.responseBytes, ap.APStartIndex[i]);
                                LOGGER.log(Level.FINE, "new WiFi AP added: ", samp);
                                networkList.add(samp);
                                if (samp.rssi > maxRSSI) {
                                    maxRSSI = samp.rssi;
                                    closestAPIndex = i;
                                }
                            }

                            netSpinner.setSelection(closestAPIndex);
                            networkAdapter.notifyDataSetChanged();

                        }
                    }));

                } else {
                    displayAlert("Network Scan Result", "No WiFi networks found.");
                }
            } else {
                displayAlert("Network Scan Result", "Invalid response received.");
            }

        }else if (commandPacket.command == CommandPacket.CONNECT_CMD) {

            if (commandPacket.responseLength != 10) {
                displayAlert("Connect Command", "Invalid response received.");
                return;
            }

            if (commandPacket.responseBytes[5] == 0) { // station mode
                displayAlert("Connect Command", "WiLoader will be disconnected. Connection status won't be updated.");
            } else {
                if (commandPacket.APChannel == 0) {
                    displayAlert("Connect Command", "Maybe WiLoader's AP channel will be changed.");
                } else if (commandPacket.APChannel != commandPacket.responseBytes[5]) {
                    displayAlert("Connect Command", "Clients will be disconnected from WiLoader's AP due to changing the channel.");
                }

                // get connection status
                // create wiloader module and set connection ip then try to send get connection status command
                int ipByte = (int) commandPacket.responseBytes[6] & 0xff;
                String targetIP = String.valueOf(ipByte);
                ipByte = (int) commandPacket.responseBytes[7] & 0xff;
                targetIP += "." + String.valueOf(ipByte);
                ipByte = (int) commandPacket.responseBytes[8] & 0xff;
                targetIP += "." + String.valueOf(ipByte);
                ipByte = (int) commandPacket.responseBytes[9] & 0xff;
                targetIP += "." + String.valueOf(ipByte);

                WiLoaderModule targetWiLoader = new WiLoaderModule();
                targetWiLoader.isValid = true;
                targetWiLoader.defaultConnectionIP = targetIP;
                InetAddress ia = null;
                try {
                    ia = InetAddress.getByName(targetIP);
                } catch (UnknownHostException ex) {
                    LOGGER.log(Level.SEVERE, ex.toString(), ex);
                }

                CommandPacket cmdPacket = new CommandPacket();
                cmdPacket.command = CommandPacket.GET_CONNECTION_STATE;
                cmdPacket.receiveTimeOut = 1500;
                CommandTerminal.renewSocket = true;
                int result;
                byte[] WiLoaderInfo = new byte[WiLoaderModule.PACKET_LENGTH];

                for (int i = 0; i < 30; i++) { // get connection status 30 times (about 21 secs)

                    try {
                        Thread.sleep(700);
                    } catch (InterruptedException ex) {
                        LOGGER.log(Level.SEVERE, ex.toString(), ex);
                        Thread.currentThread().interrupt();
                    }
                    result = CommandTerminal.sendCommand(targetWiLoader, cmdPacket);
                    LOGGER.log(Level.FINE, "process response: get connection status: attempt #{0} result: {1}", new Object[]{i, result});
                    if (result != StatusCode.OK) {
                        return;
                    }
                    if (cmdPacket.responseLength != 5 + WiLoaderModule.PACKET_LENGTH) {
                        return;
                    }

                    System.arraycopy(cmdPacket.responseBytes, 5, WiLoaderInfo, 0, WiLoaderModule.PACKET_LENGTH);

                    final WiLoaderModule tempWiLoader = new WiLoaderModule(ia, NetworkDiscovery.WILOADER_UDP_PORT, WiLoaderInfo);
                    if (tempWiLoader.isValid == false) {
                        return;
                    }

                    getActivity().runOnUiThread((new Runnable() {
                        public void run() {

                            updateLabels(tempWiLoader);
                            progressdialog.setMessage(tempWiLoader.connectionStatus);
                        }
                    }));

                    // i>1 3x700ms  = 2.1 sec delay to allow WiLoader update its connection status to "connecting"
                    if (i > 1 && tempWiLoader.connectionStatus.equalsIgnoreCase("connecting") == false) {
                        return;
                    }

                } // end of for

            }
        } // end of connect cmd

    }
}
