package com.mage.sopxy.service;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.mage.sopxy.internal.InternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServiceManager
{
    private final static Logger logger = LoggerFactory.getLogger(ServiceManager.class);

    private final static int SERVICE_KILL_TIMEOUT = 10;

    private final static ServiceManager _instance = new ServiceManager();

    private final Map<String, ServiceInfo> services = new HashMap<>();


    public static class ServiceInfo
    {
        private final ServiceWrapper service;

        private final AtomicInteger consumers = new AtomicInteger(0);


        public ServiceInfo(final ServiceWrapper service)
        {
            this.service = service;
        }
    }


    public static ServiceManager getInstance()
    {
        return _instance;
    }


    private ServiceManager()
    {

    }


    public synchronized int startService(final String channelId) throws InternalException
    {
        logger.info("Starting service for {}", channelId);

        ServiceInfo si = services.get(channelId);
        if (si == null)
        {
            logger.debug("Creating new service");
            si = new ServiceInfo(ServiceWrapper.start(channelId));
            services.put(channelId, si);
        }
        else
        {
            logger.debug("Service already running");
        }

        si.consumers.incrementAndGet();

        logger.debug("Returning service on port {}", si.service.getPort());

        return si.service.getPort();
    }


    public synchronized void stopService(final String channelId)
    {
        logger.info("Stopping service for {}", channelId);

        final ServiceInfo si = services.get(channelId);
        if (si == null)
        {
            logger.warn("Service not found");
            return;
        }

        final int consumers = si.consumers.decrementAndGet();

        logger.debug("Service consumers {}", consumers);

        if (consumers == 0)
        {
            logger.debug("Last consumer gone, killing process");

            services.remove(channelId);

            si.service.stop(SERVICE_KILL_TIMEOUT);
        }
    }
}
