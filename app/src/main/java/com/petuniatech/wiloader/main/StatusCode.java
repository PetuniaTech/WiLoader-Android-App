/*
 * Copyright (C) 2017 PetuniaTech LLC.
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/4.0/
 * or send a letter to Creative Commons, PO Box 1866, Mountain View, CA 94042, USA.
 */

package com.petuniatech.wiloader.main;

import com.petuniatech.wiloader.network.SerialTerminal;

/**
 * Created by PetuniaTech on 2017-06-27.
 */

public class StatusCode {

    public final static int OK =0;
    public final static int INVALID_WILOADER_ERROR =1;
    public final static int IP_BIND_ERROR =2;
    public final static int SOCKET_CONNECTION_ERROR =3;
    public final static int SOCKET_STREAM_WRITE_ERROR =4;
    public final static int SOCKET_STREAM_READ_ERROR =5;
    public final static int READ_TIME_OUT =6;
    public final static int RECEIVED_PACKET_ERROR =7;
    public final static int RECEIVE_THREAD_ERROR =8;

    public final static int EMPTY_PATH =9;
    public final static int FILE_EXT_ERROR =10;
    public final static int FILE_NOT_FOUND =11;
    public final static int FILE_READ_ERROR =12;
    public final static int HEX_FORMAT_ERROR =13;
    public final static int HEX_RECORD_ERROR =14;
    public final static int EMPTY_FILE =15;
    public final static int HEX_CHECKSUM_ERROR =16;
    public final static int HEX_ADDRESS_OUT_OF_RANGE_ERROR =17;
    public final static int FILE_SIZE_OUT_OF_RANGE_ERROR =18;

    public final static int FILE_CREATE_ERROR =19;
    public final static int FILE_WRITE_ERROR =20;
    public final static int BUSY_DOING_COMMAND =21;
    public final static int COMMAND_FAILED =22;
    public final static int OPTIBOOT_SYNC_TIMEOUT =23;
    public final static int OPTIBOOT_WRONG_RESPONSE =24;
    public final static int OPTIBOOT_RESPONSE_TIMEOUT =25;

    public final static int INVALID_SIGNATURE =26;
    public final static int VERIFY_ERROR =27;

    public final static int STK500_SYNC_TIMEOUT =28;
    public final static int STK500_INVALID_RESPONSE =29;
    public final static int STK500_UNSUPPORTED_MODLE =30;
    public final static int STK500_RESPONSE_TIMEOUT =31;

    public final static int EEPROM_ZERO =32;
    public final static int OPTIBOOT_NO_EEP_SUUPORT =33;
    public final static int OPTIBOOT_NO_FUSE_SUUPORT =34;
    public final static int OPTIBOOT_NO_LOCK_SUUPORT =35;

    public final static int MCU_NO_FUSE =36;
    public final static int STK500_NO_FUSE_WRITE_SUUPORT =37;

    public final static int ISP_INVALID_RESPONSE =38;
    public final static int ISP_ENTER_PROG_ERROR =39;

    public final static int FILE_NO_DATA =40;
    public final static int TASK_CANCELED =41;

    public final static int SERIAL_TERMINAL_ACK_TIMEOUT =42;
    public final static int SERIAL_TERMINAL_DATA_LENGTH_ERROR =43;
    public final static int SERIAL_TERMINAL_INVALID_ACK =44;
    public final static int SERIAL_TERMINAL_BUFFER_OVERFLOW =45;
    public final static int SERIAL_TERMINAL_IN_USE_BY_BOOTLOADER =46;
    public final static int SERIAL_TERMINAL_UART_DISABLED =47;

    public static String extraErrorInfo;

    public static String getErrorMessage(int errCode){

        String errMessage;

        switch (errCode) {
            case INVALID_WILOADER_ERROR:
                errMessage = "Please select a WiLoader and try again.";
                break;
            case IP_BIND_ERROR:
                errMessage = "Could not bind socket to the entered IP address.";
                break;
            case SOCKET_CONNECTION_ERROR:
                errMessage = "Could not connect to WiLoader.";
                break;
            case SOCKET_STREAM_WRITE_ERROR:
                errMessage = "Could not transmit data through network socket.";
                break;
            case SOCKET_STREAM_READ_ERROR:
                errMessage = "Could not receive data from network socket.";
                break;
            case READ_TIME_OUT:
                errMessage = "The receiving operation has timed out.";
                break;
            case RECEIVED_PACKET_ERROR:
                errMessage = "Invalid response received from WiLoader.";
                break;
            case RECEIVE_THREAD_ERROR:
                errMessage = "An error occurred during the network receive operation.";
                break;
            case EMPTY_PATH:
                errMessage = "Please enter file path and try again.";
                break;
            case EMPTY_FILE:
                errMessage = "The selected file is empty.";
                break;
            case FILE_EXT_ERROR:
                errMessage = "The entered file extension is not supported. Please use hex/bin/eep.";
                break;
            case FILE_NOT_FOUND:
                errMessage = "The selected file could not be found.";
                break;
            case FILE_READ_ERROR:
                errMessage = "The selected file could not be read.";
                break;
            case FILE_SIZE_OUT_OF_RANGE_ERROR:
                errMessage = "File size exceeds memory limit.";
                break;
            case HEX_ADDRESS_OUT_OF_RANGE_ERROR:
                errMessage = "Hex address exceeds memory limit.";
                break;
            case HEX_FORMAT_ERROR:
            case HEX_CHECKSUM_ERROR:
            case HEX_RECORD_ERROR:
                errMessage = "The selected file is not a valid hex file." + " (Code: " + String.valueOf(errCode) + ")";
                break;
            case FILE_CREATE_ERROR:
                errMessage = "The file could not be created in the specified path.";
                break;
            case FILE_WRITE_ERROR:
                errMessage = "The selected file could not be written.";
                break;
            case BUSY_DOING_COMMAND:
                errMessage = "Another command is in progress. Please try again later.";
                break;
            case COMMAND_FAILED:
                errMessage = "The command could not be sent to WiLoader";
                break;
            case OPTIBOOT_SYNC_TIMEOUT:
                errMessage = "Optiboot sync has timed out.";
                break;
            case OPTIBOOT_WRONG_RESPONSE:
                errMessage = "Invalid response from Optiboot.";
                break;
            case OPTIBOOT_RESPONSE_TIMEOUT:
                errMessage = "Optiboot response has timed out.";
                break;
            case INVALID_SIGNATURE:
                errMessage = "Invalid signature. rcv: " + extraErrorInfo;
                break;
            case VERIFY_ERROR:
                errMessage = "Mismatch found, verify failed. " + extraErrorInfo;
                break;
            case STK500_SYNC_TIMEOUT:
                errMessage = "STK500 sync has timed out.";
                break;
            case STK500_INVALID_RESPONSE:
                errMessage = "Invalid response from STK500 bootloader.";
                break;
            case STK500_UNSUPPORTED_MODLE:
                errMessage = "The STK500 bootloader model received is not supported.";
                break;
            case STK500_RESPONSE_TIMEOUT:
                errMessage = "STK500 response has timed out.";
                break;
            case EEPROM_ZERO:
                errMessage = "EEPROM size is zero.";
                break;
            case OPTIBOOT_NO_EEP_SUUPORT:
                errMessage = "This software doesn't support EEPROM operation via Optiboot.";
                break;
            case OPTIBOOT_NO_FUSE_SUUPORT:
                errMessage = "Optiboot doesn't support fusebit read/write operation.";
                break;
            case OPTIBOOT_NO_LOCK_SUUPORT:
                errMessage = "Optiboot doesn't support lockbit read/write operation.";
                break;
            case MCU_NO_FUSE:
                errMessage = "The selected MCU has no fuse byte.";
                break;
            case STK500_NO_FUSE_WRITE_SUUPORT:
                errMessage = "STK500 bootloader doesn't support fusebit write operation.";
                break;
            case ISP_INVALID_RESPONSE:
                errMessage = "Invalid AVRISP response.";
                break;
            case ISP_ENTER_PROG_ERROR:
                errMessage = "Could not enter ISP programming mode. Please check MCU connections and clock rate.";
                break;
            case FILE_NO_DATA:
                errMessage = "The selected file has no data to write/verify.";
                break;
            case TASK_CANCELED:
                errMessage = "Task has been canceled.";
                break;
            case SERIAL_TERMINAL_ACK_TIMEOUT:
                errMessage = "Could not receive ack from WiLoader. Please disconnect, and connect again.";
                break;
            case SERIAL_TERMINAL_DATA_LENGTH_ERROR:
                errMessage = "The maximum transmit data length is " + String.valueOf(SerialTerminal.MAX_LENGTH) + ".";
                break;
            case SERIAL_TERMINAL_INVALID_ACK:
                errMessage = "Invalid ack received from WiLoader.";
                break;
            case SERIAL_TERMINAL_BUFFER_OVERFLOW:
                errMessage = "WiLoader's UART transmit buffer overflowed.";
                break;
            case SERIAL_TERMINAL_IN_USE_BY_BOOTLOADER:
                errMessage = "WiLoader's UART is in use by bootloader.";
                break;
            case SERIAL_TERMINAL_UART_DISABLED:
                errMessage = "WiLoader's UART is disabled. Please enable UART in setting tab.";
                break;
            default:
                errMessage = "An unknown error occurred.";
                break;
        }

        return errMessage;
    }

    public static boolean isNotNetworkError (int errCode){

        if(errCode == OK){
            return true;
        }

        return errCode > RECEIVE_THREAD_ERROR;
    }
}
