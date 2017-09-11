/*
 * Copyright (C) 2017 PetuniaTech LLC.
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/4.0/
 * or send a letter to Creative Commons, PO Box 1866, Mountain View, CA 94042, USA.
 */

package com.petuniatech.wiloader.main;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;

import com.petuniatech.wiloader.R;
import com.petuniatech.wiloader.network.CommandTerminal;

/**
 * Created by PetuniaTech on 2017-06-27.
 */

public class WiLoaderActivity extends AppCompatActivity{

    SettingTabFragment settingFragment;
    TerminalTabFragment  terminalFragment;

    ViewPager pager;
    TabLayout tabLayout;

    public void onCreate(Bundle savedInstanceState) {

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wiloader);

        getSupportActionBar().hide();

        pager= (ViewPager) findViewById(R.id.view_pager);
        tabLayout= (TabLayout) findViewById(R.id.tab_layout);

        // Fragment manager to add fragment in viewpager we will pass object of Fragment manager to adpater class.
        FragmentManager manager=getSupportFragmentManager();

        //object of PagerAdapter passing fragment manager object as a parameter of constructor of PagerAdapter class.
        PagerAdapter adapter=new PagerAdapter(manager);

        //set Adapter to view pager
        pager.setAdapter(adapter);


        // adding functionality to tab and viewpager to manage each other when a page is changed or when a tab is selected
        pager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        tabLayout.addTab(tabLayout.newTab().setText("Settings"));
        tabLayout.addTab(tabLayout.newTab().setText("Terminal"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                pager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        settingFragment = (SettingTabFragment) adapter.getItem(0);
        terminalFragment = (TerminalTabFragment) adapter.getItem(1);



        Bundle wiloaderBundle = getIntent().getExtras();
        if (wiloaderBundle != null) {

            settingFragment.setArguments(wiloaderBundle);
            terminalFragment.setArguments(wiloaderBundle);
        }



    }


    @Override
    public void onBackPressed() {

        super.onBackPressed();

        if(TerminalTabFragment.serialTerminal != null){
            TerminalTabFragment.serialTerminal.disconnect();
        }

        CommandTerminal.disconnect();
    }

}
