package com.mage.sopxy;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProxyController extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    private final static Logger logger = LoggerFactory.getLogger(ProxyController.class);


    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException
    {
        process(request.getInputStream(), response.getOutputStream());
    }


    protected void process(InputStream clientInputStream, OutputStream clientOutputStream)
    {
        final Either<String, String> channelId = readChannelId(clientInputStream);
        if (channelId.isRight())
        {
            sendError(clientOutputStream, channelId.getRight());
            return;
        }

        try(final SopcastService service = SopcastService.start(channelId.getLeft()))
        {
            try (final Socket server = SopcastSocketProvider.newProvider(service.getPort()).awaitSocketAvailable(10))
            {
                if (server != null)
                {
                    SopcastProxyController.newProxy(channelId) //
                            .proxyRequests(clientInputStream, server.getOutputStream()) //
                            .proxyResponses(server.getInputStream(), clientOutputStream) //
                            .forever();
                }
            }
            catch (final IOException e)
            {
                logger.error("IOError", e);
            }
        }
    }


    private Either<String, String> readChannelId(InputStream clientInputStream)
    {
        try
        {
            final BufferedReader br = new BufferedReader(new InputStreamReader(clientInputStream));
            final String params = br.readLine();
            final String[] requestParam = params.split(" ");
            final String path = requestParam[1];
            final String[] pathParts = path.split("/");
            final String channel = pathParts[pathParts.length - 1];

            logger.debug("Proxying for path: {}", channel);

            return Either.left(channel);
        }
        catch (final IOException e)
        {
            logger.warn("Unable to extract channel from path", e);

            return Either.right("Unable to find channel in request");
        }
    }


    private void sendError(OutputStream output, String message)
    {
        logger.info("Responding with error: {}", message);

        try
        {
            output.write(message.getBytes());
            output.flush();
        }
        catch (IOException e)
        {
            // ignore
            logger.error("Error writing error response", e);
        }
    }
}
