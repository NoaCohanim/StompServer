#pragma once
#include <string>
#include <vector>
using std::vector;
using std::string;
class ConnectionHandler;

// TODO: implement the STOMP protocol
class StompProtocol
{
private:
public:
    bool beforeHalf;
    StompProtocol();
    vector<string> createFrame(string rawMessage, ConnectionHandler& ch);
    void proccessFrame(string& Frame, ConnectionHandler& ch);
    std::vector<string> makeArray (string rawMessage, char deli);
};
