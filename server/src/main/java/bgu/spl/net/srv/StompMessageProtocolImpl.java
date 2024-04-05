package bgu.spl.net.srv;

import bgu.spl.net.api.StompMessagingProtocol;
//import jdk.internal.net.http.common.Pair;
//import javafx.util.Pair;

import java.io.IOException;
import java.security.KeyStore.Entry;
import java.util.HashMap;
import java.util.Map;

public class StompMessageProtocolImpl implements StompMessagingProtocol<String> {

    Data<String> dataBase;
    Integer connectionId;
    boolean isError;

    public StompMessageProtocolImpl(Data<String> data){
        this.dataBase = data;
        connections = data.connections;

    }
    Connections<String> connections;
    public void start(int connectionId, Connections<String> connections) {
        this.connections = connections;
        this.connectionId = connectionId;
        isError = false;

    }


    public String process(String message) {
        //CONVERTS THE STRING INPUT FRAME INTO A MAP INPUT FRAME
        String[] input = message.split("\n");
        Map<String,String> inputFrame = new HashMap<>();
        String outputFrame = "";
        inputFrame.put("frameType",input[0]);
        if (inputFrame.get("frameType").equals("SEND")){
            String[] inputLine = input[1].split(":"); //destHeader
            inputFrame.put(inputLine[0],inputLine[1]);
            String messageBody = createString(input,2);
            inputFrame.put("messageBody", messageBody);
            //TODO: check for an error

        }
        else{
            for (int i=1; i<input.length; i++){
                String[] inputLine = input[i].split(":");
                //if (inputFrame.size() > 1)
                inputFrame.put(inputLine[0],inputLine[1]);
            }
        }

        int inputFrameSize = inputFrame.size();

        if (!checkFrame(inputFrame)){
            outputFrame = createError("invalid frame", "", -1);
            connections.send(inputFrameSize, outputFrame);
            removeConnection();
        }
        else{

            if (inputFrame.get("frameType").equals("CONNECT")) {
                boolean exists = false;
                String user = inputFrame.get("login");
                String passcode = inputFrame.get("passcode");
                for (Map.Entry<String, Object[]> entry : dataBase.users.entrySet()) {
                    if(entry.getKey().equals(user)){
                        exists=true;
                        if ((Boolean)entry.getValue()[1]) { //user logged in
                            Object[] pair = {entry.getValue()[0],false};
                            entry.setValue(pair);
                            outputFrame = createError("User already logged in",message,-1);
                            break;
                        } else if (!((String)entry.getValue()[0]).equals(passcode)) { //wrong pass
                            outputFrame += createError("Wrong passcode",message, -1);
                            break;
                        }
                        else { //successful connection and user exists
                            start(connectionId, this.connections);
                            Object[] pair = {entry.getValue()[0],true};
                            entry.setValue(pair);
                            outputFrame += "CONNECTED\n";
                            outputFrame += "version: 1.2\n";
                            outputFrame += '\u0000';
                            dataBase.clients.get(connectionId).setUser(user);
                            connections.send(connectionId,outputFrame);
                            break;
                        }
                    }
                }
                if (!exists) { //successful connection and user does'nt exist
                    start(connectionId, this.connections);
                    Object[] pair = {passcode,true};
                    dataBase.users.put(user, pair);
                    outputFrame += "CONNECTED\n";
                    outputFrame += "version: 1.2\n";
                    outputFrame += '\u0000';
                    //update ch user
                    dataBase.clients.get(connectionId).setUser(user);
                }
                connections.send(connectionId,outputFrame);

            }
            else if (inputFrame.get("frameType").equals("SEND")) {
                String topic = inputFrame.get("destination");
                if(dataBase.topics.get(topic) == null || dataBase.topics.get(topic).get(connectionId)==null){
                    outputFrame = createError("You are not subscribed to the channel", "",-1);
                    connections.send(connectionId,outputFrame);
                }
                else {
                    int subID = dataBase.topics.get(topic).get(connectionId);
                    outputFrame +="MESSAGE\n";
                    outputFrame+= "subscription:"+subID+"\n";
                    outputFrame+="message - id :"+(dataBase.messageID++)+"\n";
                    outputFrame+="destination :"+topic+"\n";
                    //outputFrame+="\n";
                    outputFrame+=inputFrame.get("messageBody")+"\n";
                    outputFrame+='\u0000';
                    connections.send(topic,outputFrame);

                }
                
            }

            else if (inputFrame.get("frameType").equals("SUBSCRIBE")) {

                //creates a receipt frame
                outputFrame+="RECEIPT\n";
                outputFrame+="receipt-id:";
                outputFrame+= (inputFrame.get("receipt")+"\n");
                outputFrame+='\u0000';
                String subID = inputFrame.get("id");
                String topic = inputFrame.get("destination");
                boolean topicFound = false;
                //if the topic exists, add the user to the topic map or send error if he's already subscribed
                for (Map.Entry<String,Map<Integer,Integer>> entry : dataBase.topics.entrySet()) {
                    if (entry.getKey().equals(topic)) {
                        topicFound = true;
                        if(entry.getValue().get(connectionId)  != null)
                            outputFrame = createError("user is already subscribed to the topic", message.substring(0, message.length()-2),Integer.parseInt(inputFrame.get("receipt")));
                        else
                            entry.getValue().put(connectionId,Integer.valueOf(subID));
                        break;
                    }
                }
                //if the topic is new, add it and add the user to the new topic
                if (!topicFound){
                    Map<Integer, Integer> newTopicMap = new HashMap<>();
                    newTopicMap.put(connectionId,Integer.valueOf(subID));
                    dataBase.topics.put(topic,newTopicMap);
                }
                connections.send(connectionId,outputFrame);
            }


            else if (inputFrame.get("frameType").equals("UNSUBSCRIBE")) {
                if (inputFrame.size() < 4 )

                //creates a receipt frame
                outputFrame+="RECEIPT\n";
                outputFrame+="receipt-id:";
                outputFrame+= (inputFrame.get("receipt")+"\n");
                outputFrame+='\u0000';
                String subID = inputFrame.get("id");

                //finds from which topic the client want to unsubscribe
                String topic ="";
                for (Map.Entry<String,Map<Integer,Integer>> topicMap : dataBase.topics.entrySet()) {
                    for (Map.Entry<Integer,Integer> sub : topicMap.getValue().entrySet()){
                        if (sub.getKey()==(connectionId) && sub.getValue()==Integer.parseInt(subID)){
                            topic = topicMap.getKey();
                            break;
                        }


                    }
                }
                 
                //finds the topic and removes the user from its subscriptions list
                boolean foundTopic = false;
                for (Map.Entry<String,Map<Integer,Integer>> entry : dataBase.topics.entrySet()) {
                    if (entry.getKey().equals(topic)) {
                    foundTopic = true;
                        if(entry.getValue().get(connectionId) == null)
                            outputFrame = createError("user is not subscribed to the topic so unsubscrive is illegal", message.substring(0, message.length()-2),Integer.parseInt(inputFrame.get("receipt")));
                        else
                            entry.getValue().remove(connectionId);
                        break;
                    }
                }
                if(!foundTopic)
                    outputFrame = createError("the topic is not found",  message.substring(0, message.length()-2),Integer.parseInt(inputFrame.get("receipt")));
                connections.send(connectionId,outputFrame);
            }

            else if (inputFrame.get("frameType").equals("DISCONNECT")) {

                isError = true;

                outputFrame += "RECEIPT\n";
                outputFrame+="receipt-id:";
                outputFrame+= (inputFrame.get("receipt")+"\n");
                outputFrame+='\u0000';

                //removes the user from all topics
                for (Map.Entry<String,Map<Integer,Integer>> entry : dataBase.topics.entrySet()) {
                    entry.getValue().remove(connectionId);
                }

                String user= dataBase.clients.get(connectionId).getUser();
                //sets the user to be disconnected in the users list
                Object[]  p= {dataBase.users.get(user)[0], false};
                dataBase.users.put(user, p);

                connections.send(connectionId,outputFrame);
                dataBase.clients.get(connectionId).setConnected(false);
                
            }
            if(inputFrame.get("frameType").equals("DISCONNECT") || isError){
                removeConnection();
            }
        }
    
        return outputFrame;
    }


    public boolean shouldTerminate(){
        return  isError;
    }

    public String createString(String[] arr,int from){
        String ans="";
        for (int i= from; i<arr.length-1; i++){
            ans += (arr[i]+"\n");
        }
        ans += arr[arr.length-1];
        return ans;
    }

    public Connections<String> getConnections(){
        return connections;

    }

    public void setID(int ID){
        this.connectionId = ID;
    }

    public String createError(String message, String originalMessage, int rec){
        if(dataBase.clients.get(connectionId) != null && dataBase.clients.get(connectionId).getUser() != null && dataBase.clients.get(connectionId).getUser().length() >=1)
            dataBase.users.get(dataBase.clients.get(connectionId).getUser())[1] = false;
        isError = true;
        String outputFrame="";
        outputFrame += "ERROR\n";
        if(rec >= 0){
            outputFrame += ("receipt-id:"+rec+"\n");
        }
        outputFrame += ("message: "+message+"\n");
        outputFrame += ("the message:\n");
        outputFrame += "-----\n";
        outputFrame += originalMessage;
        outputFrame += "-----\n";
        outputFrame += '\u0000';
        return outputFrame;
    }

    public void removeConnection(){
      //  try {
            //this.dataBase.clients.get(connectionId).close();
            this.dataBase.connections.disconnect(connectionId);
        //} catch (IOException ignored) {}
    }

    public boolean checkFrame(Map<String,String> inputFrame){
        if (inputFrame.get("frameType").equals("CONNECT")) {
            if(inputFrame.get("accept-version") == null || inputFrame.get("host") == null || inputFrame.get("login") == null || inputFrame.get("passcode") == null)
                return false;
        }
        else if(inputFrame.get("frameType").equals("SEND")){
            if(inputFrame.get("destination") == null)
            return false;
        }
        else if(inputFrame.get("frameType").equals("SUBSCRIBE")){
            if(inputFrame.get("receipt") == null || inputFrame.get("id") == null || inputFrame.get("destination") == null)
                return false;
        }
        else if(inputFrame.get("frameType").equals("UNSUBSCRIBE")){
            if(inputFrame.get("receipt") == null || inputFrame.get("id") == null)
                return false;
        }
        else if(inputFrame.get("frameType").equals("DISCONNECT")){
            if(inputFrame.get("receipt") == null)
                return false;
        }
        else{
            return false;
        }
        return true;
    }
}


