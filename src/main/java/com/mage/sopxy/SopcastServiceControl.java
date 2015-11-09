package com.mage.sopxy;


import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.AbstractMap;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SopcastServiceControl
{
    private static final int SOCKET_WAIT = 10;

    private final static Logger logger = LoggerFactory.getLogger(SopcastServiceControl.class);

    private final static String TEST_STRING = "Range:bytes=0-";

    private final static String SERVICE = "sp-sc-auth";

    private final static String PARAM_URL = "sop://broker.sopcast.com:3912/";

    private final String channelId;

    private Process process;


    public SopcastServiceControl(final String channelId)
    {
        super();
        this.channelId = channelId;
    }


    public Socket start()
    {
        final Entry<Integer, Integer> ports = findPorts();
        if (ports == null)
        {
            return null;
        }

        try
        {
            spawnProcess(channelId, ports);
        }
        catch (IOException e)
        {
            logger.error("Unable to spawn {} process", SERVICE, e);
            return null;
        }

        final boolean socketUp = testStreaming(SOCKET_WAIT, ports.getValue());
        if (socketUp)
        {
            try
            {
                return new Socket("localhost", ports.getValue());
            }
            catch (IOException e)
            {
                logger.error("Unable to create socket for service process");
                return null;
            }
        }

        logger.error("Unable to create socket for service process");
        return null;
    }


    private void spawnProcess(final String channelId, final Entry<Integer, Integer> ports) throws IOException
    {
        logger.info("Spawning process: {}", SERVICE);
        logger.debug("Lauching: {} {}{} {} {}", SERVICE, PARAM_URL, channelId, ports.getKey(), ports.getValue());

        process = new ProcessBuilder(SERVICE, PARAM_URL + channelId, String.valueOf(ports.getKey()),
                String.valueOf(ports.getValue())).start();
    }


    private boolean testStreaming(final int seconds, final int playerPort)
    {
        logger.info("Waiting for process to init socket");

        int i = 0;
        do
        {
            try
            {
                try (final Socket testSocket = new Socket("localhost", playerPort))
                {
                    final OutputStream os = testSocket.getOutputStream();
                    os.write(TEST_STRING.getBytes());
                    os.flush();

                    if (testSocket.getInputStream().read() != -1)
                    {
                        logger.debug("Socked responded");

                        return true;
                    }
                }
            }
            catch (final IOException ignore)
            {
                logger.debug("Socket not up yet", ignore);
            }

            try
            {
                Thread.sleep(1000L);

                logger.debug("Retrying");
            }
            catch (final InterruptedException e)
            {
                break;
            }
        }
        while (i++ < seconds);

        return false;
    }


    private Entry<Integer, Integer> findPorts()
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
            logger.warn("Unable to find two local ports", e);

            return null;
        }

        logger.debug("Found ports to use: local={}; player={}", localPort, playerPort);

        return new AbstractMap.SimpleEntry<Integer, Integer>(localPort, playerPort);
    }


    public void stop()
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

        logger.debug("Process quit with code: {}", process.exitValue());
    }
}
