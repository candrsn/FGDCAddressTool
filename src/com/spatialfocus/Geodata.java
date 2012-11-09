package com.spatialfocus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.net.MalformedURLException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.h2.tools.Csv;
import org.h2.util.DateTimeUtils;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.AttributeList;
import org.xml.sax.SAXException;

import org.geotools.*;
import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.shapefile.shp.JTSUtilities;
import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;

import com.spatialfocus.gui.AddressToolapp;
//import com.sun.xml.internal.txw2.output.DataWriter;
//import com.sun.xml.internal.bind.marshaller.DataWriter;
import com.megginson.sax.*;
import com.vividsolutions.jts.geom.Geometry;

public class Geodata {
	static Connection dbconn = null;
	String workDir = "projects";
	String projName = null;
	String sprojName = null;
	String operationMode = "";
	String evt_xml = "address_pkg.xml";
	String evt_met = "address_met.xml";
	String evt_rpt = "address_rpt.txt";
	String addressDataFile, abbreviationDataTable, addressFieldMap,
			subAddrFldMap, placeFldMap, aliasDataTable, expertParsingData;
	String qATestList, transformList, zoneField = "ZIPCODE";
	String validModes = "'initialize'transform'qa'publish'exportfiles'";

	String addrURI = "http://www.fgdc.gov/schemas/address/addr";
	String addrTypeURI = "http://www.fgdc.gov/schemas/address/addr_type";
	int maxRecords = -1;  // set to -1 for unlimited 
	
	public Geodata(String longName, String shortName) throws Exception {

		// Get some default names

		projName = longName;
		sprojName = shortName;

		// Load the user settings
		getParams();

	}

	public void getParams() throws Exception {
		boolean paramsOk = true;
		String e = "\n\n";

		java.util.Properties configFile = new java.util.Properties();
		InputStream pf;
		File pfil = new File("./AddressTool.properties");
		if ( pfil.exists() ) {
			pf = new FileInputStream(pfil);
		} else {	
			pf = this.getClass().getClassLoader()
					.getResourceAsStream("AddressTool.properties");
			if ( pf == null ) {
				pf = this.getClass().getClassLoader()
						.getResourceAsStream("AddressTool_default.properties");
			}
		}
		
		if (pf != null) {
			configFile.load(pf);

			operationMode = configFile.getProperty("OperationMode");
			workDir = configFile.getProperty("work_dir");
			projName = configFile.getProperty("ProjectName");
			sprojName = configFile.getProperty("ShortName");
			addressDataFile = configFile.getProperty("AddressData");
			abbreviationDataTable = configFile.getProperty("AbbreviationsFile");
			addressFieldMap = configFile.getProperty("AddressFieldMap");
			placeFldMap = configFile.getProperty("PlaceFieldMap");
			subAddrFldMap = configFile.getProperty("SubAddressFieldMap");
			aliasDataTable = configFile.getProperty("AliasData");

			expertParsingData = configFile.getProperty("ExpertParsingData");

			qATestList = configFile.getProperty("QATests");
			transformList = configFile.getProperty("Transforms");
			zoneField = configFile.getProperty("ZoneField");

			File f = new File(aliasDataTable);
			if (!f.exists()) {
				e.concat("Address Alias Data " + aliasDataTable
						+ " not found \n");
				paramsOk = false;
			}
			f = new File(abbreviationDataTable);
			if (!f.exists()) {
				e = e.concat("Abbreviations Data " + abbreviationDataTable
						+ " not found \n");
				paramsOk = false;
			}
			f = new File(addressFieldMap);
			if (!f.exists()) {
				e = e.concat("Address Field Mapping Data " + addressFieldMap
						+ " not found \n");
				paramsOk = false;
			}
			f = new File(subAddrFldMap);
			if (!f.exists()) {
				e = e.concat("Occupancy Field Mapping Data " + subAddrFldMap
						+ " not found \n");
				paramsOk = false;
			}
			f = new File(placeFldMap);
			if (!f.exists()) {
				e = e.concat("PlaceName Field Mapping Data " + placeFldMap
						+ " not found \n");
				paramsOk = false;

			}

		} else {

			abbreviationDataTable = "resources/sql/common_abbr.csv";
			addressFieldMap = "resources/sql/mapaddress.csv";
			subAddrFldMap = "resources/sql/mapsubaddress.csv";
			aliasDataTable = "resources/sql/common_alias.csv";
			expertParsingData = null;

		}

		File f = new File(addressDataFile);
		if (!f.exists()) {
			e = e.concat("Address Data " + addressDataFile + " not found \n");
			paramsOk = false;
		}
		if ( ! validModes.contains("'" + operationMode + "'")) {
			e = e.concat("Operational Mode " + operationMode
					+ " not supported\n");
			paramsOk = false;
		}
		if (!paramsOk) {
			throw new FileNotFoundException(e + "\n" + "   Exiting");
		}
		evt_xml = sprojName + "_pkg.xml";
		evt_met = sprojName + "_met.xml";
		evt_rpt = sprojName + "_rpt.txt";
		
		if ( pf != null ) {
			pf.close();
		}

	}

	public int initProject() throws SQLException, Exception {
		int needInit = 1;
		// make sure that workDir exists and is writeable
		File f = new File(workDir);
		if ( workDir == null || workDir.equals("")  || ( ! f.canWrite() ) ) {
			System.out.println("Working directory " + workDir + " must exist and be writeable.");
			return 0;
		}
		
		f = new File(workDir + "/tmp");
		if ( (! f.exists()) || f.mkdir() ) {
			System.out.println("Unable to create temp directory: " + workDir + "/tmp.");
			return 0;
		}
		
		f = new File(workDir + "/" + sprojName + ".h2.db");

		if (f.exists()) {
			needInit = 1;
			f.delete();
			f = new File(workDir + "/" + sprojName + ".h2.db");
		}
		if (dbconn == null || !dbconn.isValid(500)) {
			Class.forName("org.h2.Driver");
			dbconn = DriverManager.getConnection("jdbc:h2:" + workDir + "/"
					+ sprojName, "sa", "");

			if (needInit == 1) {
				createSupportTables();
			}
		}
		return 1;
	}

	public int createSupportTables() throws Exception {

		Statement stmt2 = dbconn.createStatement();

		File f = new File("resources/sql/support_tables.sql");
		if (!f.exists()) {
			System.out.println("required table templates not found");

			return 0;
		}

		StringBuilder sqlText = new StringBuilder();
		String NL = System.getProperty("line.separator");
		Scanner scanner = new Scanner(new FileInputStream(f));
		try {
			while (scanner.hasNextLine()) {
				sqlText.append(scanner.nextLine() + NL);
			}
		} finally {
			scanner.close();
		}

		stmt2.execute(sqlText.toString());
		stmt2.execute("INSERT INTO project_log (log) values('Address Tool - 0.0.5')");
		stmt2.execute("INSERT INTO project_log (log) values('Project - "
				+ projName + "')");
		stmt2.execute("INSERT INTO project_log (log) values('Short Name - "
				+ sprojName + "')");

		return 1;
	}

	public int applyAbbr(String tbl) throws Exception {
		Statement stmt = dbconn.createStatement();
		String s = "";
		String[] aas = new String("PostAbbrev PreAbbrev").split(" ");

		for (int i = 0; i < aas.length; i++) {
			s = "";
			File af = new File("resources/sql/transforms/" + aas[i] + ".sql");
			if (af.exists()) {
				try {
					BufferedReader in = new BufferedReader(new FileReader(af));
					String str;
					while ((str = in.readLine()) != null) {
						s = s.concat(str + "\n");
					}
					in.close();
				} catch (IOException e) {
				}
				s = s.replace("%tablename%", tbl);
				stmt.execute(s);
			}
		}

		return 1;
	}

	public int applyTransforms(String tbl) throws Exception {
		Statement stmt = dbconn.createStatement();
		String s = "";
		String[] tas = transformList.split(" ");

		for (int i = 0; i < tas.length; i++) {
			s = "";
			File tf = new File("resources/sql/transforms/" + tas[i] + ".sql");
			if (tf.exists()) {
				try {
					BufferedReader in = new BufferedReader(new FileReader(tf));
					String str;
					while ((str = in.readLine()) != null) {
						s = s.concat(str + "\n");
					}
					in.close();
				} catch (IOException e) {
				}
				s = s.replace("%tablename%", tbl);
				stmt.execute(s);
			}
		}

		return 1;
	}

	public int runQA(String tbl) throws Exception {
		Statement stmt = dbconn.createStatement();
		Statement stmtq = dbconn.createStatement();
		String[] s = { "", "", "", "" };
		String[] qas = qATestList.split(" ");
		File qar = new File(workDir + "/" + evt_rpt);
		FileWriter qafw = new FileWriter(qar);

		for (int i = 0; i < qas.length; i++) {
			s[0] = "";
			File qf = new File("resources/sql/qa/" + qas[i] + ".sql");
			if (qf.exists()) {
				try {
					BufferedReader in = new BufferedReader(new FileReader(qf));
					String str;
					int typ = 0;
					while ((str = in.readLine()) != null) {
						if (str.startsWith("--Setup")) {
							typ = 1;
							/* Throw away anything collected as a query so far */
							s[0] = "";
						}
						if (str.startsWith("--Query")) {
							typ = 0;
						}
						if (str.startsWith("--Cleanup")) {
							typ = 2;
						}
						if (str.startsWith("--ReportNothing")) {
							typ = 3;
							/* Grab the next line for the body */
							str = in.readLine().substring(3);
						}
						s[typ] = s[typ].concat(str + "\n");
					}
					in.close();
				} catch (IOException e) {
				}
				s[0] = s[0].replace("%tablename%", tbl);
				s[1] = s[1].replace("%tablename%", tbl);
				s[2] = s[2].replace("%tablename%", tbl);

				System.out.println("QA Test " + qas[i] + " returned: ");
				qafw.write("QA Test " + qas[i] + " returned: \n");

				if (s[1].length() > 0) {
					stmtq.execute(s[1]);
				}

				String resv = "";
				ResultSet rsm = stmt.executeQuery(s[0]);
				int rc = 0;
				while (rsm.next()) {
					if (rsm.getObject(1) != null) {
						resv = rsm.getObject(1).toString();
					} else {
						resv = qas[i] + ": Null";
					}
					System.out.println("    " + resv);
					qafw.write("     " + resv + "\n");
					rc++;
				}
				if (rc == 0) {
					System.out.println("    " + s[3]);
					qafw.write("     " + s[3] + "\n");
				}
				if (s[2].length() > 0) {
					stmtq.execute(s[2]);
				}
				qafw.write("\n\n");
			} // qafile Exists
		} // each qafile
		if (qafw != null) {
			qafw.flush();
			qafw.close();
		}

		return 1;
	}

	public int buildInfoTables(String tbl, String script) throws Exception {
		Statement stmt = dbconn.createStatement();
		Statement stmtq = dbconn.createStatement();
		String[] s = { "", "", "", "" };
		String[] qas = script.split(" ");
		File qar = new File(workDir + "/q" + evt_rpt);
		FileWriter qafw = new FileWriter(qar);

		for (int i = 0; i < qas.length; i++) {
			s[0] = "";
			File qf = new File("resources/sql/transforms/" + qas[i] + ".sql");
			if (qf.exists()) {
				try {
					BufferedReader in = new BufferedReader(new FileReader(qf));
					String str;
					int typ = 0;
					while ((str = in.readLine()) != null) {
						if (str.startsWith("--Setup")) {
							typ = 1;
							/* Throw away anything collected as a query so far */
							s[0] = "";
						}
						if (str.startsWith("--Query")) {
							typ = 0;
						}
						if (str.startsWith("--Cleanup")) {
							typ = 2;
						}
						if (str.startsWith("--ReportNothing")) {
							typ = 3;
							/* Grab the next line for the body */
							str = in.readLine().substring(3);
						}
						s[typ] = s[typ].concat(str + "\n");
					}
					in.close();
				} catch (IOException e) {
				}
				s[0] = s[0].replace("%tablename%", tbl);
				s[0] = s[0].replace("%zonefield%", zoneField);

				s[1] = s[1].replace("%tablename%", tbl);
				s[2] = s[2].replace("%tablename%", tbl);

				System.out.println("BuildInfo " + qas[i] + " returned: ");
				qafw.write("BuildInfo " + qas[i] + " returned: \n");

				if (s[1].length() > 0) {
					stmtq.execute(s[1]);
				}

				String resv = "";
				ResultSet rsm = stmt.executeQuery(s[0]);
				int rc = 0;
				while (rsm.next()) {
					if (rsm.getObject(1) != null) {
						resv = rsm.getObject(1).toString();
					} else {
						resv = qas[i] + ": Null";
					}
					System.out.println("    " + resv);
					qafw.write("     " + resv + "\n");
					rc++;
				}
				if (rc == 0) {
					System.out.println("    " + s[3]);
					qafw.write("     " + s[3] + "\n");
				}
				if (s[2].length() > 0) {
					stmtq.execute(s[2]);
				}		

				qafw.write("\n\n");
			} // qafile Exists
		} // each qafile
		if (qafw != null) {
			qafw.flush();
			qafw.close();
		}

		return 1;
	}

	public static Integer h2Stmt(String smt) throws Exception {

		if (dbconn.createStatement().execute(smt))
			return 0;

		// return a failure code
		return 1;
	}

	public static SimpleFeatureSource readGIS(String shpFilepath)
			throws Exception {

		SimpleFeatureSource featureSource = null;
		File shpFile = new File(shpFilepath);

		try {
			if ((!shpFile.exists()) && (!shpFile.isFile())) {		

				String message = "file is not found";
				System.out.println(message);
				// throw new FileNotFoundException(message);
				// the geometry field is not strictly required
			}

			Map<String, Serializable> connect = new HashMap<String, Serializable>();
			connect.put("url", shpFile.toURI().toURL());
			connect.put("charset", "Windows-1252");

			DataStore dataStore = DataStoreFinder.getDataStore(connect);
			String[] typeNames = dataStore.getTypeNames();
			String typeName = typeNames[0];

			featureSource = dataStore.getFeatureSource(typeName);

		} catch (FileNotFoundException e) {
			System.out.println(e.getLocalizedMessage());
			e.printStackTrace();
		} catch (MalformedURLException e) {
			System.out.println(e.getLocalizedMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println(e.getLocalizedMessage());
			e.printStackTrace();
		} finally {
			shpFile = null;
		}

		return featureSource;
	}

	public void exportTable(String filePath, String Table) {
		// do something

	};
	
	@SuppressWarnings("deprecation")
	public void importGIS(String shpFilePath) throws Exception {
		int warningCount = 0;
		String swkt = null;

		SimpleFeatureSource featureSource = readGIS(shpFilePath);
		SimpleFeatureCollection collection = featureSource.getFeatures();
		SimpleFeatureIterator iterator = collection.features();

		String val;
		String nam;
		String sep = "";
		FileWriter tw = new FileWriter(workDir + "/tmp/workread.csv");
		PrintWriter pw = new PrintWriter(tw);

		SimpleFeatureType fT = collection.getSchema();
		List<AttributeDescriptor> fTS = fT.getAttributeDescriptors();
		Iterator<AttributeDescriptor> fTi = fTS.iterator();

		while (fTi.hasNext()) {
			AttributeDescriptor Tn = fTi.next();
			Tn.getLocalName();

			pw.print(sep + Tn.getLocalName());

			sep = "\t";
		}
		pw.println("");

		int recs = 0;
		iterator = collection.features();
		while (iterator.hasNext() && (recs++ < maxRecords || maxRecords == -1) ) {

			SimpleFeature feature = (SimpleFeature) iterator.next();

			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			ShapeType type = JTSUtilities.findBestGeometryType(geometry);
			if (!type.isPolygonType() && !type.isLineType()
					&& !type.isPointType()) {
				System.out
						.println("The file contains unexpected geometry types");
				warningCount++;
				break;
			}

			if (geometry.isEmpty()) {
				swkt = "";
			} else {
				swkt = geometry.toText();
			}

			Collection<Property> props = feature.getProperties();

			Iterator<Property> it = props.iterator();
			sep = "";
			while (it.hasNext()) {
				Property prop = it.next();
				if (prop.getValue() != null) {
					val = prop.getValue().toString();
				} else {
					val = "";
				}
				pw.print(sep + val);
				sep = "\t";
			}
			pw.println("");
		}
		pw.close();
		tw.close();

		collection.close(iterator);
		iterator.close();
		DataAccess<SimpleFeatureType, SimpleFeature> ds = featureSource.getDataStore();
		
		ds.dispose();

		h2Import("rawdata", workDir + "/tmp/workread.csv");

		File f = new File(workDir + "/tmp/workread.csv");
		f.deleteOnExit();

		Statement stmt = dbconn.createStatement();
		stmt.execute("INSERT INTO table_info( id,name,role,status) values (0,'RAWDATA','address','R')");

	}

	public static int h2Import(String ctable, String cfile) throws Exception {

		h2Stmt("Drop TABLE if Exists " + ctable + ";");
		h2Stmt("CREATE TABLE " + ctable + " as (SELECT *  FROM CSVREAD('"
				+ cfile + "', null, 'fieldSeparator=' || CHAR(9)));");

		return 1;
	}

	public int loadMaps(String tbl) throws Exception {
		Statement stmt = dbconn.createStatement();
		Scanner scanner;

		File f = new File("resources/sql/address_table_tmpl.sql");
		if (!f.exists()) {
			BufferedReader fin = new BufferedReader(new InputStreamReader(
					this.getClass()
							.getClassLoader()
							.getResourceAsStream(
									"resources/sql/address_table_tmpl.sql")));
			scanner = new Scanner(new FileInputStream(f));
		} else {
			scanner = new Scanner(new FileInputStream(f));
		}

		StringBuilder sqlText = new StringBuilder();
		String NL = System.getProperty("line.separator");

		String s = null;

		try {
			while (scanner.hasNextLine()) {
				sqlText.append(scanner.nextLine() + NL);
			}
		} finally {
			scanner.close();
		}
		s = sqlText.toString().replaceAll("%tablename%", tbl);
		stmt.execute(s);

		stmt.execute("INSERT INTO table_info( id,name,role,status) values (1,'"
				+ tbl + "_core" + "','address','I')");

		stmt.execute("INSERT INTO table_info( id,name,role,status) values (1,'"
				+ tbl + "_prelim" + "','address','I')");

		h2Import("abbrmap", abbreviationDataTable);
		h2Import("addressmap", addressFieldMap);
		h2Import("subaddressmap", subAddrFldMap);
		h2Import("aliasmap", aliasDataTable);
		h2Import("placemap", placeFldMap);
		h2Import("expertparsing", expertParsingData);

		return 1;
	}

	public int mapRawFlds(String tbl) throws Exception {

		Statement stmt = dbconn.createStatement();
		Statement stmt2 = dbconn.createStatement();
		String s;

		// Build a SQL string to populate the intermediate format table
		String si = "";
		String sr = "";
		String sep = "";
		String sra = "ADDRESSID";

		ResultSet rs = stmt
				.executeQuery("Select * from addressmap where tableid = "
						+ "(SELECT id from table_info where name = '" + tbl
						+ "_prelim') ORDER BY MAP");

		while (rs.next()) {
			si = si + sep + rs.getObject("MAP");
			sr = sr + sep + rs.getObject("RAW");
			if (rs.getObject("MAP").toString().equals("ADDRESSID")) {
				sra = rs.getObject("RAW").toString();
			}
			sep = " ,";
		}

		s = "INSERT INTO " + tbl + "_prelim" + "(" + si + " )" + "(SELECT "
				+ sr + " FROM RawData )";

		stmt.execute(s);

		// Now Subaddress elements
		rs = stmt
				.executeQuery("Select * from subaddressmap where tableid = "
						+ "(SELECT id from table_info where name = '" + tbl
						+ "_core')");

		sep = ",";
		while (rs.next()) {
			si = "ADDRESSID" + sep + rs.getObject("MAP") + ",subaddresstype ";
			sr = sra + sep + rs.getObject("RAW") + ",'" + rs.getObject("RAW")
					+ "'";
			String sf = rs.getObject("RAW").toString();

			s = "INSERT INTO " + tbl + "_subaddress" + "(" + si + " )"
					+ "(SELECT " + sr + " FROM RawData WHERE " + sf
					+ " is not null)";
			stmt2.execute(s);
		}
		

		// Now Place elements
		rs = stmt
				.executeQuery("Select * from placemap where tableid = "
						+ "(SELECT id from table_info where name = '" + tbl
						+ "_core')");

		sep = ",";
		while (rs.next()) {
			si = "ADDRESSID" + sep + "placename" + ",placenametype ";
			sr = sra + sep + rs.getObject("RAW") + ",'" + rs.getObject("MAP")
					+ "'";
			String sf = rs.getObject("RAW").toString();

			s = "INSERT INTO " + tbl + "_place" + "(" + si + " )"
					+ "(SELECT " + sr + " FROM RawData WHERE " + sf
					+ " is not null)";
			stmt2.execute(s);
		}
		
		
		// Now the rest of Core
		stmt.execute("INSERT INTO " + tbl + "_core (SELECT * from " + tbl
				+ "_prelim )");
		
		
		return 1;

	}

	@SuppressWarnings("deprecation")
	public void exportXML() throws Exception {
		   DataWriter dw = null;

			Statement stmt = dbconn.createStatement();
			Statement stmt2 = dbconn.createStatement();
			String sql = "";
			ResultSet subsrs;
			ResultSetMetaData subrsm;

			//Get data from Core
			ResultSet srs = stmt.executeQuery("SELECT * from address_core");
			ResultSetMetaData rsm = srs.getMetaData();

			// Start Feed
			Map<String, Object> row = new HashMap<String, Object>();
			ArrayList<HashMap<String, Object>> rowplc = new ArrayList<HashMap<String, Object>>();
			ArrayList<HashMap<String, Object>> rowocc = new ArrayList<HashMap<String, Object>>();
			Map<String, Object> rowalias = new HashMap<String, Object>();

			while (srs.next()) {
				row.clear();
				for (int i = 1; i <= rsm.getColumnCount(); i++) {
					row.put(rsm.getColumnName(i).toLowerCase(), srs.getObject(i));
				}
				if (srs.isFirst())
					dw = writeXMLHdr(row);

				rowplc.clear();
				//Get data from place
				subsrs = stmt2.executeQuery("SELECT placename, placenametype, placenameorder from address_place " +
						" WHERE ADDRESSID = '" + row.get("addressid") + "' " +
						" ORDER BY placenameorder");
				subrsm = subsrs.getMetaData();
				while (subsrs.next()) {
					HashMap<String, Object> phm = new HashMap<String, Object>();
					for (int i = 1; i <= subrsm.getColumnCount(); i++) {
						phm.put(subrsm.getColumnName(i).toLowerCase(), subsrs.getObject(i));
					}
					rowplc.add(phm);
				}
				subsrs.close();
				
				rowocc.clear();
				//Get data from SubAddress
				subsrs = stmt2.executeQuery("SELECT subaddressid, subaddresstype, subaddressorder from address_subaddress " +
						" WHERE ADDRESSID = '" + row.get("addressid") + "' " +
						" ORDER by subaddressorder");
				subrsm = subsrs.getMetaData();
				while (subsrs.next()) {
					HashMap<String, Object> phm = new HashMap<String, Object>();
					for (int i = 1; i <= subrsm.getColumnCount(); i++) {
						phm.put(subrsm.getColumnName(i).toLowerCase(), subsrs.getObject(i));
					}
					rowocc.add(phm);
				}
				subsrs.close();
				
				rowalias.clear();
				//handle related Address info
				
				dw = writeXMLRow(dw, row, rowplc, rowocc, rowalias);
			}

			writeXMLFtr(dw);

			srs = stmt.executeQuery("SELECT count(*) from address_core");
			srs.first();
			String recs;
			if (srs.isFirst()) {
				recs = srs.getObject(1).toString();
				/* log the export */

				boolean srs2 = stmt2
						.execute("INSERT INTO publication_info (pub_records) values "
								+ "(" + recs + ")");
			}


	}
	
	public DataWriter writeXMLHdr(Map<String, Object> row) throws Exception {
		DataWriter dw = null;
		
		FileWriter tw = new FileWriter(workDir + "/"
				+ evt_xml);
		dw = new DataWriter(tw);
		
		dw.reset();
		
		dw.setIndentStep(2);

		// root elements
		dw.startDocument();

		/*
		 * <addr:AddressCollection version="0.4" 
		 *     xmlns:addr="http://www.fgdc.gov/schemas/address/addr" 
		 *     xmlns:addr_type="http://www.fgdc.gov/schemas/address/addr_type" 
		 *     xmlns:gml="http://www.opengis.net/gml"   
		 *     xsi:schemaLocation="http://www.fgdc.gov/schemas/address/addr addr.xsd ">
		 */
		
		dw.forceNSDecl(addrURI, "addr");
		dw.forceNSDecl(addrTypeURI, "addr_type");
		dw.forceNSDecl("http://schemas.opengis.net/gml/3.1.1","gml"); 
		dw.startPrefixMapping("addr", addrURI);	
		
		org.xml.sax.helpers.AttributesImpl attr = new org.xml.sax.helpers.AttributesImpl();
		attr.addAttribute("", "version", "version", "float", "0.4");					
		dw.startElement(addrURI, "AddressCollection", "addr:AddressCollection", attr);
		attr.clear();
		
	   return dw;
	}

	public int writeXMLFtr(DataWriter dw) throws Exception {
	
		// write the content into xml file
		dw.endElement(addrURI, "AddressCollection");
		dw.endDocument();	
		
		System.out.println("Local copy of xml file saved to: " +
			      workDir + "/" + evt_xml);	
		
		return 1;
	}

	public DataWriter writeXMLRow(DataWriter dw, Map<String, Object> row,
			ArrayList<HashMap<String, Object>> rowplc, ArrayList<HashMap<String, Object>> rowocc,
			Map<String, Object> rowalias) throws SAXException {
	
		Object s = null;
		org.xml.sax.helpers.AttributesImpl attr = new org.xml.sax.helpers.AttributesImpl();
        String addressClass = "NumberedThoroughfareAddress"; 
		try {
	
			dw.startElement(addrURI, addressClass);

			dw.startElement(addrURI, "CompleteAddressNumber");
			s = row.get("addressnumberprefix");
			if (s != null) {
				dw.dataElement(addrURI, "AddressNumberPrefix", s.toString());
			}
			s = row.get("addressnumber");
			if (s != null) {
				dw.dataElement(addrURI, "AddressNumber", s.toString());
			}
			s = row.get("addressnumbersuffix");
			if (s != null) {
				dw.dataElement(addrURI, "AddressNumberSuffix", s.toString());
			}
			dw.endElement(addrURI, "CompleteAddressNumber");
	
			dw.startElement(addrURI, "CompleteStreetName");
			s = row.get("streetnamepremodifier");
			
			if (s != null) {
				dw.dataElement(addrURI, "StreetNamePreModifier", s.toString());
			}
			s = row.get("streetnamepredirectional");
			if (s != null) {
				dw.dataElement(addrURI, "StreetNamePreDirectional", s.toString());
			}
			s = row.get("streetnamepretype");
			if (s != null) {
				dw.dataElement(addrURI, "StreetNamePreType", s.toString());
			}
			s = row.get("streetname");
			if (s != null) {
				dw.dataElement(addrURI, "StreetName", s.toString());
			}
			s = row.get("streetnameposttype");
			if (s != null) {
				dw.dataElement(addrURI, "StreetNamePostType", s.toString());
			}
			s = row.get("streetnamepostdirectional");
			if (s != null) {
				dw.dataElement(addrURI, "StreetNamePostDirectional", s.toString());
			}
			s = row.get("streetnamepostmodifier");
			if (s != null) {
				dw.dataElement(addrURI, "StreetNamePostModifier", s.toString());
			}
			dw.endElement(addrURI, "CompleteStreetName");
			
			dw.startElement(addrURI, "CompleteSubaddress");
			// Suites, Buildings, Apts
			for ( int si=0; si < rowocc.size(); si++) {
				HashMap<String, Object> sc = rowocc.get(si);
				String pn = sc.get("subaddressid").toString();
				String pt = sc.get("subaddresstype").toString();
				String po = sc.get("subaddressorder").toString();
				if (pn != null) {
					attr.clear();
					attr.addAttribute(addrURI, "SubaddressType", "addr:SubaddressType", "String", pt);
					if ( ! po.equals("") ) {
						attr.addAttribute(addrURI, "SubaddressOrder", "addr:SubaddressOrder", "String", po);
					}
					dw.dataElement(addrURI, "SubaddressElement", "", attr, pn);
				}
			}			
			dw.endElement(addrURI, "CompleteSubaddress");
			
			if (row.get("PlaceStateZip") != null) {
				dw.dataElement(addrURI, "PlaceStateZip", s.toString());
			} else {
				dw.startElement(addrURI, "CompletePlaceName");
				// USPSCommunityName
				// MunicipalJurisdiction
				// County
				for ( int si=0; si < rowplc.size(); si++) {
					HashMap<String, Object> sc = rowplc.get(si);
					String pn = sc.get("placename").toString();
					String pt = sc.get("placenametype").toString();
					String po = sc.get("placenameorder").toString();
					if (pn != null) {
						attr.clear();
						attr.addAttribute(addrURI, "PlaceNameType", "addr:PlaceNameType", "String", pt);
						if ( ! po.equals("") ) {
							attr.addAttribute(addrURI, "PlaceNameOrder", "addr:PlaceNameOrder", "String", po);
						}
						dw.dataElement(addrURI, "PlaceNameElement", "", attr, pn);
					}
				}
	
				s = row.get("statename");
				if (s != null && ! s.toString().isEmpty()) {
					dw.dataElement(addrURI, "StateName", s.toString());
				}
				s = row.get("zipcode");
				if (s != null && ! s.toString().isEmpty()) {
					dw.dataElement(addrURI, "ZipCode", s.toString());
				}
				s = row.get("zipplus4");
				if (s != null && ! s.toString().isEmpty()) {
					dw.dataElement(addrURI, "ZipPlus4", s.toString());
				}
				s = row.get("countryname");
				if (s != null && ! s.toString().isEmpty()) {
					dw.dataElement(addrURI, "CountryName", s.toString());
				}
			}
			dw.endElement(addrURI, "CompletePlaceName");
	
			s = row.get("addressid");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressId", s.toString());
			}
			s = row.get("addressauthority");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressAuthority", s.toString());
			}
	
			s = rowalias.get("relatedaddress1");
			if (s != null && ! s.toString().isEmpty()) {
				attr.clear();
				String po = rowalias.get("relationrole1").toString();
				if ( ! po.equals("") ) {
 				    attr.addAttribute(addrURI, "RelatedAddressType", "addr:RelatedAddressType", "String", po);
				}
				dw.dataElement(addrURI, "RelatedAddressId", "", attr, s.toString());
			}
			s = rowalias.get("relatedaddress2");
			if (s != null && !s.toString().isEmpty()) {
				attr.clear();
				String po = rowalias.get("relationrole2").toString();
				if ( ! po.equals("") ) {
 				   attr.addAttribute(addrURI, "RelatedAddressType", "addr:RelatedAddressType", "String", po);
				}
				dw.dataElement(addrURI, "RelatedAddressId", "", attr, s.toString());
			}
	
			s = row.get("addressxcoordinate");
			if (s != null && !s.toString().isEmpty() ) {
				dw.dataElement(addrURI, "AddressXCoordinate", s.toString());
			}
			s = row.get("addressycoordinate");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressYCoordinate", s.toString());
			}
			s = row.get("addresslongitude");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressLongitude", s.toString());
			}
			s = row.get("addresslatitude");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressLatitude", s.toString());
			}
			s = row.get("usnationalgridcoordinate");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "USNationalGridCoordinate", s.toString());
			}
			s = row.get("addresselevation");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressElevation", s.toString());
			}
			s = row.get("addresscoordinatreferencesystem");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressCoordinateReferenceSystem", s.toString());
			}
			s = row.get("addressparcelidentifiersource");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressParcelIdentifierSource", s.toString());
			}
			s = row.get("addressparcelidentifier");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressParcelIdentifier", s.toString());
			}
			s = row.get("addresstransportationsystemname");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressTransportationSystemName", s.toString());
			}
			s = row.get("addresstransportationsystemauthority");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressTransportationSystemAuthority", s.toString());
			}
			s = row.get("addresstransportationfeaturetype");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressTransportationFeatureType", s.toString());
			}
			s = row.get("addresstransportationfeatureid");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressTransportationFeatureId", s.toString());
			}
			s = row.get("relatedaddresstransportationfeatureid");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "RelatedAddressTransportationFeatureId", s.toString());
			}
			s = row.get("addressclassification");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressClassification", s.toString());
			}
			s = row.get("addressfeaturetype");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressFeatureType", s.toString());
			}
			s = row.get("addresslifecyclestatus");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressLifecycleStatus", s.toString());
			}
			s = row.get("officialstatus");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "OfficialStatus", s.toString());
			}
			s = row.get("addressanomalystatus");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressAnomalyStatus", s.toString());
			}
			s = row.get("addresssideofstreet");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressSideofStreet", s.toString());
			}
			s = row.get("addresszlevel");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressZLevel", s.toString());
			}
			s = row.get("addressfeaturetype");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressFeatureType", s.toString());
			}
			s = row.get("locationdescription");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "LocationDescription", s.toString());
			}
			s = row.get("addressfeaturetype");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressFeatureType", s.toString());
			}
			s = row.get("mailableaddress");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "MailableAddress", s.toString());
			}
			s = row.get("addressstartdate");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressStartDate", s.toString());
			}
			s = row.get("addressenddate");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressEndDate", s.toString());
			}
			s = row.get("datasetid");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "DatasetId", s.toString());
			}
			s = row.get("addressreferencesystemid");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressReferenceSystemId", s.toString());
			}
			s = row.get("addressreferencesystemauthority");
			if (s != null && ! s.toString().isEmpty()) {
				dw.dataElement(addrURI, "AddressReferenceSystemAuthority", s.toString());
			}
	
		} finally {
		}
	
	    dw.endElement(addrURI, addressClass);		
		return dw;
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		org.geotools.util.logging.Logging.GEOTOOLS
		.setLoggerFactory(org.geotools.util.logging.Log4JLoggerFactory
				.getInstance());
		org.apache.log4j.LogManager.getLogger("org.geotools").
			setLevel(org.apache.log4j.Level.OFF);

		Geodata g = new Geodata("AddressDB", "Addressdb");
		if (args.length > 0 && args[0].startsWith("-gui")) {
			try {
				AddressToolapp window = new AddressToolapp();
				window.open();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {

			try {
				if ( g.initProject() == 1 ) {
				if ( !g.operationMode.equals("init") ) {
					g.importGIS("data/Export_Output.shp");
					g.loadMaps("address");
					g.mapRawFlds("address");
					g.applyAbbr("address");
					g.applyTransforms("address");

		//			g.buildInfoTables("address", "MSAG_gr");

					g.runQA("address");

					if ( !g.operationMode.equals("qa") ) {
						if ( !g.operationMode.equals("exportfiles") ) {
				//			g.exportTable(g.workDir + "MSAG", "address.MSAG_gr");
							
						}

						if (g.operationMode.equals("publish") ) {
							// Document doc = null;
							// g.exportXML(doc);

							g.exportXML();
							
						}
					}
				}
				System.out.println("Complete");
				}

			} catch (SQLException e) {
				System.out.print(e.getMessage());
				e.printStackTrace();
			} catch (Exception e) {
				System.out.print(e.getMessage());
				e.printStackTrace();
			}
		}

	}



}
