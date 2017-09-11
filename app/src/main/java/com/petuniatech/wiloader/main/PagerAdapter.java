/*
 * Copyright (C) 2017 PetuniaTech LLC.
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/4.0/
 * or send a letter to Creative Commons, PO Box 1866, Mountain View, CA 94042, USA.
 */
package com.petuniatech.wiloader.main;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Created by PetuniaTech on 2017-06-27.
 */

public class PagerAdapter extends FragmentStatePagerAdapter {
    private  SettingTabFragment  settingFrag;
    private  TerminalTabFragment terminalFrag;

    public PagerAdapter(FragmentManager fm) {
        super(fm);

        this.settingFrag = new SettingTabFragment();
        this.terminalFrag = new TerminalTabFragment();
    }

    @Override
    public Fragment getItem(int position) {

        switch (position){
            case 0:
                return settingFrag;

            case 1:
                return terminalFrag;

            default:
                return null;

        }

    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        String title=" ";
        switch (position){
            case 0:
                title="Setting";
                break;
            case 1:
                title="Terminal";
                break;

        }

        return title;
    }
}
