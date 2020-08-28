#include <WiFi.h>

// WLAN Daten 
const char *ssid = "R2DLan";                        //Name des Netzwerks
const char *pass = "49761671295406304116";          //Passwort des Netzwerks


//Initales verbinden mit WLAN
void setupWifi()
{
  Serial.print("\nConnecting to ");
  Serial.println(ssid);
 
  WiFi.begin(ssid, pass);
 
  while (WiFi.status() != WL_CONNECTED)
  {
    delay(100);
    Serial.println(WiFi.status());
  }
 
  Serial.print("\nConnected to");
  Serial.println(ssid);
}