/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (C) 2002-2007 Maxence Bernard
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mucommander.runtime;

import com.mucommander.Debug;

/**
 * @author Maxence Bernard
*/
public class OsFamily extends RuntimeProperty {

    private final static OsFamily currentValue = parseSystemProperty(getRawSystemProperty());

    static {
        if(Debug.ON) Debug.trace("Current OS family: "+ currentValue);
    }

    protected OsFamily(String osFamilyString) {
        super(osFamilyString);
    }

    public static String getRawSystemProperty() {
        return System.getProperty("os.name");
    }

    public static OsFamily getCurrent() {
        return currentValue;
    }

    static OsFamily parseSystemProperty(String osNameProp) {
        OsFamily osFamily;

        // Windows family
        if(osNameProp.startsWith("Windows")) {
            // Windows 95, 98, Me
            if (osNameProp.startsWith("Windows 95") || osNameProp.startsWith("Windows 98") || osNameProp.startsWith("Windows Me")) {
                osFamily = OsFamilies.WINDOWS_9X;
            }
            // Windows NT, 2000, XP and up
            else {
                osFamily = OsFamilies.WINDOWS_NT;
            }
        }
        // Mac OS X family
        else if(osNameProp.startsWith("Mac OS X")) {
            osFamily = OsFamilies.MAC_OS_X;
        }
        // OS/2 family
        else if(osNameProp.startsWith("OS/2")) {
            osFamily = OsFamilies.OS_2;
        }
        // Linux family
        else if(osNameProp.startsWith("Linux")) {
            osFamily = OsFamilies.LINUX;
        }
        // Solaris family
        else if(osNameProp.startsWith("Solaris") || osNameProp.startsWith("SunOS")) {
            osFamily = OsFamilies.SOLARIS;
        }
        // Any other OS
        else {
            osFamily = OsFamilies.UNKNOWN_OS_FAMILY;
        }

        return osFamily;
    }

    /////////////////////////////////////
    // RuntimeProperty implementation //
    /////////////////////////////////////

    protected RuntimeProperty getCurrentValue() {
        return currentValue;
    }
}
