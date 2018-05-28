package terramaster2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.flightgear.terramaster.TerraMaster;
import org.flightgear.terramaster.dns.FlightgearNAPTRQuery;
import org.flightgear.terramaster.dns.WeightedUrl;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

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

  @Test
  public void testOutage() {
    FlightgearNAPTRQuery q = new FlightgearNAPTRQuery();
    List<WeightedUrl> server = q.queryDNSServer("ws20");
    assertNotNull(server);
    assertNotEquals(0, server.size());
    String[] v = q.getVersions();
    assertNotNull(v);
    assertNotEquals(0, v.length);
    // Now we have a response

    FlightgearNAPTRQuery q2 = mock(FlightgearNAPTRQuery.class);
    Whitebox.setInternalState(q2, "log", Logger.getLogger(TerraMaster.LOGGER_CATEGORY));
    Whitebox.setInternalState(q2, "urls", new ArrayList<>());
    List<String> timeOutServers = new ArrayList<>();
    timeOutServers.add("72.66.115.13");
    when(q2.getNameservers()).thenReturn(timeOutServers); // Mock implementation
    when(q2.queryDNSServer("ws20")).thenCallRealMethod();
    List<WeightedUrl> server2 = q2.queryDNSServer("ws20");
    assertNotNull(server2);
    assertNotEquals(0, server2.size());
    assertEquals(server.get(0), server.get(0));
  }
}
