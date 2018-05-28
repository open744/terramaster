package terramaster2;

import static org.junit.Assert.*;

import org.flightgear.terramaster.CoordinateCalculation;
import org.junit.Test;

public class TestCoordinates {

	@Test
	public void test() {
		double bearing = CoordinateCalculation.greatCircleBearing(10, 1, 10, 2);
		assertEquals(90, bearing, 1);
	}

	@Test
	public void test1() {
		double bearing = CoordinateCalculation.greatCircleBearing(10, 1, 10, -2);
		assertEquals(270, bearing, 1);
	}
	@Test
	public void test2() {
		double bearing = CoordinateCalculation.greatCircleBearing(10, 1, 11, 1);
		assertEquals(0, bearing, 1);
	}
	@Test
	public void test3() {
		double bearing = CoordinateCalculation.greatCircleBearing(10, 1, 9, 1);
		assertEquals(180, bearing, 1);
	}

	@Test
	public void test4() {
		double bearing = CoordinateCalculation.greatCircleBearing(40.777244, -73.872608, 51.423889, 12.236389);
		assertEquals(47.79, bearing, 1);
	}
	@Test
	public void test5() {
		double dist = CoordinateCalculation.greatCircleDistance(40.777244, -73.872608, 51.423889, 12.236389);
		assertEquals(6353.295, dist, 2);
	}
}
