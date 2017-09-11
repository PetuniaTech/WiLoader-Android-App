/*
 * Copyright (C) 2017 PetuniaTech LLC.
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/4.0/
 * or send a letter to Creative Commons, PO Box 1866, Mountain View, CA 94042, USA.
 */

package com.petuniatech.wiloader.main;

import java.net.InetAddress;

/**
 * Created by PetuniaTech on 2017-06-25.
 */

public interface UserInterface {

    public void updateWiLoaderList(InetAddress address, int remotePort, byte[] response);
    public void showScanProgress(final boolean show);
    public void displayAlert (final String title, final String message);
    public void refreshWiLoaders();
}
