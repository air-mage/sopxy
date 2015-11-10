package com.mage.sopxy;


import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SopcastSocketProvider
{
    private static final String LOCALHOST = "localhost";

    private final static Logger logger = LoggerFactory.getLogger(SopcastSocketProvider.class);

    private final static String TEST_STRING = "Range:bytes=0-";

    private final int playerPort;


    public static SopcastSocketProvider newProvider(int playerPort)
    {
        return new SopcastSocketProvider(playerPort);
    }


    private SopcastSocketProvider(int playerPort)
    {
        this.playerPort = playerPort;
    }


    public Optional<Socket> awaitSocketAvailable(int timeoutSeconds)
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

                        return Optional.of(new Socket(LOCALHOST, playerPort));
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
        while (i++ < timeoutSeconds);

        logger.debug("Socket not available");
        return Optional.empty();
    }

}
