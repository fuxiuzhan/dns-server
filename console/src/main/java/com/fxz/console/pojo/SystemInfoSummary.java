package com.fxz.console.pojo;

import lombok.Data;

import java.util.List;

@Data
public class SystemInfoSummary {
    /**
     * Linux,Windows,MacOs
     */
    private String os;
    /**
     * 启动时间
     */
    private String upTime;
    /**
     * CPU信息
     */
    private CpuInfo cpuInfo;
    /**
     * 内存信息
     */
    private MemoryInfo memoryInfo;
    /**
     * 磁盘信息
     */
    private DiskInfo diskInfo;
    /**
     * 网络信息
     */
    private NetInfo netInfo;

    @Data
    public static class CpuInfo {
        /**
         * cpu 型号
         */
        private String model;
        /**
         * cpu User
         */
        private long user;
        /**
         * cpu system
         */
        private long sys;
        /**
         * cpu io wait
         */
        private long io;
        /**
         * cpu 使用率
         */
        private long used;
        /**
         * cpu 温度
         */
        private double temp;
        /**
         * cpu 核心数
         */
        private int cores;
        /**
         * cpu 逻辑核心数
         */
        private int threads;
        /**
         * 1 分钟 load
         */
        private double load1min;
        /**
         * 5 分钟 load
         */
        private double load5min;
        /**
         * 15 分钟 load
         */
        private double load15min;
    }

    @Data
    public static class MemoryInfo {
        /**
         * 总内存数
         */
        private long size;
        /**
         * 已使用内存数
         */
        private long used;
        /**
         * 可用内存数
         */
        private long free;
    }

    @Data
    public static class DiskInfo {
        /**
         * 文件系统列表
         */
        private List<FileSystemInfo> fileSystems;
        /**
         * 磁盘列表
         */
        private List<Disk> disks;
    }

    @Data
    public static class FileSystemInfo {
        /**
         * 挂载点
         */
        private String mounted;
        /**
         * 大小
         */
        private long size;
        /**
         * 已使用
         */
        private long used;
        /**
         * 可用
         */
        private long free;
    }

    @Data
    public static class Disk {
        /**
         * 磁盘型号
         */
        private String model;
        /**
         * 大小
         */
        private long size;
        /**
         * 最近一秒中读取kb
         */
        private long read;
        /**
         * 最近一秒中写入kb
         */
        private long write;
        /**
         * 总读取
         */
        private long readTotal;
        /**
         * 总写入
         */
        private long writeTotal;
        /**
         * 挂载点
         */
        private String mounted;
    }

    @Data
    public static class NetInfo {
        /**
         * 网卡列表
         */
        private List<NetCard> cards;
        /**
         * 网卡总接收
         */
        private long totalRx;
        /**
         * 网卡总发送
         */
        private long totalTx;
    }

    @Data
    public static class NetCard {
        /**
         * 网卡名称
         */
        private String eth;
        /**
         * 网卡ipv4地址
         */
        private String ipv4;
        /**
         * 网卡最近一秒接收
         */
        private long rx;
        /**
         * 网卡最近一秒发送
         */
        private long tx;
    }
}