/*
 * Copyright (C) 2017 PetuniaTech LLC.
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/4.0/
 * or send a letter to Creative Commons, PO Box 1866, Mountain View, CA 94042, USA.
 */

package com.petuniatech.wiloader.main;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.petuniatech.wiloader.R;
import com.petuniatech.wiloader.modules.WiLoaderModule;
import com.petuniatech.wiloader.network.NetworkDiscovery;
import com.petuniatech.wiloader.tools.LogController;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity implements UserInterface {


    final static String version = "1.0.0";
    private static final Logger LOGGER = new LogController().getLogger();

    static int scanInterval = 4;
    static boolean broadcast = true;
    static boolean multicast = true;
    static String specificIP = "";
    public static String IPtoBind = "";


    MySimpleArrayAdapter adapter;
    ArrayList<WiLoaderModule> wiloaderArrayList;
    ProgressBar scanProgress;
    AlertDialog alertDialog;
    String alertMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            getSupportActionBar().setTitle("WiLoader");
        }catch(Exception ex){

        }


        scanProgress = (ProgressBar) findViewById(R.id.scanProgressBar);
        scanProgress.setIndeterminate(true);
        scanProgress.setVisibility(View.INVISIBLE);

        wiloaderArrayList = new ArrayList<WiLoaderModule>();
        adapter = new MySimpleArrayAdapter(this, wiloaderArrayList);
        ListView list = (ListView) findViewById(R.id.WiLoaderList);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id)
            {


                NetworkDiscovery.sendRepititiveDiscovryPacket(0); // stop sending discovery packets
                scanProgress.setVisibility(View.INVISIBLE);

                Intent newActivity = new Intent(MainActivity.this, WiLoaderActivity.class);

                Bundle b = new Bundle();
                b.putParcelable("WiLoader", wiloaderArrayList.get(position));
                newActivity.putExtras(b);

                startActivity(newActivity);

            }

        });

        NetworkDiscovery.setUI(this);
        NetworkDiscovery.initSocket(IPtoBind,specificIP,multicast,broadcast);
        NetworkDiscovery.sendRepititiveDiscovryPacket(scanInterval);



    }

    /*@Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_main);
        Log.e("main", "onConfigurationChanged");
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_info) {

            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("WiLoader App v" + version);
            alertDialog.setMessage("Copyright (C) 2017  PetuniaTech LLC. For more information, visit www.wiloader.com");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();

            return true;

        }else if(id == R.id.action_setting){

            final Dialog dialog = new Dialog(MainActivity.this);
            dialog.setContentView(R.layout.network_setting_menu);
            dialog.setTitle("Network Settings");

            final NumberPicker intervalPicker = (NumberPicker) dialog.findViewById(R.id.numberPicker);
            intervalPicker.setMaxValue(999);
            intervalPicker.setMinValue(3);
            intervalPicker.setWrapSelectorWheel(false);

            intervalPicker.setValue(scanInterval);

            final TextView specificIPEditText = (TextView) dialog.findViewById(R.id.specIPEditText);
            specificIPEditText.setText(specificIP);

            ArrayList<String> IPList = NetworkDiscovery.getIPAddresses();
            ArrayAdapter<String> IPAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, IPList);
            IPAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);


            final Spinner bindIPSpinner = (Spinner) dialog.findViewById(R.id.bindIPSpinner);
            bindIPSpinner.setAdapter(IPAdapter);


            final CheckBox broadcastCB = (CheckBox) dialog.findViewById(R.id.broadcastCheckBox);
            final CheckBox multicastCB = (CheckBox) dialog.findViewById(R.id.multicastCheckBox);
            final CheckBox bindIPCB = (CheckBox) dialog.findViewById(R.id.bindToIPCheckBox);

            broadcastCB.setChecked(broadcast);
            multicastCB.setChecked(multicast);

            if(IPtoBind.isEmpty()){
                bindIPCB.setChecked(false);
                bindIPSpinner.setEnabled(false);
            }else{
                bindIPCB.setChecked(true);
                bindIPSpinner.setEnabled(true);
                int index = IPList.indexOf(IPtoBind);
                if(index>= 0){
                    bindIPSpinner.setSelection(index);
                }
            }

            bindIPCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    if(isChecked) {
                        bindIPSpinner.setEnabled(true);
                    } else {
                        bindIPSpinner.setEnabled(false);
                    }
                }
            });

            Button applyBT = (Button) dialog.findViewById(R.id.applyBT);
            // if button is clicked, close the custom dialog
            applyBT.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String specIP = specificIPEditText.getText().toString();
                    specIP = specIP.replace(" ","");
                    if(specIP.isEmpty() == false){
                        if(SettingTabFragment.isValidIP(specIP) == false){
                            displayAlert("IP Format Error", "Please enter valid IPv4 address in Specific IP textfield.");
                            return;
                        }
                    }

                    if(bindIPCB.isChecked()){
                        if(bindIPSpinner.getSelectedItemPosition() <0){
                            displayAlert("IP Format Error", "Please select an IP to bind.");
                            return;
                        }

                        if(SettingTabFragment.isValidIP(bindIPSpinner.getSelectedItem().toString()) == false){
                            displayAlert("IP Format Error", "Please select an IP to bind.");
                            return;
                        }
                    }

                    broadcast = broadcastCB.isChecked();
                    multicast = multicastCB.isChecked();
                    scanInterval = intervalPicker.getValue();
                    specificIP = specIP;
                    if(bindIPCB.isChecked()){
                        IPtoBind = bindIPSpinner.getSelectedItem().toString();
                    }else{
                        IPtoBind = "";
                    }

                    NetworkDiscovery.initSocket(IPtoBind,specificIP,multicast,broadcast);
                    NetworkDiscovery.sendRepititiveDiscovryPacket(scanInterval);

                    dialog.dismiss();
                }
            });



            dialog.show();
        }

        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onRestart() {
        super.onRestart();

        wiloaderArrayList.clear();
        adapter.notifyDataSetChanged();
        NetworkDiscovery.sendRepititiveDiscovryPacket(scanInterval);

    }

    @Override
    public void onResume() {
        super.onResume();

        wiloaderArrayList.clear();
        adapter.notifyDataSetChanged();
        NetworkDiscovery.sendRepititiveDiscovryPacket(scanInterval);

    }

    @Override
    public void refreshWiLoaders(){

        MainActivity.this.runOnUiThread((new Runnable() {
            public void run() {

                for(WiLoaderModule tempModule: wiloaderArrayList){
                    tempModule.noResponseCount ++;
                }

                // remove WiLoader from the list if it doesn't respond 2 times
                try {
                    Iterator<WiLoaderModule> it = wiloaderArrayList.iterator();
                    while (it.hasNext()) {
                        if (it.next().noResponseCount > 2) {
                            it.remove();
                        }
                    }
                }catch(Exception ex){
                    LOGGER.log(Level.SEVERE, ex.toString(), ex);
                }

                adapter.notifyDataSetChanged();
            }
        }));
    }

    @Override
    public void updateWiLoaderList(InetAddress address, int remotePort, byte[] response) {

        final WiLoaderModule wiloaderModule = new WiLoaderModule(address, remotePort, response);

        if (wiloaderModule.isValid == false) {
            return;
        }

        MainActivity.this.runOnUiThread((new Runnable() {
            public void run() {

                boolean listed = false;
                for(WiLoaderModule tempModule: wiloaderArrayList){
                    if(tempModule.isEqualTo(wiloaderModule)){
                        listed = true;
                        tempModule.update(wiloaderModule);
                        tempModule.noResponseCount =0;
                    }
                }

                if(listed == false){
                    wiloaderArrayList.add(wiloaderModule);
                }

                adapter.notifyDataSetChanged();
            }
        }));

    }

    @Override
    public void showScanProgress(final boolean show){

        MainActivity.this.runOnUiThread((new Runnable() {
            public void run() {
                if(show){
                    scanProgress.setVisibility(View.VISIBLE);
                }else {
                    scanProgress.setVisibility(View.INVISIBLE);
                }
            }
        }));

    }

    @Override
    public void displayAlert (final String title, final String message){

        MainActivity.this.runOnUiThread((new Runnable() {
            public void run() {

                if(alertDialog != null){
                    if(alertDialog.isShowing() && message.equalsIgnoreCase(alertMessage)){
                        return;
                    }
                }

                alertMessage = message;
                alertDialog = new AlertDialog.Builder(MainActivity.this).create();
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

        public class MySimpleArrayAdapter extends ArrayAdapter<WiLoaderModule> {
        private final Context context;

        private final ArrayList<WiLoaderModule> wiloaderList;

        public MySimpleArrayAdapter(Context context, ArrayList<WiLoaderModule> list) {
            super(context, R.layout.listview, list);
            this.context = context;

            this.wiloaderList = list;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.listview, parent, false);
            TextView nameTextView = (TextView) rowView.findViewById(R.id.name);
            TextView IPTextView = (TextView) rowView.findViewById(R.id.IP);
            TextView RSSITextView = (TextView) rowView.findViewById(R.id.RSSI);
            TextView voltageTextView = (TextView) rowView.findViewById(R.id.targetVoltage);

            WiLoaderModule wiloader = wiloaderList.get(position);

            ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);

            if (wiloader.rssiText.isEmpty()) {
                RSSITextView.setText("");
                imageView.setImageResource(R.mipmap.logo_red);
            } else {

                RSSITextView.setText(wiloader.rssiText + "  dbm");

                if (wiloader.rssiByte >= -60) {
                    imageView.setImageResource(R.mipmap.logo_blue);
                    RSSITextView.setTextColor(Color.BLUE);
                } else if (wiloader.rssiByte >= -70) {
                    imageView.setImageResource(R.mipmap.logo_green);
                    RSSITextView.setTextColor(Color.GREEN);
                } else if (wiloader.rssiByte >= -80) {
                    imageView.setImageResource(R.mipmap.logo_yellow);
                    RSSITextView.setTextColor(Color.argb(255,255,220,0)); // Gold color
                } else {
                    imageView.setImageResource(R.mipmap.logo_red);
                    RSSITextView.setTextColor(Color.RED);
                }
            }

            nameTextView.setText(wiloader.toString());

            if (wiloader.mode.equalsIgnoreCase("station")) {
                IPTextView.setText(wiloader.stationIP + " (ST)");
            } else if (wiloader.mode.equalsIgnoreCase("AP")) {
                IPTextView.setText(wiloader.softAPIP + " (AP)");
            } else if (wiloader.mode.equalsIgnoreCase("station+AP")) {
                if (wiloader.stationIP.equalsIgnoreCase("0.0.0.0")) {
                    IPTextView.setText(wiloader.softAPIP + " (AP)");
                } else {
                    IPTextView.setText(wiloader.stationIP + " (STAP)");
                }
            }

            voltageTextView.setText(wiloader.targetVoltageText + "v");

            return rowView;
        }
    }// end of MySimpleArrayAdapter

}
