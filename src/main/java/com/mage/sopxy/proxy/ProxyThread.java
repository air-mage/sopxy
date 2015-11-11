package com.mage.sopxy.proxy;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class ProxyThread extends Thread
{
    private final static Logger logger = LoggerFactory.getLogger(ProxyThread.class);

    private final String name;

    private final CountDownLatch latch;

    private final InputStream is;

    private final OutputStream os;

    private final byte[] buffer;


    public ProxyThread(final String name, final int bufferSize, final CountDownLatch latch, final InputStream is,
            final OutputStream os)
    {
        super();
        this.name = name;
        this.buffer = new byte[bufferSize];
        this.latch = latch;
        this.is = is;
        this.os = os;
    }


    @Override
    public void run()
    {
        logger.debug("{} starting", name);
        int bytesRead;
        try
        {
            while (!Thread.currentThread().isInterrupted() && ((bytesRead = is.read(buffer)) != -1))
            {
                os.write(buffer, 0, bytesRead);
                os.flush();
            }
        }
        catch (final IOException e)
        {
            logger.error("Proxy stopped", e);
        }
        finally
        {
            latch.countDown();
        }

        logger.debug("{} shut down", name);
    }
}