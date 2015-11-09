package com.mage.sopxy;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class LocalTest
{
    public static void main(final String[] args) throws IOException
    {
        final ProxyController controller = new ProxyController();

        try (final ServerSocket ss = new ServerSocket(8080))
        {
            final Socket socket = ss.accept();
            controller.process(socket.getInputStream(), socket.getOutputStream());
        }
    }
}
