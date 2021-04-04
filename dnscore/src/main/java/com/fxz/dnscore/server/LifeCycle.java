package com.fxz.dnscore.server;

/**
 * @author xiuzhan.fu
 */
public interface LifeCycle {

    String name();

    void start() throws InterruptedException;

    void stop();

    void restart();
}
