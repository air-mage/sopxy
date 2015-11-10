package com.mage.sopxy;


import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.AbstractMap;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServiceWrapper implements Closeable
{
    private final static Logger logger = LoggerFactory.getLogger(ServiceWrapper.class);

    private final static String SERVICE = "sp-sc-auth";

    private final static String PARAM_URL = "sop://broker.sopcast.com:3912/";

    private final String channelId;

    private int port;

    private Process process;


    public static ServiceWrapper start(final String channelId) throws InternalException
    {
        final ServiceWrapper service = new ServiceWrapper(channelId);
        service.start();

        return service;
    }


    private ServiceWrapper(final String channelId)
    {
        super();
        this.channelId = channelId;
    }


    private void start() throws InternalException
    {
        final Entry<Integer, Integer> ports = findPorts();

        logger.info("Launching: {} {}{} {} {}", SERVICE, PARAM_URL, channelId, ports.getKey(), ports.getValue());

        try
        {
            process = new ProcessBuilder(SERVICE, PARAM_URL + channelId, String.valueOf(ports.getKey()),
                    String.valueOf(ports.getValue())).start();
        }
        catch (IOException e)
        {
            throw new InternalException("Unable to spawn nested process", e);
        }

        port = ports.getValue();
    }


    private Entry<Integer, Integer> findPorts() throws InternalException
    {
        final Integer localPort;
        final Integer playerPort;

        logger.debug("Seeking for free ports");

        try (final ServerSocket localSocket = new ServerSocket(0))
        {
            localPort = localSocket.getLocalPort();
            try (final ServerSocket playerSocket = new ServerSocket(0))
            {
                playerPort = playerSocket.getLocalPort();
            }
        }
        catch (final IOException e)
        {
            throw new InternalException("Unable to find two local ports", e);
        }

        logger.debug("Found ports to use: local={}; player={}", localPort, playerPort);

        return new AbstractMap.SimpleEntry<Integer, Integer>(localPort, playerPort);
    }


    public int getPort()
    {
        return port;
    }


    @Override
    public void close()
    {
        logger.info("Destroying process");

        if (process == null)
        {
            logger.info("Process probably was not started");

            return;
        }

        if (process.isAlive())
        {
            process.destroy();
        }

        while(process.isAlive())
        {
            
        }
        
        logger.debug("Process quit with code: {}", process.exitValue());
    }
}
