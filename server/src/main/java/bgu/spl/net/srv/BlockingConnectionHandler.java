package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    Data<T> dataBase;

    int connectionID;
    public String user;
    private final MessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;



    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, MessagingProtocol<T> protocol, Data<T> data) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
        this.dataBase = data;
        this.connectionID = ++data.connectionID;
        data.clients.put(this.connectionID,this);
        protocol.setID(this.connectionID);

    }

    @Override
    public void run() {
        try (Socket sock = this.sock) { //just for automatic closing
            int read;
            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());
            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                T nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    protocol.process(nextMessage);
                } 
            }

            // this.protocol.getConnections().disconnect(connectionID);
            // close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    @Override
    public void send(T msg) {
        if (msg != null) {
            try{
            out.write(encdec.encode(msg));
            out.flush();} catch (IOException ex) {}
        }
    }
    public String getUser(){
        return user;
    }

    public void setUser(String user){
        this.user = user;
    }

    public void setConnected(boolean con){
        this.connected = con;
    }

}
