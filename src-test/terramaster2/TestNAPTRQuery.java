package terramaster2;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.flightgear.terramaster.dns.FlightgearNAPTRQuery;
import org.flightgear.terramaster.dns.WeightedUrl;
import org.junit.Test;

public class TestNAPTRQuery {

  @Test
  public void testGet() {
    FlightgearNAPTRQuery q = new FlightgearNAPTRQuery();
    List<WeightedUrl> server = q.queryDNSServer("ws20");
    assertNotNull(server);
    assertNotEquals(0, server.size());
  }

  @Test
  public void testReadWrite() {
    FlightgearNAPTRQuery q = new FlightgearNAPTRQuery();
    List<WeightedUrl> server = q.queryDNSServer("ws20");
    assertNotNull(server);
    assertNotEquals(0, server.size());
    String[] v = q.getVersions();
    assertNotNull(v);
    assertNotEquals(0, v.length);
    
  }
}
