package com.mage.sopxy;


import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProxyServlet extends HttpServlet implements ServletContextListener
{
    private static final long serialVersionUID = 1L;

    private final static Logger logger = LoggerFactory.getLogger(ProxyServlet.class);


    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException
    {
        try
        {
            final String channelId = extractChannelId(request);
            final Map<String, String> headers = extractHeaders(request);

            process(channelId, response.getOutputStream(), headers);
        }
        catch (InternalException e)
        {
            logger.error("Error processing request", e);

            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    private Map<String, String> extractHeaders(HttpServletRequest request)
    {
        logger.debug("Parsing headers");

        final Enumeration<String> names = request.getHeaderNames();
        final Map<String, String> headers = new LinkedHashMap<>();

        while (names.hasMoreElements())
        {
            final String name = names.nextElement();
            final String content = request.getHeader(name);

            logger.debug("{}: {}", name, content);

            headers.put(name, content);
        }

        return headers;
    }


    private String extractChannelId(final HttpServletRequest request)
    {
        final String pathInfo = request.getPathInfo();

        logger.debug("Extractin channel id from: {}", pathInfo);

        final String[] path = pathInfo.split("/");
        final String channelId = path[path.length - 1];

        logger.debug("Channel id extracted: {}", channelId);

        return channelId;
    }


    protected void process(final String channelId, OutputStream clientOutputStream, Map<String, String> headers)
            throws InternalException
    {
        try (final ServiceWrapper service = ServiceWrapper.start(channelId))
        {
            final int port = service.getPort();
            try (final Socket server = SocketProvider.newProvider(port).awaitSocketAvailable(10))
            {
                ProxyManager.newProxy(channelId) //
                        .proxyResponses(server.getInputStream(), clientOutputStream) //
                        .sendHeaders(headers, server.getOutputStream()) //
                        .tillClientGone();
            }
            catch (IOException e)
            {
                throw new InternalException("Error processing server socket", e);
            }
        }
    }


    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        logger.info("Starting application");
    }


    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {
        logger.info("Shutting down application");
    }
}
