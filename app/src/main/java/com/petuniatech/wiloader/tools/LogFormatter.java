/*
 * Copyright (C) 2017 PetuniaTech LLC.
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/4.0/
 * or send a letter to Creative Commons, PO Box 1866, Mountain View, CA 94042, USA.
 */

package com.petuniatech.wiloader.tools;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Created by PetuniaTech on 2017-06-24.
 */

public class LogFormatter extends Formatter {

    static final String LINE_SEP = System.getProperty("line.separator", "\r\n");
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    String message;
    StringBuilder strHolder = new StringBuilder();
    StringBuilder str = new StringBuilder();



    @Override
    public String format(LogRecord logRecord) {

        strHolder.delete(0, strHolder.length());
        strHolder.append(sdf.format(new Date(logRecord.getMillis()))).append(" ");
        strHolder.append(logRecord.getSourceClassName()).append(": ");
        strHolder.append(logRecord.getSourceMethodName()).append(": ");
        strHolder.append(logRecord.getThreadID());
        strHolder.append(LINE_SEP);
        strHolder.append(logRecord.getLevel()).append(": ");
        message = logRecord.getMessage();
        Object[] params = logRecord.getParameters();
        if (params != null) {
            int index;
            for (int i = 0; i < params.length; i++) {
                try {
                    index = message.lastIndexOf("{" + i + "}");
                    // handle array as object param
                    if (index < 0) {
                        message = logRecord.getMessage();
                        if (params.length == 1 && params[0] != null && params[0].getClass().isArray()) {
                            message += arrayObjToString(params[0]);
                        } else {
                            for (Object param : params) {
                                message += " " + String.valueOf((Object)String.valueOf(param));
                            }
                        }
                        break;
                    }
                    message = message.substring(0, index) + message.substring(index).replace("{" + i + "}", String.valueOf((Object)String.valueOf(params[i])));
                } catch (Exception e) {
                }
            }
        }

        strHolder.append(message);

        Throwable e = logRecord.getThrown();
        if (e != null) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            strHolder.append(LINE_SEP).append(sw.toString());
        }

        strHolder.append(LINE_SEP).append(LINE_SEP);

        return strHolder.toString();
    }

    private String arrayObjToString(Object arrObj) {

        str.delete(0, str.length());
        Class<?> componentType = arrObj.getClass().getComponentType();

        if (componentType == boolean.class) {
            boolean[] b = (boolean[]) arrObj;
            for (boolean bl : b) {
                str.append(" ").append(bl);
            }
        }else if (componentType == byte.class) {
            byte[] b = (byte[]) arrObj;
            for (byte bt : b) {
                str.append(" ").append(Integer.toHexString((int) bt & 0xff));
            }
        }else if (componentType == char.class) {
            char[] c = (char[]) arrObj;
            for (char ch : c) {
                str.append(" ").append(ch);
            }
        }else if (componentType == double.class) {
            double[] d = (double[]) arrObj;
            for (double db : d) {
                str.append(" ").append(db);
            }
        }else if (componentType == float.class) {
            float[] f = (float[]) arrObj;
            for (float ft : f) {
                str.append(" ").append(ft);
            }
        }else if (componentType == int.class) {
            int[] i = (int[]) arrObj;
            for (int it : i) {
                str.append(" ").append(it);
            }
        }else if (componentType == long.class) {
            long[] l = (long[]) arrObj;
            for (long lg : l) {
                str.append(" ").append(lg);
            }
        }else if (componentType == short.class) {
            short[] s = (short[]) arrObj;
            for (short sh : s) {
                str.append(" ").append(sh);
            }
        }else if (componentType == String.class) {
            String[] s = (String[]) arrObj;
            for (String st : s) {
                str.append(" ").append(st);
            }
        }

        return str.toString();
    }
}
