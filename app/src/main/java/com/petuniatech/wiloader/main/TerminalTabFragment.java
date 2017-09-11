/*
 * Copyright (C) 2017 PetuniaTech LLC.
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/4.0/
 * or send a letter to Creative Commons, PO Box 1866, Mountain View, CA 94042, USA.
 */

package com.petuniatech.wiloader.main;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.petuniatech.wiloader.R;
import com.petuniatech.wiloader.modules.WiLoaderModule;
import com.petuniatech.wiloader.network.CommandPacket;
import com.petuniatech.wiloader.network.CommandTerminal;
import com.petuniatech.wiloader.network.SerialInterface;
import com.petuniatech.wiloader.network.SerialTerminal;
import com.petuniatech.wiloader.tools.LogController;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by PetuniaTech on 2017-06-27.
 */

public class TerminalTabFragment extends Fragment implements SerialInterface {

    private static final Logger LOGGER = new LogController().getLogger();

    static SerialTerminal serialTerminal = new SerialTerminal();
    static WiLoaderModule wiLoaderModule = new WiLoaderModule();

    static volatile boolean appendCR = false;
    static volatile boolean appendLF = false;
    static volatile MsgFormat messageFormat = MsgFormat.UTF8;

    static volatile boolean connected = false;

    final RotateAnimation restartRotate = new RotateAnimation(0, 359, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
    final RotateAnimation connectRotate = new RotateAnimation(0, 359, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
    final RotateAnimation sendRotate = new RotateAnimation(0, 359, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);


    EditText messageET;
    ListView messagesContainer;
    ChatAdapter adapter;

    AlertDialog alertDialog;
    String alertMessage;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.terminal_layout, container, false);

        setRetainInstance(true);



        //Init Controls
        messagesContainer = (ListView) rootView.findViewById(R.id.messagesContainer);
        messageET = (EditText) rootView.findViewById(R.id.messageEdit);



        messagesContainer.setAdapter(adapter);
        messagesContainer.setOverScrollMode(View.OVER_SCROLL_NEVER);

        TextView meLabel = (TextView) rootView.findViewById(R.id.meLbl);
        TextView companionLabel = (TextView) rootView.findViewById(R.id.friendLabel);
        meLabel.setText("Me");
        companionLabel.setText(wiLoaderModule.toString());

        ImageButton clearBtn = (ImageButton) rootView.findViewById(R.id.clearBtn);
        final ImageButton restartBtn = (ImageButton) rootView.findViewById(R.id.resetBtn);
        final ImageButton connectBtn = (ImageButton) rootView.findViewById(R.id.connectBtn);
        final ImageButton sendBtn = (ImageButton) rootView.findViewById(R.id.sendBtn);


        connectBtn.setAnimation(connectRotate);
        restartBtn.setAnimation(restartRotate);
        sendBtn.setAnimation(sendRotate);

        if(connected){
            connectBtn.setImageResource(R.mipmap.stop_icon);
        }else{
            connectBtn.setImageResource(R.mipmap.start_icon);
        }



        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.clear();
                adapter.notifyDataSetChanged();
                scroll();
            }
        });

        // connection procedure
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (wiLoaderModule.isValid == false) {
                    displayAlert("No WiLoader Selected", "Please select a WiLoader and try again.");
                    return;
                }

                final WiLoaderModule targetWiLoader = new WiLoaderModule(wiLoaderModule);

                Runnable connectTask = new Runnable() {
                    @Override
                    public void run() {
                        if (connected == false) {


                            if (serialTerminal.setBusy() == false) {
                                displayAlert("Busy Doing Command", "Another command is in progress. Please try again later.");
                                return;
                            }


                            getActivity().runOnUiThread((new Runnable() {
                                public void run() {
                                    connectRotate.reset();
                                    connectRotate.start();
                                    connectBtn.setClickable(false);
                                    connectBtn.setEnabled(false);
                                }
                            }));

                            int result = serialTerminal.connect(targetWiLoader);
                            LOGGER.log(Level.FINE, "serial connect button: result: ", result);

                            getActivity().runOnUiThread((new Runnable() {
                                public void run() {
                                    connectRotate.cancel();
                                    connectBtn.setClickable(true);
                                    connectBtn.setEnabled(true);
                                }
                            }));

                            if (result == StatusCode.OK) {
                                connected = true;
                                getActivity().runOnUiThread((new Runnable() {
                                    public void run() {
                                        connectRotate.cancel();
                                        connectBtn.setImageResource(R.mipmap.stop_icon);
                                    }
                                }));

                            } else {
                                getActivity().runOnUiThread((new Runnable() {
                                    public void run() {
                                        connectRotate.cancel();
                                    }
                                }));

                                // show alert
                                displayAlert("Connection Error", StatusCode.getErrorMessage(result));
                            }

                            serialTerminal.setIdle();


                        } else {

                            serialTerminal.disconnect();
                            connected = false;

                            getActivity().runOnUiThread((new Runnable() {
                                public void run() {
                                    connectRotate.cancel();
                                    connectBtn.setImageResource(R.mipmap.start_icon);
                                }
                            }));
                        }
                    }
                };

                new Thread(connectTask).start();
            }
        });


        //transmission procedure
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String dataString = messageET.getText().toString();

                if (dataString.isEmpty()) {
                    dataString = "";
                    if (appendLF == false && appendCR == false) {
                        return;
                    }
                }

                if (connected == false) {
                    displayAlert("Connection Error", "Please connect to WiLoader using Play button.");
                    return;
                }

                if (serialTerminal.connectedWiLoader.isValid == false) {
                    displayAlert("Invalid WiLoader", "Please select a WiLoader and try again.");
                    return;
                }


                byte[] dataBytes = null;
                if (messageFormat == MsgFormat.UTF8) {
                    if (appendCR) {
                        dataString = dataString + (char) 0x0D;
                    }
                    if (appendLF) {
                        dataString = dataString + (char) 0x0A;
                    }
                    try {
                        dataBytes = dataString.getBytes("UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        LOGGER.log(Level.SEVERE, ex.toString(), ex);
                        displayAlert("Data Format Error", "Please check message field and try again.");
                        return;
                    }

                } else if (messageFormat == MsgFormat.HEX) {
                    //Hex String
                    if (appendCR) {
                        dataString = dataString + " 0D";
                    }
                    if (appendLF) {
                        dataString = dataString + " 0A";
                    }

                    String[] split = dataString.split("\\s+");
                    dataBytes = new byte[split.length];
                    int num;
                    for (int i = 0; i < split.length; i++) {

                        try {
                            num = Integer.parseInt(split[i], 16);
                        } catch (NumberFormatException ex) {
                            LOGGER.log(Level.SEVERE, ex.toString(), ex);
                            displayAlert("Hex Format Error", "Please enter space delimited hex byte string and try again.");
                            return;
                        }
                        //only accept positive values
                        if (num < 0 || num > 255) {
                            displayAlert("Number Range Error", "Please enter values between 0 and 255 and try again.");
                            return;
                        }

                        dataBytes[i] = (byte) num;
                    }

                } else {
                    //Decimal byte String
                    if (appendCR) {
                        dataString = dataString + " 13";
                    }
                    if (appendLF) {
                        dataString = dataString + " 10";
                    }

                    String[] split = dataString.split("\\s+");
                    dataBytes = new byte[split.length];
                    int num;
                    for (int i = 0; i < split.length; i++) {

                        try {
                            num = Integer.parseInt(split[i]);
                        } catch (NumberFormatException ex) {
                            LOGGER.log(Level.SEVERE, ex.toString(), ex);
                            displayAlert("Decimal Format Error", "Please enter space delimited decimal byte string and try again.");
                            return;
                        }
                        //only accept positive values
                        if (num < 0 || num > 255) {
                            displayAlert("Number Range Error", "Please enter values between 0 and 255 and try again.");
                            return;
                        }

                        dataBytes[i] = (byte) num;
                    }
                }

                if (dataBytes == null) {
                    displayAlert("Data Format Error", "Please check message field and try again.");
                    return;
                }

                if (dataBytes.length == 0) {
                    displayAlert("Empty Textfiled", "Please enter data in the message field.");
                    return;
                }

                if (dataBytes.length > SerialTerminal.MAX_LENGTH) {
                    displayAlert("Data Length Error", StatusCode.getErrorMessage(StatusCode.SERIAL_TERMINAL_DATA_LENGTH_ERROR));
                    return;
                }

                final String terminalString = dataString;
                final byte fDataBytes[] = Arrays.copyOf(dataBytes, dataBytes.length);

                Runnable transmitTask = new Runnable() {
                    @Override
                    public void run() {

                        if (serialTerminal.setBusy() == false) {
                            displayAlert("Busy Doing Command", "Another command is in progress. Please try again later.");
                            return;
                        }

                        getActivity().runOnUiThread((new Runnable() {
                            public void run() {
                                sendBtn.setClickable(false);
                                sendBtn.setEnabled(false);
                                sendBtn.setImageResource(R.mipmap.send_progress_icon);
                                sendRotate.reset();
                                sendRotate.start();
                            }
                        }));

                        int result = serialTerminal.sendData(fDataBytes);
                        final long tTime = serialTerminal.time;
                        LOGGER.log(Level.FINE, "serial transmit data: result: {0} time: {1}", new Object[]{result, tTime});

                        getActivity().runOnUiThread((new Runnable() {
                            public void run() {
                                sendBtn.setImageResource(R.mipmap.send_icon);
                                sendRotate.cancel();
                                sendBtn.setClickable(true);
                                sendBtn.setEnabled(true);
                            }
                        }));

                        if (result == StatusCode.OK) {

                            getActivity().runOnUiThread((new Runnable() {
                                public void run() {

                                    ChatMessage chatMessage = new ChatMessage();
                                    chatMessage.setId(1);//dummy
                                    chatMessage.setMessage(terminalString);
                                    chatMessage.setDate(new SimpleDateFormat("MMM dd, HH:mm:ss.SSS").format(new Date(tTime)));
                                    chatMessage.setMe(true);

                                    displayMessage(chatMessage);
                                    messageET.setText("");
                                }
                            }));

                        } else {
                            // show alert
                            displayAlert("Data Transmission Error", StatusCode.getErrorMessage(result));
                        }

                        serialTerminal.setIdle();
                    }
                };

                new Thread(transmitTask).start();
            }
        });


        //reset mcu procedure
        restartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final CommandPacket commandPacket = new CommandPacket();
                commandPacket.receiveTimeOut = 2000;
                commandPacket.command = CommandPacket.RESET_TARGET_CMD;

                if (wiLoaderModule.isValid == false) {
                    displayAlert("No WiLoader Selected", "Please select a WiLoader and try again.");
                    return;
                }

                if (CommandTerminal.setBusy() == false) {
                    displayAlert("Busy Doing Command", "Another command is in progress. Please try again later.");
                    return;
                }

                final WiLoaderModule targetWiLoader = new WiLoaderModule(wiLoaderModule);

                Runnable restartTask = new Runnable() {
                    @Override
                    public void run() {


                        getActivity().runOnUiThread((new Runnable() {
                            public void run() {
                                restartBtn.setClickable(false);
                                restartBtn.setEnabled(false);
                                restartRotate.reset();
                                restartRotate.start();
                            }
                        }));

                        int result = CommandTerminal.handleCommand(commandPacket, targetWiLoader);
                        LOGGER.log(Level.FINE, "send command result: ", result);

                        getActivity().runOnUiThread((new Runnable() {
                            public void run() {
                                restartBtn.setClickable(true);
                                restartBtn.setEnabled(true);
                                restartRotate.cancel();
                            }
                        }));

                        if (result == StatusCode.OK) {

                        } else {
                            displayAlert("The operation failed", StatusCode.getErrorMessage(result));
                        }

                        CommandTerminal.setIdle();

                    }
                };

                new Thread(restartTask).start();
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
        }

        appendCR = false;
        appendLF = false;
        messageFormat = MsgFormat.UTF8;
        connected = false;

        alertDialog = null;
        alertMessage = "";

        adapter = new ChatAdapter(getActivity(), new ArrayList<ChatMessage>());

        restartRotate.setDuration(500);
        restartRotate.setRepeatCount(Animation.INFINITE);
        restartRotate.setInterpolator(new LinearInterpolator());
        restartRotate.cancel();

        connectRotate.setDuration(500);
        connectRotate.setRepeatCount(Animation.INFINITE);
        connectRotate.setInterpolator(new LinearInterpolator());
        connectRotate.cancel();

        sendRotate.setDuration(500);
        sendRotate.setRepeatCount(Animation.INFINITE);
        sendRotate.setInterpolator(new LinearInterpolator());
        sendRotate.cancel();

        serialTerminal.setSource(this);
        serialTerminal.disconnect();

    }

    @Override
    public void receiveSerialData(byte[] data) {

        final long rTime = System.currentTimeMillis();
        final byte[] fdata = data;

        getActivity().runOnUiThread((new Runnable() {
            public void run() {
                // Update UI here.

                String receivedData;
                if (messageFormat == MsgFormat.DECIMAL) {
                    //Decimal string
                    receivedData = "";
                    for (int i = 0; i < fdata.length; i++) {
                        receivedData += String.valueOf((int) fdata[i] & 0xff) + "  ";
                    }

                } else {
                    //Hex string
                    receivedData = "";
                    for (int i = 0; i < fdata.length; i++) {
                        receivedData += String.format("%02x", (int) fdata[i] & 0xff) + "  ";
                    }
                }

                //replace by UTF-8 if it is valid UTF-8 string otherwise display Hex string
                if (messageFormat == MsgFormat.UTF8) {
                    String message = "";
                    try {
                        message = new String(fdata, "UTF-8");
                        receivedData = message;
                    } catch (UnsupportedEncodingException ex) {
                        LOGGER.log(Level.SEVERE, ex.toString(), ex);
                    }
                }

                LOGGER.log(Level.FINE, "update terminal: length: {0} Format: {1} time: {2} final text: {3}",
                        new Object[]{fdata.length, messageFormat, rTime, receivedData});

                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setId(2);//dummy
                chatMessage.setMessage(receivedData);
                chatMessage.setDate(new SimpleDateFormat("MMM dd, HH:mm:ss.SSS").format(new Date(rTime)));
                chatMessage.setMe(false);

                displayMessage(chatMessage);

            }
        }));
    }


    public void displayMessage(ChatMessage message) {

        adapter.add(message);
        adapter.notifyDataSetChanged();

        if (message.getIsMe()) {
            scroll();
            return;
        }
        if (messagesContainer.getLastVisiblePosition() >= messagesContainer.getCount() - 2) {
            scroll();
        }

    }

    private void scroll() {
        messagesContainer.setSelection(messagesContainer.getCount() - 1);
    }

    public void displayAlert(final String title, final String message) {

        getActivity().runOnUiThread((new Runnable() {
            public void run() {

                if(alertDialog != null){
                    if(alertDialog.isShowing() && message.equalsIgnoreCase(alertMessage)){
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

    public enum MsgFormat {
        UTF8, HEX, DECIMAL
    }

}
