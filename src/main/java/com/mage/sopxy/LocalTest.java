package com.mage.sopxy;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class LocalTest
{
    public static void main(final String[] args) throws IOException
    {
        while (true)
        {
            final ProxyServlet controller = new ProxyServlet();

            try (final ServerSocket ss = new ServerSocket(8080))
            {
                try (final Socket socket = ss.accept())
                {
                    try
                    {
                        controller.process(socket.getInputStream(), socket.getOutputStream());
                    }
                    catch (InternalException e)
                    {
                        socket.getOutputStream().write(e.getMessage().getBytes());
                        socket.getOutputStream().flush();
                    }
                }
            }
        }
    }
}
