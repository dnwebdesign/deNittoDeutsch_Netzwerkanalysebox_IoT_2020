package com.deNittoDeutsch.IoT_App;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.telephony.CellInfoGsm;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.util.List;

import static android.content.ContentValues.TAG;

public class IoTappActivity extends Activity {

    private static final int REQUEST_PERMISSION_ACCESS_FINE_LOCATION
            = 123;

    private TextView tv;
    private LocationManager m;
    private LocationListener l;
    private String p;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        tv = (TextView) findViewById(R.id.textview);
        tv.setText("");
        // Berechtigungen prüfen
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]
                            {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION_ACCESS_FINE_LOCATION);
        } else {
            doIt();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        if ((requestCode == REQUEST_PERMISSION_ACCESS_FINE_LOCATION) &&
                (grantResults.length > 0 && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED)) {
            doIt();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            m.requestLocationUpdates(p, 45000, 0, l);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            m.removeUpdates(l);
        }
    }

    private void doIt() {

        String clientId = MqttClient.generateClientId();
        MqttAndroidClient client =
                new MqttAndroidClient(this.getApplicationContext(), "tcp://mqtt.eclipse.org:1883",
                        clientId);

        // LocationManager-Instanz ermitteln
        m = getSystemService(LocationManager.class);
        // Liste mit Namen aller Provider ausgeben
        List<String> providers = m.getAllProviders();
        for (String name : providers) {
            boolean enabled = m.isProviderEnabled(name);
            tv.append("Name: " + name +
                    " --- isProviderEnabled(): " +
                    enabled + "\n");
            if (!enabled) {
                continue;
            }
            LocationProvider lp = m.getProvider(name);
            tv.append("   requiresCell(): " +
                    lp.requiresCell() + "\n");
            tv.append("   requiresNetwork(): " +
                    lp.requiresNetwork() + "\n");
            tv.append("   requiresSatellite(): " +
                    lp.requiresSatellite() + "\n\n");
        }
        // Provider mit grober Auflösung
        // und niedrigen Energieverbrauch
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        p = m.getBestProvider(criteria, true);
        tv.append("\nVerwende " + p + "\n");
        // LocationListener-Objekt erzeugen
        l = new LocationListener() {
            @Override
            public void onStatusChanged(String provider, int status,
                                        Bundle extras) {
                tv.append("onStatusChanged()\n");
            }

            @Override
            public void onProviderEnabled(String provider) {
                tv.append("onProviderEnabled()\n");
            }

            @Override
            public void onProviderDisabled(String provider) {
                tv.append("onProviderDisabled()\n");
            }

            @Override
            public void onLocationChanged(Location location) {


                ConnectivityManager cmWifi = (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo wifiInfo = cmWifi.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                ConnectivityManager cmMobile = (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo mobileInfo = cmMobile.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);


                tv.append("\nonLocationChanged()\n");
                if (location != null) {
                    String s = "Breite: " + location.getLatitude()
                            + "\nLänge: " + location.getLongitude();
                    tv.append("CELLINFO GSM: " + wifiInfo + "\n");
                    tv.append("CELLINFO GSM: " + mobileInfo + "\n");
                    tv.append(s + "\n");

                    try {
                        IMqttToken token = client.connect();
                        token.setActionCallback(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                Log.d(TAG, "onSuccess");

                                // GPS Topics & Payload Strings & encodedPayloads bytes[]
                                String topicLaengengrad = "thkoeln/IoT/MobileApp/longitude";
                                String payloadLaengengrad = "" + location.getLongitude();
                                byte[] encodedPayloadLaengengrad = new byte[0];

                                String topicBreitengrad = "thkoeln/IoT/MobileApp/latitude";
                                String payloadBreitengrad = "" + location.getLatitude();
                                byte[] encodedPayloadBreitengrad = new byte[0];

                                // WIFI Topics & Payload Strings & encodedPayloads bytes[]
                                String topicWifi = "thkoeln/IoT./MobileApp/wifi";
                                String payloadWifi = "" + wifiInfo;
                                byte[] encodedPayloadWifi = new byte[0];

                                String topicWifiType = "thkoeln/IoT../MobileApp/wifiType";
                                String payloadWifiType = "" + wifiInfo.getTypeName();
                                byte[] encodedPayloadWifiType = new byte[0];

                                String topicWifiState = "thkoeln/IoT.../MobileApp/wifiState";
                                String payloadWifiState = "" + wifiInfo.getState();
                                byte[] encodedPayloadWifiState = new byte[0];

                                // Mobile Topics & Payload Strings & encodedPayloads bytes[]
                                String topicMobile = "thkoeln/IoT/MobileApp/Mobile";
                                String payloadMobile = "" + mobileInfo;
                                byte[] encodedPayloadMobile = new byte[0];

                                String topicMobileType = "thkoeln/IoT./MobileApp/MobileType";
                                String payloadMobileType = "" + mobileInfo.getSubtypeName();
                                byte[] encodedPayloadMobileType = new byte[0];

                                String topicMobileState = "thkoeln/IoT../MobileApp/MobileState";
                                String payloadMobileState = "" + mobileInfo.getState();
                                byte[] encodedPayloadMobileState = new byte[0];

                                try {

                                    // GPS Publish
                                    encodedPayloadLaengengrad = payloadLaengengrad.getBytes("UTF-8");
                                    MqttMessage messageLaengengrad = new MqttMessage(encodedPayloadLaengengrad);
                                    client.publish(topicLaengengrad, messageLaengengrad);

                                    encodedPayloadBreitengrad = payloadBreitengrad.getBytes("UTF-8");
                                    MqttMessage messageBreitengrad = new MqttMessage(encodedPayloadBreitengrad);
                                    client.publish(topicBreitengrad, messageBreitengrad);

                                    // WIFI Type Publish
                                    encodedPayloadWifiType = payloadWifiType.getBytes("UTF-8");
                                    MqttMessage messageWifiType = new MqttMessage(encodedPayloadWifiType);
                                    client.publish(topicWifiType, messageWifiType);

                                    // WIFI Type State
                                    encodedPayloadWifiState = payloadWifiState.getBytes("UTF-8");
                                    MqttMessage messageWifiState = new MqttMessage(encodedPayloadWifiState);
                                    client.publish(topicWifiState, messageWifiState);

                                    // WIFI Publish
                                    encodedPayloadWifi = payloadWifi.getBytes("UTF-8");
                                    MqttMessage messageWifi = new MqttMessage(encodedPayloadWifi);
                                    client.publish(topicWifi, messageWifi);

                                    // Mobile Type Publish
                                    encodedPayloadMobileType = payloadMobileType.getBytes("UTF-8");
                                    MqttMessage messageMobileType = new MqttMessage(encodedPayloadMobileType);
                                    client.publish(topicMobileType, messageMobileType);

                                    // Mobile State Publish
                                    encodedPayloadMobileState = payloadMobileState.getBytes("UTF-8");
                                    MqttMessage messageMobileState = new MqttMessage(encodedPayloadMobileState);
                                    client.publish(topicMobileState, messageMobileState);

                                    // Mobile Publish
                                    encodedPayloadMobile = payloadMobile.getBytes("UTF-8");
                                    MqttMessage messageMobile = new MqttMessage(encodedPayloadMobile);
                                    client.publish(topicMobile, messageMobile);

                                } catch (UnsupportedEncodingException | MqttException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                Log.d(TAG, "onFailure");

                            }


                        });
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }
}