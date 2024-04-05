package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
//import bgu.spl.net.impl.echo.EchoProtocol;
import bgu.spl.net.impl.echo.LineMessageEncoderDecoder;
import bgu.spl.net.srv.*;

import java.util.function.Supplier;

import static bgu.spl.net.srv.Server.threadPerClient;

public class StompServer {

    public static void main(String[] args) {
        // TODO: implement this

        boolean isTPC;
        Data<String> dataBase = new Data<String>();
        Connections<String> connections= new ConnectionsImpl<>(dataBase);
        dataBase.setConnections(connections);

        isTPC = "tpc".equals(args[1]);

        if (isTPC){
            Server.threadPerClient(
                    Integer.parseInt(args[0]), //port
                    () -> new StompMessageProtocolImpl(dataBase), //protocol factory
                    LineMessageEncoderDecoder::new, //message encoder decoder factory
                    dataBase
            ).serve();

        }
        else {
             Server.reactor(
                     Runtime.getRuntime().availableProcessors(),
                     Integer.parseInt(args[0]), //port
                     () -> new StompMessageProtocolImpl(dataBase), //protocol factory
                     LineMessageEncoderDecoder::new,//message encoder decoder factory
                     dataBase
             ).serve();

        }




    }
}
