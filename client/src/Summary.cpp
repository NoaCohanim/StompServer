#include "../include/Summary.h"
#include <fstream>

Summary::Summary(): topic(""),userID(-1),teamA(""),teamB(""),generalStats(std::map<string,string>{}),teamAStats(std::map<string,string>{}),teamBStats(std::map<string,string>{}),eventBeforeHalftime(std::map<string,string>{}),eventAfterHalftime(std::map<string,string>{}){}
string Summary::mapToString(std::map<string, string> &m){
    string ans = "";
    for (auto it = m.cbegin(); it != m.cend(); ++it) {
        ans += ((*it).first + ": " + (*it).second + "\n");
        }
    return ans;
}

string Summary::mapToStringValuesOnly(std::map<string, string> &m){
    string ans = "";
    for (auto it = m.cbegin(); it != m.cend(); ++it) {
        ans += ((*it).second + "\n");
        }
    return ans;
}

void Summary::getSummary(string path){
    string sum= teamA+" vs "+teamB+"\n";
    sum += "Game stats:\n";
    sum += "General Stats:\n";
    sum += mapToString(generalStats);
    sum += (teamA+" stats:\n");
    sum += mapToString(teamAStats);
    sum += (teamB+" stats:\n");
    sum += mapToString(teamBStats);
    sum += "Game event reports:";
    sum += mapToStringValuesOnly(eventBeforeHalftime);
    sum += mapToStringValuesOnly(eventAfterHalftime);

    std::ofstream file(path);
    file << sum;
    file.close();


}