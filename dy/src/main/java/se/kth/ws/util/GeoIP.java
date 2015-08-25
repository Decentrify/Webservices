package se.kth.ws.util;

import com.maxmind.geoip.Country;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

/**
 * Main class for controlling which nodes can 
 * start communicating with the main system.
 *
 * Created by babbarshaer onw 2015-08-25.
 */
public class GeoIP {
    
    String dbLocation;
    DatabaseReader reader = null;
    
    public GeoIP(){
        
    }
    
    public void initializeDatabase(String loc) throws IOException {
        
        this.dbLocation = loc;
        File file = new File(dbLocation);
        this.reader = new DatabaseReader.Builder(file).build();

    }
    
    
    public boolean isAllowed(InetAddress address) throws IOException, GeoIp2Exception {
        
        CountryResponse response = reader.country(address);
        System.out.println(response.getCountry().getName());
        
        return false;
    }
    
}
