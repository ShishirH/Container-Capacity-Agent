/*
 * ******************************************************************************
 *  * Copyright (c) 2012, 2018 IBM Corp. and others
 *  *
 *  * This program and the accompanying materials are made available under
 *  * the terms of the Eclipse Public License 2.0 which accompanies this
 *  * distribution and is available at https://www.eclipse.org/legal/epl-2.0/
 *  * or the Apache License, Version 2.0 which accompanies this distribution and
 *  * is available at https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * This Source Code may also be made available under the following
 *  * Secondary Licenses when the conditions for such availability set
 *  * forth in the Eclipse Public License, v. 2.0 are satisfied: GNU
 *  * General Public License, version 2 with the GNU Classpath
 *  * Exception [1] and GNU General Public License, version 2 with the
 *  * OpenJDK Assembly Exception [2].
 *  *
 *  * [1] https://www.gnu.org/software/classpath/license.html
 *  * [2] http://openjdk.java.net/legal/assembly-exception.html
 *  *
 *  * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0 OR GPL-2.0 WITH Classpath-exception-2.0 OR LicenseRef-GPL-2.0 WITH Assembly-exception
 *  ******************************************************************************
 */

package com.ibm.cloudtools.agent;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

import java.util.HashMap;
import java.util.Map;

/**
 * Uses OSHI to attempt to identify whether the user is on a Virtual Machine
 */
public class DetectVM
{
    /*
    Constant for Mac address OUI portion, the first 24 bits of MAC address
    https://www.webopedia.com/TERM/O/OUI.html
    */
    private static final Map<String, String> vmMacAddressOUI = new HashMap<>();
    private static final String[] vmModelArray =
            new String[]{
                    "KVM",
                    "lguest",
                    "OpenVZ",
                    "Qemu",
                    "Microsoft Virtual PC",
                    "VMWare",
                    "linux-vserver",
                    "Xen",
                    "FreeBSD Jail",
                    "VirtualBox",
                    "Parallels",
                    "Linux Containers",
                    "LXC"
            };

    static
    {
        vmMacAddressOUI.put("00:50:56", "VMware ESX 3");
        vmMacAddressOUI.put("00:0C:29", "VMware ESX 3");
        vmMacAddressOUI.put("00:05:69", "VMware ESX 3");
        vmMacAddressOUI.put("00:03:FF", "Microsoft Hyper-V");
        vmMacAddressOUI.put("00:1C:42", "Parallels Desktop");
        vmMacAddressOUI.put("00:0F:4B", "Virtual Iron 4");
        vmMacAddressOUI.put("00:16:3E", "Xen or Oracle VM");
        vmMacAddressOUI.put("08:00:27", "VirtualBox");
    }

    /**
     * The function attempts to identify which Virtual Machine (VM) based on common VM signatures in
     * MAC address and computer model.
     *
     * @return A boolean indicating whether it is running on a VM.
     */
    public static boolean identifyVM()
    {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardwareAbstractionLayer = systemInfo.getHardware();

        /* Try well known MAC addresses */
        NetworkIF[] nifs = hardwareAbstractionLayer.getNetworkIFs();

        for (NetworkIF nif : nifs)
        {
            String mac = nif.getMacaddr().substring(0, 8).toUpperCase();
            if (vmMacAddressOUI.containsKey(mac))
            {
                if (nif.getName().contains("virbr"))
                {
                    continue;
                }
                System.err.println(vmMacAddressOUI.get(mac));
                return true;
            }
        }

        /* Try well known models */
        String model = hardwareAbstractionLayer.getComputerSystem().getModel();
        for (String vm : vmModelArray)
        {
            if (model.contains(vm))
            {
                System.err.println("RUNNING ON VM: " + vm);
                return true;
            }
        }
        String manufacturer = hardwareAbstractionLayer.getComputerSystem().getManufacturer();
        if ("Microsoft Corporation".equals(manufacturer) && "Virtual Machine".equals(model))
        {
            System.err.println("RUNNING ON VM: Microsoft Hyper-V");
        }

        /* Couldn't find VM, return empty string */
        return false;
    }
}
