/*
 * ******************************************************************************
 *  * Copyright (c) 2012, 2019 IBM Corp. and others
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

package com.ibm.cloudtools.system;

import com.ibm.cloudtools.agent.Constants;
import com.ibm.cloudtools.agent.ContainerAgent;
import com.ibm.cloudtools.agent.Util;
import com.ibm.cloudtools.exportMetrics.GenerateConfig;
import com.ibm.cloudtools.memory.MemoryMetricsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class SystemDump
{
    private static SystemInfo systemInfo = new SystemInfo();
    public static HardwareAbstractionLayer hardwareAbstractionLayer = systemInfo.getHardware();
    public static CentralProcessor centralProcessor = hardwareAbstractionLayer.getProcessor();
    private static OperatingSystem operatingSystem = systemInfo.getOperatingSystem();

    public static void printSystemLog()
    {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
        System.setProperty(org.slf4j.impl.SimpleLogger.LOG_FILE_KEY,
                Paths.get(Util.separatorsToSystem("Output/dump.txt")).toString());
        Logger LOG = LoggerFactory.getLogger(ContainerAgent.class);

        LOG.info("Computer System:");
        printComputerSystem(hardwareAbstractionLayer.getComputerSystem(), LOG);

        LOG.info("Processor:");
        printProcessor(hardwareAbstractionLayer.getProcessor(), LOG);

        LOG.info("Memory:");
        printMemory(hardwareAbstractionLayer.getMemory(), LOG);

        LOG.info("CPU");
        printCpu(hardwareAbstractionLayer.getProcessor(), LOG);

        LOG.info("Processes:");
        printProcesses(operatingSystem, hardwareAbstractionLayer.getMemory(), LOG);

        LOG.info("Disks:");
        printDisks(hardwareAbstractionLayer.getDiskStores(), LOG);

        LOG.info("File System:");
        printFileSystem(operatingSystem.getFileSystem(), LOG);

        LOG.info("Network interfaces:");
        printNetworkInterfaces(hardwareAbstractionLayer.getNetworkIFs(), LOG);

        LOG.info("Network parameters:");
        printNetworkParameters(operatingSystem.getNetworkParams(), LOG);

        // hardware: displays
        LOG.info("Displays:");
        printDisplays(hardwareAbstractionLayer.getDisplays(), LOG);

        // hardware: USB devices
        LOG.info("USB Devices:");
        printUsbDevices(hardwareAbstractionLayer.getUsbDevices(true), LOG);
    }

    private static void printComputerSystem(final ComputerSystem computerSystem, Logger LOG)
    {

        LOG.info("manufacturer: " + computerSystem.getManufacturer());
        LOG.info("model: " + computerSystem.getModel());
        LOG.info("serialnumber: " + computerSystem.getSerialNumber());
        final Firmware firmware = computerSystem.getFirmware();
        LOG.info("firmware:");
        LOG.info("  manufacturer: " + firmware.getManufacturer());
        LOG.info("  name: " + firmware.getName());
        LOG.info("  description: " + firmware.getDescription());
        LOG.info("  version: " + firmware.getVersion());
        LOG.info(
                "  release date: "
                        + (firmware.getReleaseDate() == null
                        ? "unknown"
                        : firmware.getReleaseDate() == null ? "unknown" : firmware.getReleaseDate()));
        final Baseboard baseboard = computerSystem.getBaseboard();
        LOG.info("baseboard:");
        LOG.info("  manufacturer: " + baseboard.getManufacturer());
        LOG.info("  model: " + baseboard.getModel());
        LOG.info("  version: " + baseboard.getVersion());
        LOG.info("  serialnumber: " + baseboard.getSerialNumber());
        LOG.info("\n");
        LOG.info("\n");
        LOG.info("\n");
    }

    private static void printProcessor(CentralProcessor processor, Logger LOG)
    {
        LOG.info(processor.toString());
        LOG.info(" " + processor.getPhysicalPackageCount() + " physical CPU package(s)");
        LOG.info(" " + processor.getPhysicalProcessorCount() + " physical CPU core(s)");
        LOG.info(" " + processor.getLogicalProcessorCount() + " logical CPU(s)");
        LOG.info("Identifier: " + processor.getIdentifier());
        LOG.info("ProcessorID: " + processor.getProcessorID());
        LOG.info("\n");
        LOG.info("\n");
        LOG.info("\n");
    }

    private static void printMemory(GlobalMemory memory, Logger LOG)
    {
        LOG.info(
                "Memory Available: "
                        + FormatUtil.formatBytes(memory.getAvailable())
                        + "/"
                        + FormatUtil.formatBytes(memory.getTotal()));
        LOG.info(
                "Swap used: "
                        + FormatUtil.formatBytes(memory.getSwapUsed())
                        + "/"
                        + FormatUtil.formatBytes(memory.getSwapTotal()));
        if (memory.getSwapUsed() > 0)
        {
            GenerateConfig.comments += "#WARNING: SWAP MEMORY IS USED!";
        }
        LOG.info("\n");
        LOG.info("\n");
        LOG.info("\n");
    }

    private static void printCpu(CentralProcessor processor, Logger LOG)
    {
        long[][] prevProcTicks = processor.getProcessorCpuLoadTicks();
        LOG.info(String.format("%nCPU LOAD TICKS: "));
        for (long[] procTicks : prevProcTicks)
        {
            LOG.info(Arrays.toString(procTicks));
        }
        long freq = processor.getVendorFreq();
        if (freq > 0)
        {
            LOG.info("Vendor Frequency: " + FormatUtil.formatHertz(freq));
        }
    }

    private static void printProcesses(OperatingSystem os, GlobalMemory memory, Logger LOG)
    {
        LOG.info(
                String.format(
                        "Processes: " + os.getProcessCount() + ", Threads: " + os.getThreadCount() + "%n"));
        // Sort by highest CPU
        List<OSProcess> procs = Arrays.asList(os.getProcesses(5, OperatingSystem.ProcessSort.CPU));

        LOG.info("   PID  %CPU %MEM       VSZ       RSS Name");
        for (int i = 0; i < procs.size() && i < 5; i++)
        {
            OSProcess p = procs.get(i);
            LOG.info(
                    String.format(
                            " %5d %5.1f %4.1f %9s %9s %s%n",
                            p.getProcessID(),
                            100d * (p.getKernelTime() + p.getUserTime()) / p.getUpTime(),
                            100d * p.getResidentSetSize() / memory.getTotal(),
                            FormatUtil.formatBytes(p.getVirtualSize()),
                            FormatUtil.formatBytes(p.getResidentSetSize()),
                            p.getName()));
        }
    }

    private static void printDisks(HWDiskStore[] diskStores, Logger LOG)
    {
        LOG.info(String.format("Disks:%n"));
        for (HWDiskStore disk : diskStores)
        {
            boolean readwrite = disk.getReads() > 0 || disk.getWrites() > 0;
            LOG.info(
                    String.format(
                            " %s: (model: %s - S/N: %s) size: %s, reads: %s (%s), writes: %s (%s), xfer: %s "
                                    + "ms%n",
                            disk.getName(),
                            disk.getModel(),
                            disk.getSerial(),
                            disk.getSize() > 0 ? FormatUtil.formatBytesDecimal(disk.getSize()) : "?",
                            readwrite ? disk.getReads() : "?",
                            readwrite ? FormatUtil.formatBytes(disk.getReadBytes()) : "?",
                            readwrite ? disk.getWrites() : "?",
                            readwrite ? FormatUtil.formatBytes(disk.getWriteBytes()) : "?",
                            readwrite ? disk.getTransferTime() : "?"));
            HWPartition[] partitions = disk.getPartitions();
            if (partitions == null)
            {
                continue;
            }
            for (HWPartition part : partitions)
            {
                LOG.info(
                        String.format(
                                " |-- %s: %s (%s) Maj:Min=%d:%d, size: %s%s%n",
                                part.getIdentification(),
                                part.getName(),
                                part.getType(),
                                part.getMajor(),
                                part.getMinor(),
                                FormatUtil.formatBytesDecimal(part.getSize()),
                                part.getMountPoint().isEmpty() ? "" : " @ " + part.getMountPoint()));
            }
        }
    }

    private static void printFileSystem(oshi.software.os.FileSystem fileSystem, Logger LOG)
    {
        LOG.info(String.format("File System:%n"));
        LOG.info(
                String.format(
                        " File Descriptors: %d/%d%n",
                        fileSystem.getOpenFileDescriptors(), fileSystem.getMaxFileDescriptors()));

        OSFileStore[] fsArray = fileSystem.getFileStores();
        for (OSFileStore fs : fsArray)
        {
            long usable = fs.getUsableSpace();
            long total = fs.getTotalSpace();

            LOG.info(
                    String.format(
                            " %s (%s) [%s] %s of %s free (%.1f%%), %s of %s files free (%.1f%%) is %s "
                                    + (fs.getLogicalVolume() != null && fs.getLogicalVolume().length() > 0
                                    ? "[%s]"
                                    : "%s")
                                    + " and is mounted at %s%n",
                            fs.getName(),
                            fs.getDescription().isEmpty() ? "file system" : fs.getDescription(),
                            fs.getType(),
                            FormatUtil.formatBytes(usable),
                            FormatUtil.formatBytes(fs.getTotalSpace()),
                            100d * usable / total,
                            fs.getFreeInodes(),
                            fs.getTotalInodes(),
                            100d * fs.getFreeInodes() / fs.getTotalInodes(),
                            fs.getVolume(),
                            fs.getLogicalVolume(),
                            fs.getMount()));
        }
    }

    private static void printNetworkInterfaces(NetworkIF[] networkIFs, Logger LOG)
    {
        LOG.info(String.format("Network interfaces:%n"));
        for (NetworkIF net : networkIFs)
        {
            LOG.info(String.format(" Name: %s (%s)%n", net.getName(), net.getDisplayName()));
            LOG.info(String.format("   MAC Address: %s %n", net.getMacaddr()));
            LOG.info(
                    String.format(
                            "   MTU: %s, Speed: %s %n",
                            net.getMTU(), FormatUtil.formatValue(net.getSpeed(), "bps")));
            LOG.info(String.format("   IPv4: %s %n", Arrays.toString(net.getIPv4addr())));
            LOG.info(String.format("   IPv6: %s %n", Arrays.toString(net.getIPv6addr())));
            boolean hasData =
                    net.getBytesRecv() > 0
                            || net.getBytesSent() > 0
                            || net.getPacketsRecv() > 0
                            || net.getPacketsSent() > 0;
            LOG.info(
                    String.format(
                            "   Traffic: received %s/%s%s; transmitted %s/%s%s %n",
                            hasData ? net.getPacketsRecv() + " packets" : "?",
                            hasData ? FormatUtil.formatBytes(net.getBytesRecv()) : "?",
                            hasData ? " (" + net.getInErrors() + " err)" : "",
                            hasData ? net.getPacketsSent() + " packets" : "?",
                            hasData ? FormatUtil.formatBytes(net.getBytesSent()) : "?",
                            hasData ? " (" + net.getOutErrors() + " err)" : ""));
        }
    }

    private static void printNetworkParameters(NetworkParams networkParams, Logger LOG)
    {
        LOG.info("Network parameters:");
        LOG.info(String.format(" Host name: %s%n", networkParams.getHostName()));
        LOG.info(String.format(" Domain name: %s%n", networkParams.getDomainName()));
        LOG.info(String.format(" DNS servers: %s%n", Arrays.toString(networkParams.getDnsServers())));
        LOG.info(String.format(" IPv4 Gateway: %s%n", networkParams.getIpv4DefaultGateway()));
        LOG.info(String.format(" IPv6 Gateway: %s%n", networkParams.getIpv6DefaultGateway()));
    }

    private static void printDisplays(Display[] displays, Logger LOG)
    {
        LOG.info("Displays:");
        int i = 0;
        for (Display display : displays)
        {
            LOG.info(" Display " + i + ":");
            LOG.info(display.toString());
            i++;
        }
    }

    private static void printUsbDevices(UsbDevice[] usbDevices, Logger LOG)
    {
        LOG.info("USB Devices:");
        for (UsbDevice usbDevice : usbDevices)
        {
            LOG.info(usbDevice.toString());
        }
    }

    public static long getResidentSize()
    {
        OSProcess osProcess = operatingSystem.getProcess(operatingSystem.getProcessId());
        MemoryMetricsImpl.resValues.addValue(osProcess.getResidentSetSize() / Constants.ONE_MB);
        return osProcess.getResidentSetSize();
    }
}
