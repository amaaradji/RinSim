/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.util;

import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.tan;

import java.util.Hashtable;
import java.util.Map;

/**
 * Adapted from:
 * http://www.ibm.com/developerworks/java/library/j-coordconvert/index.html
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class CoordinateConversion {

	private CoordinateConversion() {

	}

	public static double[] utm2LatLon(String UTM) {
		UTM2LatLon c = new UTM2LatLon();
		return c.convertUTMToLatLong(UTM);
	}

	public static String latLon2UTM(double latitude, double longitude) {
		LatLon2UTM c = new LatLon2UTM();
		return c.convertLatLonToUTM(latitude, longitude);

	}

	protected static void validate(double latitude, double longitude) {
		if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude >= 180.0) {
			throw new IllegalArgumentException("Legal ranges: latitude [-90,90], longitude [-180,180).");
		}

	}

	public static String latLon2MGRUTM(double latitude, double longitude) {
		LatLon2MGRUTM c = new LatLon2MGRUTM();
		return c.convertLatLonToMGRUTM(latitude, longitude);

	}

	public static double[] mgrutm2LatLon(String MGRUTM) {
		MGRUTM2LatLon c = new MGRUTM2LatLon();
		return c.convertMGRUTMToLatLong(MGRUTM);
	}

	public static double degreeToRadian(double degree) {
		return degree * Math.PI / 180;
	}

	public static double radianToDegree(double radian) {
		return radian * 180 / Math.PI;
	}

	protected static class LatLon2UTM {
		public String convertLatLonToUTM(double latitude, double longitude) {
			validate(latitude, longitude);
			String UTM = "";

			setVariables(latitude, longitude);

			String longZone = getLongZone(longitude);
			LatZones latZones = new LatZones();
			String latZone = latZones.getLatZone(latitude);

			double _easting = getEasting();
			double _northing = getNorthing(latitude);

			UTM = longZone + " " + latZone + " " + (int) _easting + " " + (int) _northing;
			// UTM = longZone + " " + latZone + " " + decimalFormat.format(_easting) +
			// " "+ decimalFormat.format(_northing);

			return UTM;

		}

		protected void setVariables(double latitude, double longitude) {
			latitude = degreeToRadian(latitude);
			rho = equatorialRadius * (1 - e * e) / pow(1 - pow(e * sin(latitude), 2), 3 / 2.0);

			nu = equatorialRadius / pow(1 - pow(e * sin(latitude), 2), (1 / 2.0));

			double var1;
			if (longitude < 0.0) {
				var1 = (int) ((180 + longitude) / 6.0) + 1;
			} else {
				var1 = (int) (longitude / 6) + 31;
			}
			double var2 = 6 * var1 - 183;
			double var3 = longitude - var2;
			p = var3 * 3600 / 10000;

			S = A0 * latitude - B0 * sin(2 * latitude) + C0 * sin(4 * latitude) - D0 * sin(6 * latitude) + E0 * sin(8 * latitude);

			K1 = S * k0;
			K2 = nu * sin(latitude) * cos(latitude) * pow(sin1, 2) * k0 * 100000000 / 2;
			K3 = pow(sin1, 4) * nu * sin(latitude) * Math.pow(cos(latitude), 3) / 24
					* (5 - pow(tan(latitude), 2) + 9 * e1sq * pow(cos(latitude), 2) + 4 * pow(e1sq, 2) * pow(cos(latitude), 4)) * k0 * 10000000000000000L;

			K4 = nu * cos(latitude) * sin1 * k0 * 10000;

			K5 = pow(sin1 * cos(latitude), 3) * nu / 6 * (1 - pow(tan(latitude), 2) + e1sq * pow(cos(latitude), 2)) * k0 * 1000000000000L;

			A6 = pow(p * sin1, 6) * nu * sin(latitude) * pow(cos(latitude), 5) / 720
					* (61 - 58 * pow(tan(latitude), 2) + pow(tan(latitude), 4) + 270 * e1sq * pow(cos(latitude), 2) - 330 * e1sq * pow(sin(latitude), 2)) * k0 * 1E+24;

		}

		protected String getLongZone(double longitude) {
			double longZone = 0;
			if (longitude < 0.0) {
				longZone = (180.0 + longitude) / 6 + 1;
			} else {
				longZone = longitude / 6 + 31;
			}
			String val = String.valueOf((int) longZone);
			if (val.length() == 1) {
				val = "0" + val;
			}
			return val;
		}

		protected double getNorthing(double latitude) {
			double northing = K1 + K2 * p * p + K3 * pow(p, 4);
			if (latitude < 0.0) {
				northing = 10000000 + northing;
			}
			return northing;
		}

		protected double getEasting() {
			return 500000 + (K4 * p + K5 * pow(p, 3));
		}

		// Lat Lon to UTM variables

		// equatorial radius
		double equatorialRadius = 6378137;

		// polar radius
		double polarRadius = 6356752.314;

		// flattening
		double flattening = 0.00335281066474748;// (equatorialRadius-polarRadius)/equatorialRadius;

		// inverse flattening 1/flattening
		double inverseFlattening = 298.257223563;// 1/flattening;

		// Mean radius
		double rm = pow(equatorialRadius * polarRadius, 1 / 2.0);

		// scale factor
		double k0 = 0.9996;

		// eccentricity
		double e = Math.sqrt(1 - pow(polarRadius / equatorialRadius, 2));

		double e1sq = e * e / (1 - e * e);

		double n = (equatorialRadius - polarRadius) / (equatorialRadius + polarRadius);

		// r curv 1
		double rho = 6368573.744;

		// r curv 2
		double nu = 6389236.914;

		// Calculate Meridional Arc Length
		// Meridional Arc
		double S = 5103266.421;

		double A0 = 6367449.146;

		double B0 = 16038.42955;

		double C0 = 16.83261333;

		double D0 = 0.021984404;

		double E0 = 0.000312705;

		// Calculation Constants
		// Delta Long
		double p = -0.483084;

		double sin1 = 4.84814E-06;

		// Coefficients for UTM Coordinates
		double K1 = 5101225.115;

		double K2 = 3750.291596;

		double K3 = 1.397608151;

		double K4 = 214839.3105;

		double K5 = -2.995382942;

		double A6 = -1.00541E-07;

	}

	protected static class LatLon2MGRUTM extends LatLon2UTM {
		public String convertLatLonToMGRUTM(double latitude, double longitude) {
			validate(latitude, longitude);
			String mgrUTM = "";

			setVariables(latitude, longitude);

			String longZone = getLongZone(longitude);
			LatZones latZones = new LatZones();
			String latZone = latZones.getLatZone(latitude);

			double _easting = getEasting();
			double _northing = getNorthing(latitude);
			Digraphs digraphs = new Digraphs();
			String digraph1 = digraphs.getDigraph1(Integer.parseInt(longZone), _easting);
			String digraph2 = digraphs.getDigraph2(Integer.parseInt(longZone), _northing);

			String easting = String.valueOf((int) _easting);
			if (easting.length() < 5) {
				easting = "00000" + easting;
			}
			easting = easting.substring(easting.length() - 5);

			String northing;
			northing = String.valueOf((int) _northing);
			if (northing.length() < 5) {
				northing = "0000" + northing;
			}
			northing = northing.substring(northing.length() - 5);

			mgrUTM = longZone + latZone + digraph1 + digraph2 + easting + northing;
			return mgrUTM;
		}
	}

	protected static class MGRUTM2LatLon extends UTM2LatLon {
		public double[] convertMGRUTMToLatLong(String mgrutm) {
			double[] latlon = { 0.0, 0.0 };
			// 02CNR0634657742
			int zone = Integer.parseInt(mgrutm.substring(0, 2));
			String latZone = mgrutm.substring(2, 3);

			String digraph1 = mgrutm.substring(3, 4);
			String digraph2 = mgrutm.substring(4, 5);
			easting = Double.parseDouble(mgrutm.substring(5, 10));
			northing = Double.parseDouble(mgrutm.substring(10, 15));

			LatZones lz = new LatZones();
			double latZoneDegree = lz.getLatZoneDegree(latZone);

			double a1 = latZoneDegree * 40000000 / 360.0;
			double a2 = 2000000 * Math.floor(a1 / 2000000.0);

			Digraphs digraphs = new Digraphs();

			double digraph2Index = digraphs.getDigraph2Index(digraph2);

			double startindexEquator = 1;
			if (1 + zone % 2 == 1) {
				startindexEquator = 6;
			}

			double a3 = a2 + (digraph2Index - startindexEquator) * 100000;
			if (a3 <= 0) {
				a3 = 10000000 + a3;
			}
			northing = a3 + northing;

			zoneCM = -183 + 6 * zone;
			double digraph1Index = digraphs.getDigraph1Index(digraph1);
			int a5 = 1 + zone % 3;
			double[] a6 = { 16, 0, 8 };
			double a7 = 100000 * (digraph1Index - a6[a5 - 1]);
			easting = easting + a7;

			setVariables();

			double latitude = 0;
			latitude = 180 * (phi1 - fact1 * (fact2 + fact3 + fact4)) / Math.PI;

			if (latZoneDegree < 0) {
				latitude = 90 - latitude;
			}

			double d = _a2 * 180 / Math.PI;
			double longitude = zoneCM - d;

			if (getHemisphere(latZone).equals("S")) {
				latitude = -latitude;
			}

			latlon[0] = latitude;
			latlon[1] = longitude;
			return latlon;
		}
	}

	protected static class UTM2LatLon {
		double easting;

		double northing;

		int zone;

		String southernHemisphere = "ACDEFGHJKLM";

		protected String getHemisphere(String latZone) {
			String hemisphere = "N";
			if (southernHemisphere.indexOf(latZone) > -1) {
				hemisphere = "S";
			}
			return hemisphere;
		}

		public double[] convertUTMToLatLong(String UTM) {
			double[] latlon = { 0.0, 0.0 };
			String[] utm = UTM.split(" ");
			zone = Integer.parseInt(utm[0]);
			String latZone = utm[1];
			easting = Double.parseDouble(utm[2]);
			northing = Double.parseDouble(utm[3]);
			String hemisphere = getHemisphere(latZone);
			double latitude = 0.0;
			double longitude = 0.0;

			if (hemisphere.equals("S")) {
				northing = 10000000 - northing;
			}
			setVariables();
			latitude = 180 * (phi1 - fact1 * (fact2 + fact3 + fact4)) / Math.PI;

			if (zone > 0) {
				zoneCM = 6 * zone - 183.0;
			} else {
				zoneCM = 3.0;

			}

			longitude = zoneCM - _a3;
			if (hemisphere.equals("S")) {
				latitude = -latitude;
			}

			latlon[0] = latitude;
			latlon[1] = longitude;
			return latlon;

		}

		protected void setVariables() {
			arc = northing / k0;
			mu = arc / (a * (1 - pow(e, 2) / 4.0 - 3 * pow(e, 4) / 64.0 - 5 * pow(e, 6) / 256.0));

			ei = (1 - pow((1 - e * e), (1 / 2.0))) / (1 + pow((1 - e * e), (1 / 2.0)));

			ca = 3 * ei / 2 - 27 * pow(ei, 3) / 32.0;

			cb = 21 * pow(ei, 2) / 16 - 55 * pow(ei, 4) / 32;
			cc = 151 * pow(ei, 3) / 96;
			cd = 1097 * pow(ei, 4) / 512;
			phi1 = mu + ca * sin(2 * mu) + cb * sin(4 * mu) + cc * sin(6 * mu) + cd * sin(8 * mu);

			n0 = a / pow((1 - pow((e * sin(phi1)), 2)), (1 / 2.0));

			r0 = a * (1 - e * e) / pow((1 - pow((e * sin(phi1)), 2)), (3 / 2.0));
			fact1 = n0 * tan(phi1) / r0;

			_a1 = 500000 - easting;
			dd0 = _a1 / (n0 * k0);
			fact2 = dd0 * dd0 / 2;

			t0 = pow(tan(phi1), 2);
			Q0 = e1sq * pow(cos(phi1), 2);
			fact3 = (5 + 3 * t0 + 10 * Q0 - 4 * Q0 * Q0 - 9 * e1sq) * pow(dd0, 4) / 24;

			fact4 = (61 + 90 * t0 + 298 * Q0 + 45 * t0 * t0 - 252 * e1sq - 3 * Q0 * Q0) * pow(dd0, 6) / 720;

			//
			lof1 = _a1 / (n0 * k0);
			lof2 = (1 + 2 * t0 + Q0) * pow(dd0, 3) / 6.0;
			lof3 = (5 - 2 * Q0 + 28 * t0 - 3 * pow(Q0, 2) + 8 * e1sq + 24 * pow(t0, 2)) * pow(dd0, 5) / 120;
			_a2 = (lof1 - lof2 + lof3) / cos(phi1);
			_a3 = _a2 * 180 / Math.PI;

		}

		double arc;

		double mu;

		double ei;

		double ca;

		double cb;

		double cc;

		double cd;

		double n0;

		double r0;

		double _a1;

		double dd0;

		double t0;

		double Q0;

		double lof1;

		double lof2;

		double lof3;

		double _a2;

		double phi1;

		double fact1;

		double fact2;

		double fact3;

		double fact4;

		double zoneCM;

		double _a3;

		double b = 6356752.314;

		double a = 6378137;

		double e = 0.081819191;

		double e1sq = 0.006739497;

		double k0 = 0.9996;

	}

	protected static class Digraphs {
		private final Map<Integer, String> digraph1 = new Hashtable<Integer, String>();

		private final Map<Integer, String> digraph2 = new Hashtable<Integer, String>();

		private final String[] digraph1Array = { "A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L", "M", "N", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z" };

		private final String[] digraph2Array = { "V", "A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L", "M", "N", "P", "Q", "R", "S", "T", "U", "V" };

		public Digraphs() {
			digraph1.put(new Integer(1), "A");
			digraph1.put(new Integer(2), "B");
			digraph1.put(new Integer(3), "C");
			digraph1.put(new Integer(4), "D");
			digraph1.put(new Integer(5), "E");
			digraph1.put(new Integer(6), "F");
			digraph1.put(new Integer(7), "G");
			digraph1.put(new Integer(8), "H");
			digraph1.put(new Integer(9), "J");
			digraph1.put(new Integer(10), "K");
			digraph1.put(new Integer(11), "L");
			digraph1.put(new Integer(12), "M");
			digraph1.put(new Integer(13), "N");
			digraph1.put(new Integer(14), "P");
			digraph1.put(new Integer(15), "Q");
			digraph1.put(new Integer(16), "R");
			digraph1.put(new Integer(17), "S");
			digraph1.put(new Integer(18), "T");
			digraph1.put(new Integer(19), "U");
			digraph1.put(new Integer(20), "V");
			digraph1.put(new Integer(21), "W");
			digraph1.put(new Integer(22), "X");
			digraph1.put(new Integer(23), "Y");
			digraph1.put(new Integer(24), "Z");

			digraph2.put(new Integer(0), "V");
			digraph2.put(new Integer(1), "A");
			digraph2.put(new Integer(2), "B");
			digraph2.put(new Integer(3), "C");
			digraph2.put(new Integer(4), "D");
			digraph2.put(new Integer(5), "E");
			digraph2.put(new Integer(6), "F");
			digraph2.put(new Integer(7), "G");
			digraph2.put(new Integer(8), "H");
			digraph2.put(new Integer(9), "J");
			digraph2.put(new Integer(10), "K");
			digraph2.put(new Integer(11), "L");
			digraph2.put(new Integer(12), "M");
			digraph2.put(new Integer(13), "N");
			digraph2.put(new Integer(14), "P");
			digraph2.put(new Integer(15), "Q");
			digraph2.put(new Integer(16), "R");
			digraph2.put(new Integer(17), "S");
			digraph2.put(new Integer(18), "T");
			digraph2.put(new Integer(19), "U");
			digraph2.put(new Integer(20), "V");

		}

		public int getDigraph1Index(String letter) {
			for (int i = 0; i < digraph1Array.length; i++) {
				if (digraph1Array[i].equals(letter)) {
					return i + 1;
				}
			}

			return -1;
		}

		public int getDigraph2Index(String letter) {
			for (int i = 0; i < digraph2Array.length; i++) {
				if (digraph2Array[i].equals(letter)) {
					return i;
				}
			}

			return -1;
		}

		public String getDigraph1(int longZone, double easting) {
			int a1 = longZone;
			double a2 = 8 * ((a1 - 1) % 3) + 1;

			double a3 = easting;
			double a4 = a2 + (int) (a3 / 100000) - 1;
			return digraph1.get(new Integer((int) Math.floor(a4)));
		}

		public String getDigraph2(int longZone, double northing) {
			int a1 = longZone;
			double a2 = 1 + 5 * ((a1 - 1) % 2);
			double a3 = northing;
			double a4 = a2 + (int) (a3 / 100000);
			a4 = (a2 + (int) (a3 / 100000.0)) % 20;
			a4 = Math.floor(a4);
			if (a4 < 0) {
				a4 = a4 + 19;
			}
			return digraph2.get(new Integer((int) Math.floor(a4)));

		}

	}

	protected static class LatZones {
		private final char[] letters = { 'A', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Z' };

		private final int[] degrees = { -90, -84, -72, -64, -56, -48, -40, -32, -24, -16, -8, 0, 8, 16, 24, 32, 40, 48, 56, 64, 72, 84 };

		private final char[] negLetters = { 'A', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M' };

		private final int[] negDegrees = { -90, -84, -72, -64, -56, -48, -40, -32, -24, -16, -8 };

		private final char[] posLetters = { 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Z' };

		private final int[] posDegrees = { 0, 8, 16, 24, 32, 40, 48, 56, 64, 72, 84 };

		private final int arrayLength = 22;

		public LatZones() {
		}

		public int getLatZoneDegree(String letter) {
			char ltr = letter.charAt(0);
			for (int i = 0; i < arrayLength; i++) {
				if (letters[i] == ltr) {
					return degrees[i];
				}
			}
			return -100;
		}

		public String getLatZone(double latitude) {
			int latIndex = -2;
			int lat = (int) latitude;

			if (lat >= 0) {
				int len = posLetters.length;
				for (int i = 0; i < len; i++) {
					if (lat == posDegrees[i]) {
						latIndex = i;
						break;
					}

					if (lat > posDegrees[i]) {
						continue;
					} else {
						latIndex = i - 1;
						break;
					}
				}
			} else {
				int len = negLetters.length;
				for (int i = 0; i < len; i++) {
					if (lat == negDegrees[i]) {
						latIndex = i;
						break;
					}

					if (lat < negDegrees[i]) {
						latIndex = i - 1;
						break;
					} else {
						continue;
					}

				}

			}

			if (latIndex == -1) {
				latIndex = 0;
			}
			if (lat >= 0) {
				if (latIndex == -2) {
					latIndex = posLetters.length - 1;
				}
				return String.valueOf(posLetters[latIndex]);
			} else {
				if (latIndex == -2) {
					latIndex = negLetters.length - 1;
				}
				return String.valueOf(negLetters[latIndex]);

			}
		}

	}

}
