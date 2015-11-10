package com.mage.sopxy;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProxyManager
{
    private static final int RESPONSE_BUFFER_SIZE = 65535;

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


    public ProxyManager proxyResponses(final InputStream serverInputStream, final OutputStream clientOutputStream)
    {
        logger.debug("Starting server to client communication proxy thread");

        threads.submit(
                new ProxyThread("Responses", RESPONSE_BUFFER_SIZE, proxyLatch, serverInputStream, clientOutputStream));

        return this;
    }


    public void tillClientGone()
    {
        logger.debug("Waiting till proxy threads dies");
        try
        {
            proxyLatch.await();
        }
        catch (final InterruptedException e)
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
                catch (final InterruptedException e1)
                {
                    logger.info("Unable to wait for proxy threads to die. Quitting");
                    break;
                }
            }
        }
    }


    public ProxyManager sendHeaders(final Map<String, String> headers, final OutputStream serverOutputStream)
            throws InternalException
    {
        try
        {
            final Writer w = new OutputStreamWriter(serverOutputStream);

            logger.debug("Writing GET");

            w.write("GET /sopxy/149257 HTTP/1.1");
            w.flush();

            logger.debug("Writing headers");

            for (Entry<String, String> e : headers.entrySet())
            {
                w.write(e.getKey() + ": " + e.getValue());
                w.flush();
            }

            return this;
        }
        catch (IOException e1)
        {
            logger.error("Unable to send headers", e1);
            throw new InternalException("Unable to send headers", e1);
        }
    }
}
