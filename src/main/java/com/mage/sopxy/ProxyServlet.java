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

import com.mage.sopxy.internal.InternalException;
import com.mage.sopxy.internal.SocketProvider;
import com.mage.sopxy.proxy.ProxyManager;
import com.mage.sopxy.service.ServiceManager;
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


    private String extractChannelId(final HttpServletRequest request) throws InternalException
    {
        final String pathInfo = request.getPathInfo();

        logger.debug("Extractin channel id from: {}", pathInfo);

        try
        {
            final String[] path = pathInfo.split("/");
            final String channelId = path[path.length - 1];

            logger.debug("Channel id extracted: {}", channelId);

            return channelId;
        }
        catch (Exception e)
        {
            logger.error("Unable to extract channel id", e);
            throw new InternalException("Unable to extract channel id");
        }
    }


    protected void process(final String channelId, OutputStream clientOutputStream, Map<String, String> headers)
            throws InternalException
    {
        final int servicePort = ServiceManager.getInstance().startService(channelId);
        
        try (final Socket server = SocketProvider.newProvider(servicePort).awaitSocketAvailable(10))
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
        finally
        {
            ServiceManager.getInstance().stopService(channelId);
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
