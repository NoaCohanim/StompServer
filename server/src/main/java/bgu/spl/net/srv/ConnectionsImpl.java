package bgu.spl.net.srv;

import java.util.Map;

public class ConnectionsImpl<T> implements Connections<T> {

    Data<T> dataBase;

    public ConnectionsImpl(Data<T> data){
        this.dataBase = data;

    }


    public boolean send(int connectionId, T msg){
        if (dataBase.clients.get(connectionId) != null)
            dataBase.clients.get(connectionId).send(msg);
        return true;

    }

    public void send(String topic, T msg){
        Map<Integer,Integer> topicSubs = dataBase.topics.get(topic);
        for (Map.Entry<Integer,Integer> entry : topicSubs.entrySet()){
            if (dataBase.clients.get(entry.getKey()) != null)
                dataBase.clients.get(entry.getKey()).send(msg);
         }

    }

    public void disconnect(int connectionId){
        //try{dataBase.clients.get(connectionId).close();} catch(IOException x) {}
        dataBase.clients.remove(connectionId);

    }

    public ConnectionHandler<T> getCHbyClientID(Integer id){
        return dataBase.clients.get(id);
    }

}
