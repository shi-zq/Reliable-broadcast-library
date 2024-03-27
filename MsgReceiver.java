import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

public class MsgReceiver implements Runnable{
    private MsgSender msgSender;
    private String ip;
    private int port;
    private InetAddress broadcast;
    private DatagramSocket sendSocket;
    private ByteArrayOutputStream baos;
    private ObjectOutputStream oos;
    private SharedResource sharedResource;
    final int bufferSize = 2048; //1024 riservato per head 1024 riservato per body

    public MsgReceiver(MsgSender msgSender, String ip, int port, InetAddress broadcast) throws IOException {
        this.msgSender = msgSender;
        this.ip = ip;
        this.port = port;
        this.broadcast = broadcast;
        this.sendSocket = new DatagramSocket();
        sendSocket.setBroadcast(true);
        this.baos = new ByteArrayOutputStream();
        this.oos = new ObjectOutputStream(baos);
        this.sharedResource = new SharedResource();
    }

    public void run(){

        try {
            Selector selector = Selector.open();
            DatagramChannel serverSocket = DatagramChannel.open();
            serverSocket.bind(new InetSocketAddress(port));
            serverSocket.configureBlocking(false);
            serverSocket.register(selector, SelectionKey.OP_READ);
            System.out.println("server ready");
            while(true) {
                try {
                    selector.select();
                }
                catch (IOException e) {
                    break;
                }
                Set<SelectionKey> readyKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = readyKeys.iterator();
                while(iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    try {
                        if(key.isReadable()) {
                            DatagramChannel receive = (DatagramChannel)key.channel();
                            byte[] readBuffer = new byte[bufferSize];
                            ByteBuffer readData = ByteBuffer.wrap(readBuffer);
                            receive.receive(readData);
                            try {
                                ByteArrayInputStream bais = new ByteArrayInputStream(readData.array());
                                ObjectInputStream ois = new ObjectInputStream(bais);
                                ReliableMsg msg =  (ReliableMsg) ois.readObject();
                                msg.print();
                                switch(msg.getType()) {
                                    //加case+加handle
                                    case("JOIN"):
                                        handleJoin(msg);
                                        break;
                                    case("END"):
                                        handleEnd(msg);
                                        break;
                                    default:
                                        break;
                                }

                            }
                            catch(IOException | ClassNotFoundException ignored) {

                            }
                        }
                    }
                    catch(IOException ignored) {

                    }
                }

            }

        }
        catch(IOException ignored) {

        }
    }

    public void handleJoin(ReliableMsg msg) {
        if(!msg.getFrom().equals(ip)) {
            //only join from ip different, meaningless join msg from myself
            switch (sharedResource.getState()) {
                case("new"):
                    //ignored
                    break;
                case("joined"):
                    sharedResource.setChange();
                    break;
                case("change"):
                    //ignored
                    break;
            }
        }
    }

    public void handleEnd(ReliableMsg msg) {

    }

}
