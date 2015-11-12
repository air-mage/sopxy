package com.mage.sopxy.internal;


import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SocketProvider
{
    private static final String LOCALHOST = "localhost";

    private final static Logger logger = LoggerFactory.getLogger(SocketProvider.class);

    private final static String TEST_STRING = "Range:bytes=0-";

    private final int playerPort;


    public static SocketProvider newProvider(final int playerPort)
    {
        return new SocketProvider(playerPort);
    }


    private SocketProvider(final int playerPort)
    {
        this.playerPort = playerPort;
    }


    public Socket awaitSocketAvailable(final int timeoutSeconds) throws InternalException
    {
        logger.info("Waiting {} seconds for process to init socket on port {}", timeoutSeconds, playerPort);

        final Timer t = new Timer(timeoutSeconds * 1000);
        while (t.timeLeft())
        {
            try (final Socket testSocket = new Socket(LOCALHOST, playerPort))
            {
                final OutputStream os = testSocket.getOutputStream();
                os.write(TEST_STRING.getBytes());
                os.flush();

                if (testSocket.getInputStream().read() != -1)
                {
                    logger.trace("Socked responded");

                    return new Socket(LOCALHOST, playerPort);
                }
            }
            catch (final IOException ignore)
            {
                // ignore, no socket yet
            }

            try
            {
                Thread.sleep(100L);
            }
            catch (final InterruptedException e)
            {
                break;
            }
        }

        logger.debug("Socket not available");

        throw new InternalException("Unable to wait for socket for given timeout");
    }

}
