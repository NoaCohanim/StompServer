#include "../include/ConnectionHandler.h"
#include <thread>

/*
Command Palette, enter and select C/C++ Build and debug active file: Select Project, and then select the correct project that you want to work with
*/

void socketListen(ConnectionHandler* ch, bool* userConnected) {
    while (ch->serverConnected) {
        string frame="";
        if (ch->getFrameAscii(frame, '\0') && frame.length()>0){
            ch->sp.proccessFrame(frame, *ch);
        }  
    }
    *userConnected = false;
}

vector<string> makeArray (string rawMessage, char deli) {
    vector<string> array = {};
    std::stringstream ss(rawMessage);
    string tmp;
    while(std::getline(ss, tmp, deli))
    {
        array.push_back(tmp);
    }
    return array;
}

int main(int argc, char *argv[]) {
    ConnectionHandler* ch;
    bool* userConnected;
    userConnected = new bool(false);
    std::thread t1;
    while(true){
        bool init = false;
        while (!init || *userConnected){
            //cin:
            const short bufsize = 1024;
            char buf[bufsize];
            std::cin.getline(buf, bufsize);
            std::string rawInput(buf);
            vector<string> input = makeArray(rawInput, ' ');
            //first login:
            if (input.at(0)=="login" && !(*userConnected)) {
                if (t1.joinable()) t1.join();
                string hostport = input.at(1);
                vector<string> host_port = makeArray(hostport,':');
                string host = host_port[0];
                int port = std::stoi(host_port[1]);
                ch = new ConnectionHandler(host,  port); //building connection handler and connecting to the server
                while (!ch->serverConnected) {
                    if (!ch->connect()) { //connect
                        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
                        return 1;
                    }
                    std::cerr << "Client connected to " << host << ":" << port << std::endl;
                    ch->serverConnected = true;
                    *userConnected = true;
                    init = true;
                }
                vector<string> frames = ch->sp.createFrame(rawInput, *ch);
                int numFrames = frames.size();
                for (int i = 0; i < numFrames; i++) {
                    ch->sendFrameAscii(frames.at(i), '\0');
                } 
                t1 = std::thread (socketListen, ch, userConnected);
            }
            //normal frames sending:
            else if (*userConnected && ch->userConnected && !ch->waitingForLove) {
                vector<string> frames = ch->sp.createFrame(rawInput, *ch);
                int numFrames = frames.size();
                for (int i = 0; i < numFrames; i++) {
                    if (ch->serverConnected)ch->sendFrameAscii(frames.at(i), '\0'); 
                }
            } 
            else if (input.at(0)!="login" && !(*userConnected)){
                std::cout <<("you must log in first")<<std::endl; //CONSIDER SENDING AN ERROR INSTEAD
             }
            }
            delete(ch);
        }
        t1.join();
        delete(userConnected);
    }
