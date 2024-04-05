#pragma once

#include <string>
#include <iostream>
#include <boost/asio.hpp>
#include <map>
using std::string;
class Summary{
public:
    //fields:
    string topic;
    int userID;
    string teamA;
    string teamB;
    std::map<string,string> generalStats;
    std::map<string,string> teamAStats;
    std::map<string,string> teamBStats;
    std::map<string,string> eventBeforeHalftime;
    std::map<string,string> eventAfterHalftime;
    //getters:
    void getSummary(string path);


    Summary();
    string mapToString(std::map<string, string> &m);
    string mapToStringValuesOnly(std::map<string, string> &m);

    };

