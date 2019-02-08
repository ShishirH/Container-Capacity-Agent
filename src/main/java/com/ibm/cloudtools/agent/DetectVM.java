package com.ibm.cloudtools.agent;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

import java.util.HashMap;
import java.util.Map;

/**
 * Uses OSHI to attempt to identify whether the user is on a Virtual Machine
 */
class DetectVM
{

    // Constant for Mac address OUI portion, the first 24 bits of MAC address
    // https://www.webopedia.com/TERM/O/OUI.html
    private static final Map<String, String> vmMacAddressOUI = new HashMap<>();
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

    private static final String[] vmModelArray = new String[] { "KVM", "lguest", "OpenVZ", "Qemu",
            "Microsoft Virtual PC", "VMWare", "linux-vserver", "Xen", "FreeBSD Jail", "VirtualBox", "Parallels",
            "Linux Containers", "LXC" };


    /**
     * The function attempts to identify which Virtual Machine (VM) based on
     * common VM signatures in MAC address and computer model.
     *
     * @return A string indicating the machine's virtualization info if it can
     *         be determined, or an emptry string otherwise.
     */
    static boolean identifyVM()
    {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hw = si.getHardware();

        // Try well known MAC addresses
        NetworkIF[] nifs = hw.getNetworkIFs();

        for (NetworkIF nif : nifs) {
            String mac = nif.getMacaddr().substring(0, 8).toUpperCase();
            if (vmMacAddressOUI.containsKey(mac)) {
                System.err.println(vmMacAddressOUI.get(mac));
                return true;
            }
        }

        // Try well known models
        String model = hw.getComputerSystem().getModel();
        for (String vm : vmModelArray) {
            if (model.contains(vm)) {
                System.err.println("RUNNING ON VM: " + vm);
                return true;
            }
        }
        String manufacturer = hw.getComputerSystem().getManufacturer();
        if ("Microsoft Corporation".equals(manufacturer) && "Virtual Machine".equals(model)) {
            System.err.println("RUNNING ON VM: Microsoft Hyper-V");
        }

        // Couldn't find VM, return empty string
        return false;
    }
}