#include "../include/StompProtocol.h"
#include "../include/ConnectionHandler.h"
#include "../include/event.h"

StompProtocol::StompProtocol():beforeHalf(false) {
}

vector<string> StompProtocol::makeArray (string rawMessage, char deli) {
    vector<string> array = {};
    std::stringstream ss(rawMessage);
    string tmp;
    while(std::getline(ss, tmp, deli))
    {
        array.push_back(tmp);
    }
    return array;
}


vector<string> StompProtocol::createFrame(string rawMessage, ConnectionHandler& ch) {
    vector<string> input = makeArray(rawMessage, ' ');
    vector<string> out;
    string frame = "";
    if (input.at(0)=="login") { //creating connect frame

        if(!ch.userConnected) {
            frame.append("CONNECT\n");
            frame.append("accept-version:1.2\n");
            frame.append("host:stomp.cs.bgu.ac.il\n");
            frame.append("login:"+input.at(2)+"\n");
            frame.append("passcode:"+input.at(3)+"\n");
            frame.append("\0");
            ch.user  = input.at(2);
            out.push_back(frame);
            //ch.userConnected = true; will be set when connected 
        }
        else
            std::cout <<("The client is already logged in. log out before trying again")<<std::endl;

    }
    
    else if (input.at(0) == "join") {
        frame.append("SUBSCRIBE\n");
        frame.append("destination:/"+input.at(1)+"\n");
        frame.append("id:"+ std::to_string(ch.subsTopics.size()) +"\n");
        frame.append("receipt:"+std::to_string(++ch.receiptID)+"\n");
        frame.append("\0");
        out.push_back(frame);
        ch.pendingReceipts[std::to_string(ch.receiptID)] = input.at(1);

    }
    else if (input.at(0) == "exit") {
        frame.append("UNSUBSCRIBE\n");
        auto it = std::find(ch.subsTopics.begin(), ch.subsTopics.end(), input.at(1));
        int id = it - ch.subsTopics.begin();
        frame.append("id:" + std::to_string(id) + "\n");
        frame.append("receipt:"+std::to_string(++ch.receiptID)+"\n");
        frame.append("\0");
        out.push_back(frame);
        ch.pendingReceipts[std::to_string(ch.receiptID)] = input.at(1);

    }
    else if(input.at(0) == "report") {
        names_and_events eventsObject = parseEventsFile(input.at(1));

        string dest = "/"+(eventsObject.team_a_name+"_"+eventsObject.team_b_name);
    
        
        vector<Event> eventsVector = eventsObject.events;
        int numOfEvents = eventsVector.size();
        for(int i=0; i<numOfEvents; i++){
            auto event = eventsVector.at(i);
            frame.append("SEND\n");
            frame.append("destination:"+dest+"\n\n");
            string message;
            message.append("user:"+ch.user+"\n");
            message.append("team a:"+eventsObject.team_a_name+"\n");
            message.append("team b:"+eventsObject.team_b_name+"\n");
            message.append("event name:"+event.get_name()+"\n");
            message.append("time:"+std::to_string(event.get_time())+"\n");
            message.append("general game updates:\n");
            std::map<std::string, std::string> game_updates = event.get_game_updates();
            for(std::map<string,string>::iterator it = game_updates.begin(); it != game_updates.end(); ++it) {
                message.append(it->first+":"+it->second+"\n");
            }
            message+= "team a updates: \n";
            std::map<std::string, std::string> team_a_updates = event.get_team_a_updates();
            for(std::map<string,string>::iterator it = team_a_updates.begin(); it != team_a_updates.end(); ++it) {
                message.append(it->first+":"+it->second+"\n");
            }
            message+= "team b updates: \n";
            std::map<std::string, std::string> team_b_updates = event.get_team_b_updates();
            for(std::map<string,string>::iterator it = team_b_updates.begin(); it != team_b_updates.end(); ++it) {
                message.append(it->first+":"+it->second+"\n");
            }
            message+= "description: \n" + event.get_discription();
            frame.append(message);
            frame.append("\0");
            out.push_back(frame);
            frame ="";
        }
    }
    else if(input.at(0) == "summary") {
        //saves all the game updates the client got from user (input[2]) into a file(input[3]) of the game (input[1])
        bool found = false;
        for(std::map<string, Summary>::iterator it = ch.userSummary.begin(); it != ch.userSummary.end(); ++it) {
            if(it->first == input.at(2)+"_"+"/"+input.at(1))
                found = true;
            }

        if (!found)
            std::cout << "the user-topic does not exist"  << std::endl;
        else
            ch.userSummary[input.at(2)+"_"+"/"+input.at(1)].getSummary(input.at(3));
    }
    else if(input.at(0) == "logout") {
        ch.waitingForLove = true;
        frame.append("DISCONNECT\n");
        frame.append("receipt:"+std::to_string(++ch.receiptID)+"\n");
        frame.append("\0");
        out.push_back(frame);
        ch.pendingReceipts[std::to_string(ch.receiptID)] = "logout";
    }

    return out;
}

void StompProtocol::proccessFrame(string& frame, ConnectionHandler& ch) {
    std::cout << frame << std::endl;
    vector<string> input = makeArray(frame, '\n');
    if (input.at(0) == "CONNECTED"){
        ch.userConnected  = true ;
        std::cout << "Login successful" <<std::endl;
    }
        /*********************************************************************/
    else if (input.at(0) == "MESSAGE"){
        vector<string> users = makeArray(input.at(5), ':');
        vector<string> topic_ = makeArray(input.at(3), ':');
        std::string userTopic = users[1]+"_"+topic_[1];
        auto it = ch.userSummary.find(userTopic);
        if (it==ch.userSummary.end()){
            ch.userSummary.insert({userTopic,Summary()});
        }
        string time;
        string event;
        string currStats = "pre";
        vector<string> teams_ = makeArray(input.at(3), '/');
        vector<string> teams = makeArray(teams_.at(1), '_');
        ch.userSummary[userTopic].teamA = teams.at(0);
        ch.userSummary[userTopic].teamB = teams.at(1);
        int len = input.size();
        for (int i = 5; i <len; i++){
            string currLine = input.at(i);
            vector<string> line = makeArray(currLine, ':');
            if (currStats == "pre"){
                if (line.at(0) == "time"){
                    time = line.at(1);
                    event += (time+" - ");
                    string preLine = input.at(i-1);
                    vector<string> preLineArray = makeArray(preLine, ':');
                    event += (preLineArray.at(1)+":\n\n"); //adds the event name to the event description
                }
                else if(line.at(0) == "general game updates")
                    currStats = "general";
            }
            else if (currStats == "general"){
                if(line.at(0) == "team a updates")
                    currStats ="a";
                else{
                    ch.userSummary[userTopic].generalStats[line.at(0)] = line.at(1);
                    if (line.at(0) == "before halftime")
                        beforeHalf =  (line.at(1) == " true");
                }
            }
            else if (currStats == "a"){
                if(line.at(0) == "team b updates")
                    currStats ="b";
                else
                    ch.userSummary[userTopic].teamAStats[line.at(0)] = line.at(1);
            }

            else if (currStats == "b"){
                if(line.at(0) == "description")
                    currStats ="desc";
                else
                    ch.userSummary[userTopic].teamBStats[line.at(0)] = line.at(1);

            }
            else if (currStats == "desc"){
                if(line.at(0) == "\0")
                    currStats = "processOver";
                else{
                    event += currLine+"\n";
                    currStats = "processOver";

                }
                    
            }
            if(beforeHalf)
                ch.userSummary[userTopic].eventBeforeHalftime[time]=event;
            else
                ch.userSummary[userTopic].eventAfterHalftime[time]=event;
        }
    }
        /*********************************************************************/
    else if (input.at(0) == "RECEIPT"){

        string currRec= input.at(1).substr(11,input.at(1).length());
        if (ch.pendingReceipts[currRec] == "logout"){
            ch.logoutUser(); //user logs out and close the socket
        }
        else{
            string topic = ch.pendingReceipts.at(currRec);
            auto it = std::find(ch.subsTopics.begin(), ch.subsTopics.end(), topic);
            bool subscribed = (it != ch.subsTopics.end());
            if(subscribed){ //needs to unsubscribe
                int topicID = it - ch.subsTopics.begin();
                ch.subsTopics.at(topicID) = "exTopic";
                std::cout<<("Exited channel "+topic)<<std::endl;
            }
            else{
                ch.subsTopics.push_back(topic);
                 std::cout<<("Joined channel "+topic)<<std::endl;          
            }
        }
    }
        /*********************************************************************/
    else if (input.at(0) == "ERROR"){
        std::cout<<frame<<std::endl; 
        ch.logoutUser();
    }
}

