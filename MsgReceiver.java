import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;

public class MsgReceiver {
    //classe per ricezione dei messaggi
    private String multicastAddress;

    public MsgReceiver(String multicastAddress) {
        this.multicastAddress = multicastAddress;
    }

    public void run() {
        InetAddress ip= null;
        try {
            ip = InetAddress.getByName("225.255.255.255");
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
        DatagramPacket data=new DatagramPacket(inputByte, inputByte.length, ip, 30000);
        DatagramSocket ms = new DatagramSocket();
        ms.send(data);
        ms.close();
    }
        try {
            Selector selector = Selector.open();

            ServerSocketChannel serverSocket = ServerSocketChannel.open();
            serverSocket.bind(new InetSocketAddress("localhost", 55555));
            serverSocket.configureBlocking(false);
            serverSocket.register(selector, SelectionKey.OP_READ);
            System.out.println("MsgReceiver ready at port 55555" );
            while(true) {
                try {
                    selector.select();
                } catch (IOException e) {
                    System.out.println("seletore fallito");
                    break;
                }
                Set<SelectionKey> readyKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = readyKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key=iterator.next();
                    iterator.remove();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
