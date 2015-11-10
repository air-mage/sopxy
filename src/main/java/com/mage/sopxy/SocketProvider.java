package com.mage.sopxy;


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


    public static SocketProvider newProvider(int playerPort)
    {
        return new SocketProvider(playerPort);
    }


    private SocketProvider(int playerPort)
    {
        this.playerPort = playerPort;
    }


    public Socket awaitSocketAvailable(int timeoutSeconds) throws InternalException
    {
        logger.info("Waiting {} seconds for process to init socket on port {}", timeoutSeconds, playerPort);

        int i = 0;
        do
        {
            try
            {
                try (final Socket testSocket = new Socket(LOCALHOST, playerPort))
                {
                    final OutputStream os = testSocket.getOutputStream();
                    os.write(TEST_STRING.getBytes());
                    os.flush();

                    if (testSocket.getInputStream().read() != -1)
                    {
                        logger.debug("Socked responded");

                        try
                        {
                            return new Socket(LOCALHOST, playerPort);
                        }
                        catch (IOException e)
                        {
                            throw new InternalException("Unable to return local player socket", e);
                        }
                    }
                }
            }
            catch (final IOException ignore)
            {
                logger.debug("Socket not up yet");
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
        while (i++ < timeoutSeconds);

        logger.debug("Socket not available");

        throw new InternalException("Unable to wait for socket for given timeout");
    }

}
