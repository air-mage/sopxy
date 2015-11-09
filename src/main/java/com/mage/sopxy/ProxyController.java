package com.mage.sopxy;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProxyController
{
    private final static Logger logger = LoggerFactory.getLogger(ProxyController.class);

    private final ExecutorService threads = Executors.newFixedThreadPool(2);


    public void sopcastProxy(final InputStream streamFromClient, final OutputStream streamToClient,
            final String channelId) throws InterruptedException
    {
        logger.info("Starting for " + channelId);

        final CountDownLatch latch = new CountDownLatch(1);

        threads.execute(new SopcastServiceLaunchProcess(channelId, latch));
        try
        {
            logger.info("Waiting for service to start");

            latch.await();

            logger.info("Start writing stream");

            connectToSopcastService(streamFromClient, streamToClient);

            logger.info("Cancelling");
        }
        finally
        {
            threads.shutdownNow();
            while (threads.isTerminated())
            {
                logger.debug("Waiting for threads to terminate");
                Thread.sleep(100L);
            }
        }

        logger.info("Exiting");
    }


    private void connectToSopcastService(final InputStream streamFromClient, final OutputStream streamToClient)
    {
        logger.debug("Connecting to sopcast server");

        try (final Socket server = new Socket("localhost", 8902))
        {
            final InputStream streamFromServer = server.getInputStream();
            final OutputStream streamToServer = server.getOutputStream();

            startClientToServerThread(streamFromClient, streamToServer);
            startProxying(streamToClient, streamFromServer);
        }
        catch (final IOException e)
        {
            logger.error("Unable to connect to local sopcast service", e);
        }
    }


    private void startProxying(final OutputStream streamToClient, final InputStream streamFromServer)
    {
        logger.debug("Start server to client responses proxying");

        final byte[] replyBuffer = new byte[4096];
        int bytesRead;
        try
        {
            while ((bytesRead = streamFromServer.read(replyBuffer)) != -1)
            {
                streamToClient.write(replyBuffer, 0, bytesRead);
                streamToClient.flush();
            }
        }
        catch (final IOException e)
        {
            logger.error("Server to client transfer error", e);
        }

        logger.debug("Stopped server to client responses proxying");
    }


    private void startClientToServerThread(final InputStream streamFromClient, final OutputStream streamToServer)
    {
        logger.debug("Starting client to server command proxy thread");

        final byte[] requestBuffer = new byte[1024];
        final Thread clientToServerThread = new Thread()
        {
            @Override
            public void run()
            {
                int bytesRead;
                try
                {
                    while ((bytesRead = streamFromClient.read(requestBuffer)) != -1)
                    {
                        streamToServer.write(requestBuffer, 0, bytesRead);
                        streamToServer.flush();
                    }
                }
                catch (final IOException e)
                {
                    logger.error("Client to server transfer error", e);
                }

                logger.debug("Stopped client to server command proxy thread");
            }
        };

        threads.execute(clientToServerThread);
    }


    public static void main(final String[] args) throws InterruptedException
    {
        final ProxyController controller = new ProxyController();

        try (final ServerSocket ss = new ServerSocket(8080))
        {
            final Socket client = ss.accept();

            final InputStream sis = client.getInputStream();
            final BufferedReader br = new BufferedReader(new InputStreamReader(sis));
            final String request = br.readLine();
            final String[] requestParam = request.split(" ");
            final String path = requestParam[1];
            final String[] pathParts = path.split("/");
            final String channel = pathParts[pathParts.length - 1];

            logger.debug("Proxying for path: {}", channel);

            controller.sopcastProxy(sis, client.getOutputStream(), channel);
        }
        catch (final IOException e)
        {
            e.printStackTrace();
        }
    }
}
