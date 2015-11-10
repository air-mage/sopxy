package com.mage.sopxy;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProxyManager
{
    private static final int RESPONSE_BUFFER_SIZE = 65535;

    private static final int REQUEST_BUFFER_SIZE = 1024;

    private final static Logger logger = LoggerFactory.getLogger(ProxyManager.class);

    private final ExecutorService threads;

    private final CountDownLatch proxyLatch = new CountDownLatch(1);


    public static ProxyManager newProxy(final String channelId)
    {
        return new ProxyManager(channelId);
    }


    private ProxyManager(final String channelId)
    {
        threads = Executors.newFixedThreadPool(2,
                new ThreadFactoryBuilder().setNameFormat("sop-" + channelId + "-%d").setDaemon(true).build());
    }


    public ProxyManager proxyRequests(final InputStream clientInputStream,
            final OutputStream serverOutputStream)
    {
        logger.debug("Starting client to server communication proxy");
        threads.submit(new ProxyThread(REQUEST_BUFFER_SIZE, proxyLatch, clientInputStream, serverOutputStream));

        return this;
    }


    public ProxyManager proxyResponses(final InputStream serverInputStream,
            final OutputStream clientOutputStream)
    {
        logger.debug("Starting server to client communication proxy");
        threads.submit(new ProxyThread(RESPONSE_BUFFER_SIZE, proxyLatch, serverInputStream, clientOutputStream));

        return this;
    }


    public void forever()
    {
        logger.debug("Waiting till one of proxy threads dies");
        try
        {
            proxyLatch.await();
        }
        catch (InterruptedException e)
        {
            logger.info("Terminating proxy threads");

            threads.shutdownNow();
            
            while (!threads.isTerminated())
            {
                logger.debug("Waiting for threads to terminate");

                try
                {
                    Thread.sleep(100L);
                }
                catch (InterruptedException e1)
                {
                    logger.info("Unable to wait for proxy threads to die. Quitting");
                    break;
                }
            }
        }
    }
}
