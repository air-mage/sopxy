package com.mage.sopxy;


import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Host: qwakka.local:8080 User-Agent: VLC/2.2.1 LibVLC/2.2.1 Range: bytes=0- Connection: close Icy-MetaData: 1
 * 
 * @author air-mage
 *
 */
public class SopcastServiceLaunchProcess extends Thread
{
    private final static String TEST_STRING = "Range:bytes=0-";

    private final static Logger logger = LoggerFactory.getLogger(SopcastServiceLaunchProcess.class);

    private final static int LOCAL_PORT = 3908;

    private final static int PLAYER_PORT = 8902;

    private final String channelId;

    private final CountDownLatch latch;


    public SopcastServiceLaunchProcess(final String channelId, final CountDownLatch latch)
    {
        this.channelId = channelId;
        this.latch = latch;
    }


    @Override
    public void run()
    {
        logger.debug("Starting process");

        Process p = null;
        try
        {
            p = new ProcessBuilder("sp-sc-auth", "sop://broker.sopcast.com:3912/" + channelId,
                    String.valueOf(LOCAL_PORT), String.valueOf(PLAYER_PORT)).start();
            logger.debug("Started");

            while (true)
            {
                try
                {
                    try (final Socket testSocket = new Socket("localhost", PLAYER_PORT))
                    {
                        OutputStream os = testSocket.getOutputStream(); 
                        os.write(TEST_STRING.getBytes());
                        os.flush();

                        if (testSocket.getInputStream().read() != -1)
                        {
                            latch.countDown();

                            logger.debug("Socked responded");

                            break;
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
                catch (InterruptedException e)
                {
                    break;
                }
            }

            logger.debug("Waiting on process");

            while (!Thread.interrupted())
            {
                try
                {
                    Thread.sleep(100L);
                }
                catch (InterruptedException e)
                {
                    logger.info("Terminated");

                    break;
                }
            }
        }
        catch (final IOException e)
        {
            logger.error("Unable to start external process", e);

            return;
        }
        finally
        {
            logger.debug("Terminating external process");

            if (p != null)
            {
                try
                {
                    p.getInputStream().close();
                    p.getOutputStream().close();
                }
                catch (final IOException e)
                {
                    logger.error("Error closing IO streams", e);
                }

                p.destroy();
            }
        }

        logger.debug("Quitting thread");
    }
}
