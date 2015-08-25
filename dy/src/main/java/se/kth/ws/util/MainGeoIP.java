package se.kth.ws.util;

import com.maxmind.geoip2.exception.GeoIp2Exception;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Created by babbarshaer on 2015-08-25.
 */
public class MainGeoIP {
    
    public static void main(String[] args) throws IOException, GeoIp2Exception {
        
        GeoIP geoIP = new GeoIP();
        geoIP.initializeDatabase("geoip_database.mmdb");
        
        InetAddress address = InetAddress.getByName("193.10.64.85");
        geoIP.isAllowed(address);
    }
}
