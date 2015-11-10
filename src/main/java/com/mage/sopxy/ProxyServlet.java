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


public class ProxyServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    private final static Logger logger = LoggerFactory.getLogger(ProxyServlet.class);


    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException
    {
        try
        {
            process(request.getInputStream(), response.getOutputStream());
        }
        catch (InternalException e)
        {
            logger.error("Error processing request", e);

            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    protected void process(InputStream clientInputStream, OutputStream clientOutputStream) throws InternalException
    {
        final String channelId = readChannelId(clientInputStream);

        try (final ServiceWrapper service = ServiceWrapper.start(channelId))
        {
            final int port = service.getPort();
            try (final Socket server = SocketProvider.newProvider(port).awaitSocketAvailable(10))
            {
                ProxyManager.newProxy(channelId) //
                        .proxyRequests(clientInputStream, server.getOutputStream()) //
                        .proxyResponses(server.getInputStream(), clientOutputStream) //
                        .forever();
            }
            catch (IOException e)
            {
                throw new InternalException("Error processing server socket", e);
            }
        }
    }


    private String readChannelId(InputStream clientInputStream) throws InternalException
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

            return channel;
        }
        catch (final IOException e)
        {
            logger.warn("Unable to extract channel from path", e);

            throw new InternalException("Unable to extract channel from path", e);
        }
    }
}
