package com.fxz.console.controller;

import com.fxz.console.pojo.SystemInfoSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;
import oshi.util.Util;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/system")
public class SystemInfoController {

    private static List<String> diskNames;
    private static List<String> ips;


    private static List<String> mappers;

    public static void setIps(@Value("${system.info.ips:}") List<String> ips) {
        SystemInfoController.ips = ips;
    }

    public static void setDiskNames(@Value("${system.info.disks:}") List<String> diskNames) {
        SystemInfoController.diskNames = diskNames;
    }

    public static void setMappers(@Value("${system.info.mappers:}") List<String> mappers) {
        SystemInfoController.mappers = mappers;
    }

    private static Map<String, DiskStats> prevDiskStats = new HashMap<>();
    private static Map<String, NetStats> prevNetStats = new HashMap<>();
    private static long[] prevCpuTicks = null;


    @PostConstruct
    public void init() {
        clearCache();
        SystemInfoSummary summary = getSystemInfoWithAccurateSpeed();
        printSystemInfo(summary);
    }

    @GetMapping("/info")
    public SystemInfoSummary info() {
        return getSystemInfo();
    }

    private static class DiskStats {
        long readBytes;
        long writeBytes;
        long timestamp;

        DiskStats(long readBytes, long writeBytes, long timestamp) {
            this.readBytes = readBytes;
            this.writeBytes = writeBytes;
            this.timestamp = timestamp;
        }
    }

    private static class NetStats {
        long rxBytes;
        long txBytes;
        long timestamp;

        NetStats(long rxBytes, long txBytes, long timestamp) {
            this.rxBytes = rxBytes;
            this.txBytes = txBytes;
            this.timestamp = timestamp;
        }
    }

    /**
     * 获取系统信息汇总（需要基准数据，第一次调用可能速度数据不准确）
     */
    public static SystemInfoSummary getSystemInfo() {
        SystemInfoSummary summary = new SystemInfoSummary();
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        OperatingSystem os = systemInfo.getOperatingSystem();

        // 设置操作系统信息
        summary.setOs(os.getFamily());
        summary.setUpTime(formatUptime(os.getSystemUptime()));

        // 获取CPU信息
        summary.setCpuInfo(getCpuInfo(hardware));

        // 获取内存信息
        summary.setMemoryInfo(getMemoryInfo(hardware));

        // 获取磁盘信息
        summary.setDiskInfo(getDiskInfo(os, hardware));

        // 获取网络信息
        summary.setNetInfo(getNetInfo(hardware));

        return summary;
    }

    /**
     * 获取包含准确速度数据的系统信息（包含1秒延迟用于采样）
     */
    public static SystemInfoSummary getSystemInfoWithAccurateSpeed() {
        // 首先进行一次采样作为基准
        initBaselineData();

        // 等待1秒用于计算准确的速度
        Util.sleep(1000);

        // 获取完整的系统信息
        return getSystemInfo();
    }

    /**
     * 初始化基准数据
     */
    private static void initBaselineData() {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();

        // 初始化CPU基准数据
        CentralProcessor processor = hardware.getProcessor();
        prevCpuTicks = processor.getSystemCpuLoadTicks();

        // 初始化磁盘基准数据
        prevDiskStats.clear();
        List<HWDiskStore> diskStores = hardware.getDiskStores();
        long currentTime = System.currentTimeMillis();
        for (HWDiskStore diskStore : diskStores) {
            if (isMatch(diskStore.getModel(), diskNames)) {
                diskStore.updateAttributes();
                String diskName = diskStore.getName();
                prevDiskStats.put(diskName, new DiskStats(
                        diskStore.getReadBytes(),
                        diskStore.getWriteBytes(),
                        currentTime
                ));
            }
        }

        // 初始化网络基准数据
        prevNetStats.clear();
        List<NetworkIF> networkIFs = hardware.getNetworkIFs();
        for (NetworkIF net : networkIFs) {
            if (isMatch(!CollectionUtils.isEmpty(Arrays.asList(net.getIPv4addr())) ? net.getIPv4addr()[0] : "null", ips)) {
                net.updateAttributes();
                String netName = net.getName();
                prevNetStats.put(netName, new NetStats(
                        net.getBytesRecv(),
                        net.getBytesSent(),
                        currentTime
                ));
            }
        }
    }

    /**
     * 格式化运行时间
     */
    private static String formatUptime(long seconds) {
        long days = TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) - TimeUnit.DAYS.toHours(days);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) - TimeUnit.HOURS.toMinutes(hours) - TimeUnit.DAYS.toMinutes(days);
        long secs = seconds - TimeUnit.DAYS.toSeconds(days) - TimeUnit.HOURS.toSeconds(hours) - TimeUnit.MINUTES.toSeconds(minutes);
        if (days > 0) {
            return String.format("%dd %02d:%02d:%02d", days, hours, minutes, secs);
        } else if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%02d:%02d", minutes, secs);
        }


    }

    private static SystemInfoSummary.CpuInfo getCpuInfo(HardwareAbstractionLayer hardware) {
        SystemInfoSummary.CpuInfo cpuInfo = new SystemInfoSummary.CpuInfo();
        CentralProcessor processor = hardware.getProcessor();

        // 获取CPU型号
        cpuInfo.setModel(processor.getProcessorIdentifier().getName());

        // 获取CPU核心数和线程数
        cpuInfo.setCores(processor.getPhysicalProcessorCount());
        cpuInfo.setThreads(processor.getLogicalProcessorCount());

        // 获取CPU使用率
        long[] ticks;
        if (prevCpuTicks == null) {
            // 第一次调用，获取基准数据
            prevCpuTicks = processor.getSystemCpuLoadTicks();
            Util.sleep(500); // 短暂延迟
        }
        ticks = processor.getSystemCpuLoadTicks();

        long user = ticks[CentralProcessor.TickType.USER.getIndex()] - prevCpuTicks[CentralProcessor.TickType.USER.getIndex()];
        long nice = ticks[CentralProcessor.TickType.NICE.getIndex()] - prevCpuTicks[CentralProcessor.TickType.NICE.getIndex()];
        long sys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()] - prevCpuTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
        long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()] - prevCpuTicks[CentralProcessor.TickType.IDLE.getIndex()];
        long iowait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()] - prevCpuTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
        long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()] - prevCpuTicks[CentralProcessor.TickType.IRQ.getIndex()];
        long softirq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()] - prevCpuTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
        long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()] - prevCpuTicks[CentralProcessor.TickType.STEAL.getIndex()];
        long totalCpu = user + nice + sys + idle + iowait + irq + softirq + steal;

        if (totalCpu > 0) {
            cpuInfo.setUser(Math.min(100, user * 100L / totalCpu));
            cpuInfo.setSys(Math.min(100, sys * 100L / totalCpu));
            cpuInfo.setIo(Math.min(100, iowait * 100L / totalCpu));
            cpuInfo.setUsed(Math.min(100, (totalCpu - idle) * 100L / totalCpu));
        }

        // 更新上一次的CPU ticks
        prevCpuTicks = ticks;
        // 获取系统负载
        double[] loadAverage = processor.getSystemLoadAverage(3);
        cpuInfo.setLoad1min(loadAverage[0] >= 0 ? loadAverage[0] : 0);
        cpuInfo.setLoad5min(loadAverage[1] >= 0 ? loadAverage[1] : 0);
        cpuInfo.setLoad15min(loadAverage[2] >= 0 ? loadAverage[2] : 0);
        // 获取CPU温度（如果可用）
        try {
            Sensors sensors = hardware.getSensors();
            cpuInfo.setTemp(sensors.getCpuTemperature());
        } catch (Exception e) {
            cpuInfo.setTemp(0.0);
        }
        return cpuInfo;
    }

    private static SystemInfoSummary.MemoryInfo getMemoryInfo(HardwareAbstractionLayer hardware) {
        SystemInfoSummary.MemoryInfo memoryInfo = new SystemInfoSummary.MemoryInfo();
        GlobalMemory memory = hardware.getMemory();
        memoryInfo.setSize(memory.getTotal());
        memoryInfo.setUsed(memory.getTotal() - memory.getAvailable());
        memoryInfo.setFree(memory.getAvailable());
        memoryInfo.setSwapSize(memory.getVirtualMemory().getSwapTotal());
        memoryInfo.setSwapUsed(memory.getVirtualMemory().getSwapUsed());
        memoryInfo.setSwapFree(memory.getVirtualMemory().getSwapUsed() - memory.getVirtualMemory().getSwapUsed());
        return memoryInfo;
    }

    private static SystemInfoSummary.DiskInfo getDiskInfo(OperatingSystem os, HardwareAbstractionLayer hardware) {
        SystemInfoSummary.DiskInfo diskInfo = new SystemInfoSummary.DiskInfo();

        // 获取文件系统信息
        List<SystemInfoSummary.FileSystemInfo> fileSystems = new ArrayList<>();
        FileSystem fileSystem = os.getFileSystem();
        List<OSFileStore> fsArray = fileSystem.getFileStores();

        for (OSFileStore fs : fsArray) {
            if (isMatch(fs.getMount(), mappers)) {
                SystemInfoSummary.FileSystemInfo fsInfo = new SystemInfoSummary.FileSystemInfo();
                fsInfo.setMounted(fs.getMount());
                fsInfo.setSize(fs.getTotalSpace());
                long usableSpace = fs.getUsableSpace();
                fsInfo.setUsed(fs.getTotalSpace() - usableSpace);
                fsInfo.setFree(usableSpace);
                fileSystems.add(fsInfo);
            }
        }

        diskInfo.setFileSystems(fileSystems);

        // 获取物理磁盘信息
        diskInfo.setDisks(getPhysicalDisks(hardware));
        if (!CollectionUtils.isEmpty(diskInfo.getDisks())) {
            long totalR = 0, totalW = 0;
            for (SystemInfoSummary.Disk disk : diskInfo.getDisks()) {
                totalR += disk.getRead();
                totalW += disk.getWrite();
            }
            diskInfo.setTotalR(totalR);
            diskInfo.setTotalW(totalW);
        }
        return diskInfo;
    }

    private static List<SystemInfoSummary.Disk> getPhysicalDisks(HardwareAbstractionLayer hardware) {
        List<SystemInfoSummary.Disk> disks = new ArrayList<>();
        List<HWDiskStore> diskStores = hardware.getDiskStores();
        long currentTime = System.currentTimeMillis();
        for (HWDiskStore diskStore : diskStores) {
            // 更新磁盘信息以获取最新数据
            diskStore.updateAttributes();

            String diskName = diskStore.getName();
            String diskModel = diskStore.getModel().trim();
            long size = diskStore.getSize();
            long readBytes = diskStore.getReadBytes();
            long writeBytes = diskStore.getWriteBytes();

            SystemInfoSummary.Disk disk = new SystemInfoSummary.Disk();
            // 设置磁盘模型，如果模型为空则使用名称
            disk.setModel(diskModel.isEmpty() ? diskName : diskModel);
            disk.setSize(size);

            // 设置总读取和总写入（转换为KB）
            disk.setReadTotal(readBytes / 1024);
            disk.setWriteTotal(writeBytes / 1024);

            // 计算实时读写速度
            long readSpeed = 0;
            long writeSpeed = 0;

            if (prevDiskStats.containsKey(diskName)) {
                DiskStats prevStats = prevDiskStats.get(diskName);
                long timeDiff = Math.max(1, currentTime - prevStats.timestamp);
                long readDiff = Math.max(0, readBytes - prevStats.readBytes);
                long writeDiff = Math.max(0, writeBytes - prevStats.writeBytes);

                // 计算每秒的KB数
                readSpeed = (readDiff / 1024) * 1000 / timeDiff;
                writeSpeed = (writeDiff / 1024) * 1000 / timeDiff;
            }

            disk.setRead(readSpeed);
            disk.setWrite(writeSpeed);

            // 使用磁盘名称或序列号作为标识
            String serial = diskStore.getSerial().trim();
            disk.setMounted(serial.isEmpty() ? diskName : serial);

            disks.add(disk);

            // 更新上一次的数据
            prevDiskStats.put(diskName, new DiskStats(readBytes, writeBytes, currentTime));
        }
        return disks;
    }

    private static SystemInfoSummary.NetInfo getNetInfo(HardwareAbstractionLayer hardware) {
        SystemInfoSummary.NetInfo netInfo = new SystemInfoSummary.NetInfo();
        List<SystemInfoSummary.NetCard> cards = new ArrayList<>();
        List<NetworkIF> networkIFs = hardware.getNetworkIFs();
        long currentTime = System.currentTimeMillis();

        long totalRx = 0;
        long totalTx = 0;

        for (NetworkIF net : networkIFs) {
            // 更新网络接口信息
            net.updateAttributes();

            String netName = net.getName();
            String[] ipv4 = net.getIPv4addr();
            long rxBytes = net.getBytesRecv();
            long txBytes = net.getBytesSent();

            // 只统计有流量的网卡
            if (rxBytes == 0 && txBytes == 0) {
                continue;
            }

            SystemInfoSummary.NetCard card = new SystemInfoSummary.NetCard();
            card.setEth(netName);

            // 获取IPv4地址
            String ipAddress = "";
            if (ipv4.length > 0 && !ipv4[0].isEmpty()) {
                ipAddress = ipv4[0];
            }
            card.setIpv4(ipAddress);

            // 计算网络速度
            long rxSpeed = 0;
            long txSpeed = 0;

            if (prevNetStats.containsKey(netName)) {
                NetStats prevStats = prevNetStats.get(netName);
                long timeDiff = Math.max(1, currentTime - prevStats.timestamp);
                long rxDiff = Math.max(0, rxBytes - prevStats.rxBytes);
                long txDiff = Math.max(0, txBytes - prevStats.txBytes);

                // 计算每秒字节数
                rxSpeed = rxDiff * 1000 / timeDiff;
                txSpeed = txDiff * 1000 / timeDiff;
            }

            card.setRx(rxSpeed);
            card.setTx(txSpeed);

            cards.add(card);

            totalRx += rxSpeed;
            totalTx += txSpeed;

            // 更新上一次的数据
            prevNetStats.put(netName, new NetStats(rxBytes, txBytes, currentTime));
        }

        netInfo.setCards(cards);
        netInfo.setTotalRx(totalRx);
        netInfo.setTotalTx(totalTx);

        return netInfo;
    }

    /**
     * 格式化输出系统信息
     */
    public static void printSystemInfo(SystemInfoSummary summary) {
        System.out.println("======================================= 系统信息汇总 =======================================");
        System.out.println(String.format("操作系统: %-30s 运行时间: %s", summary.getOs(), summary.getUpTime()));

        System.out.println("\n======================================= CPU信息 =======================================");
        SystemInfoSummary.CpuInfo cpu = summary.getCpuInfo();
        System.out.println(String.format("CPU型号: %s", cpu.getModel()));
        System.out.println(String.format("核心信息: %d 物理核心, %d 逻辑核心", cpu.getCores(), cpu.getThreads()));
        System.out.println(String.format("使用情况: 总计 %d%%, 用户 %d%%, 系统 %d%%, IO等待 %d%%",
                cpu.getUsed(), cpu.getUser(), cpu.getSys(), cpu.getIo()));
        System.out.println(String.format("系统负载: %.2f (1分钟), %.2f (5分钟), %.2f (15分钟)",
                cpu.getLoad1min(), cpu.getLoad5min(), cpu.getLoad15min()));
        System.out.println(String.format("CPU温度: %.1f°C", cpu.getTemp()));

        System.out.println("\n======================================= 内存信息 =======================================");
        SystemInfoSummary.MemoryInfo mem = summary.getMemoryInfo();
        System.out.println(String.format("总内存: %s", FormatUtil.formatBytes(mem.getSize())));
        System.out.println(String.format("已使用: %s (%.1f%%)",
                FormatUtil.formatBytes(mem.getUsed()),
                (double) mem.getUsed() * 100 / mem.getSize()));
        System.out.println(String.format("可用内存: %s (%.1f%%)",
                FormatUtil.formatBytes(mem.getFree()),
                (double) mem.getFree() * 100 / mem.getSize()));

        System.out.println("\n======================================= 文件系统信息 =======================================");
        SystemInfoSummary.DiskInfo disk = summary.getDiskInfo();
        if (disk.getFileSystems() != null && !disk.getFileSystems().isEmpty()) {
            System.out.println("挂载点\t\t总容量\t\t已使用\t\t可用\t\t使用率");
            System.out.println("------------------------------------------------------------------------");
            for (SystemInfoSummary.FileSystemInfo fs : disk.getFileSystems()) {
                String mounted = fs.getMounted().length() > 12 ?
                        fs.getMounted().substring(0, 12) + "..." : fs.getMounted();
                double usage = (double) fs.getUsed() * 100 / fs.getSize();
                System.out.println(String.format("%-15s %-10s %-10s %-10s %.1f%%",
                        mounted,
                        FormatUtil.formatBytes(fs.getSize()),
                        FormatUtil.formatBytes(fs.getUsed()),
                        FormatUtil.formatBytes(fs.getFree()),
                        usage));
            }
        } else {
            System.out.println("未检测到文件系统");
        }

        System.out.println("\n======================================= 物理磁盘信息 =======================================");
        if (disk.getDisks() != null && !disk.getDisks().isEmpty()) {
            System.out.println("磁盘型号\t\t容量\t\t读取速度\t写入速度\t总读取\t\t总写入");
            System.out.println("------------------------------------------------------------------------------------------");
            for (SystemInfoSummary.Disk d : disk.getDisks()) {
                String model = d.getModel().length() > 15 ?
                        d.getModel().substring(0, 15) + "..." : d.getModel();
                System.out.println(String.format("%-18s %-10s %-8s %-8s %-12s %-12s",
                        model,
                        FormatUtil.formatBytes(d.getSize()),
                        formatSpeed(d.getRead()) + "/s",
                        formatSpeed(d.getWrite()) + "/s",
                        FormatUtil.formatBytes(d.getReadTotal() * 1024L),
                        FormatUtil.formatBytes(d.getWriteTotal() * 1024L)));
            }
        } else {
            System.out.println("未检测到物理磁盘");
        }

        System.out.println("\n======================================= 网络信息 =======================================");
        SystemInfoSummary.NetInfo net = summary.getNetInfo();
        System.out.println(String.format("网络总吞吐: 接收 %s/s, 发送 %s/s",
                FormatUtil.formatBytes(net.getTotalRx()),
                FormatUtil.formatBytes(net.getTotalTx())));

        if (net.getCards() != null && !net.getCards().isEmpty()) {
            System.out.println("网卡名称\t\tIP地址\t\t接收速度\t\t发送速度");
            System.out.println("------------------------------------------------------------------------");
            for (SystemInfoSummary.NetCard card : net.getCards()) {
                System.out.println(String.format("%-15s %-15s %-12s %-12s",
                        card.getEth(),
                        card.getIpv4().isEmpty() ? "N/A" : card.getIpv4(),
                        FormatUtil.formatBytes(card.getRx()) + "/s",
                        FormatUtil.formatBytes(card.getTx()) + "/s"));
            }
        } else {
            System.out.println("未检测到网络接口");
        }
        System.out.println("======================================= 结束 =======================================");
    }

    /**
     * 格式化速度显示
     */
    private static String formatSpeed(long speedKB) {
        if (speedKB < 1024) {
            return speedKB + "KB";
        } else if (speedKB < 1024 * 1024) {
            return String.format("%.1fMB", speedKB / 1024.0);
        } else {
            return String.format("%.1fGB", speedKB / (1024.0 * 1024));
        }
    }

    /**
     * 清理缓存数据（如果需要重新开始采样）
     */
    public static void clearCache() {
        prevDiskStats.clear();
        prevNetStats.clear();
        prevCpuTicks = null;
    }

    public static boolean isMatch(String str, List<String> lists) {
        if (StringUtils.isEmpty(str) || CollectionUtils.isEmpty(lists)) {
            return Boolean.TRUE;
        }
        for (String list : lists) {
            if (str.contains(list)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

}
