package bgu.spl.net.srv;

//import jdk.internal.net.http.common.Pair;
//import javafx.util.Pair;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Data<T> {
    boolean reactor_or_TPC;

    Connections<T> connections;
    int messageID; //supplies a new message ID
    Integer connectionID; //supplies a new client ID
    Map<Integer, ConnectionHandler<T>> clients; //maps a client id to a CH
    Map<String,Map<Integer,Integer>> topics; //maps a topic name to  a map of topic subscriptions:<clientID, subID>
    Map<String, Object[]> users; //all users that existed with a pair:<password,isConnected>

    public Data(){
        this.connectionID = 0;
        this.messageID = 0;
        clients = new ConcurrentHashMap<>();
        topics = new ConcurrentHashMap<>();
        users = new ConcurrentHashMap<>();
    }

    public void setConnections(Connections<T> c){
        this.connections = c;
    }

}
