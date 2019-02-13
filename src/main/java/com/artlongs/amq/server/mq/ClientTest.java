package com.artlongs.amq.server.mq;

import com.artlongs.amq.core.MqConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Scanner;

/**
 * Func :
 * Created by leeton on 2018/12/25.
 */
public class ClientTest {

    public static void main(String[] args) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(2048);
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            SocketChannel channel = SocketChannel.open();
            channel.connect(new InetSocketAddress(MqConfig.server_ip, MqConfig.port));
            channel.configureBlocking(false);

            String msg = scanner.nextLine();
            buf.put((new Date() + ":" + msg + "\r\n").getBytes());
            buf.flip();
            channel.write(buf);
            buf.clear();

        }




    }

}
