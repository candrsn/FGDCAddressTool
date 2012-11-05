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

import org.geotools.*;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.shapefile.shp.JTSUtilities;
import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;

import com.spatialfocus.gui.AddressToolapp;
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
		File pfil = new File("AddressTool.properties");
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
		if ( f.mkdir() ) {
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
		stmt2.execute("INSERT INTO project_log (log) values('Address Tool - 0.0.4')");
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

	public static Integer h2_stmt(String smt) throws Exception {

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
		FileWriter tw = new FileWriter("tmp/workread.csv");
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

		iterator = collection.features();
		while (iterator.hasNext()) {

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

		h2_import("rawdata", "tmp/workread.csv");

		File f = new File("tmp/workread.csv");
		f.deleteOnExit();

		Statement stmt = dbconn.createStatement();
		stmt.execute("INSERT INTO table_info( id,name,role,status) values (0,'RAWDATA','address','R')");

	}

	public static int h2_import(String ctable, String cfile) throws Exception {

		h2_stmt("Drop TABLE if Exists " + ctable + ";");
		h2_stmt("CREATE TABLE " + ctable + " as (SELECT *  FROM CSVREAD('"
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

		h2_import("abbrmap", abbreviationDataTable);
		h2_import("addressmap", addressFieldMap);
		h2_import("subaddressmap", subAddrFldMap);
		h2_import("aliasmap", aliasDataTable);
		h2_import("placemap", placeFldMap);
		h2_import("expertparsing", expertParsingData);

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

		stmt.execute("INSERT INTO " + tbl + "_core (SELECT * from " + tbl
				+ "_prelim )");
		return 1;

	}

	public Document writeXMLHdr(Map<String, Object> row) throws Exception {

		Document doc = null;
		try {

			DocumentBuilderFactory docFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// root elements
			doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("AddressCollection");
			doc.appendChild(rootElement);

			Attr attr = doc.createAttribute("version");
			attr.setValue("0.4");
			rootElement.setAttributeNode(attr);

			attr = doc.createAttribute("xmlns:addr");
			attr.setValue("http://www.fgdc.gov/schemas/address/addr");
			rootElement.setAttributeNode(attr);

			attr = doc.createAttribute("xmlns:addr_type");
			attr.setValue("http://www.fgdc.gov/schemas/address/addr_type");
			rootElement.setAttributeNode(attr);

			attr = doc.createAttribute("xmlns:gml");
			attr.setValue("http://www.fgdc.gov/schemas/address/addr_type");
			rootElement.setAttributeNode(attr);

			attr = doc.createAttribute("xmlns:smil20");
			attr.setValue("http://www.w3.org/2001/SMIL20/");
			rootElement.setAttributeNode(attr);

			attr = doc.createAttribute("xmlns:smil20lang");
			attr.setValue("http://www.w3.org/2001/SMIL20/Language");
			rootElement.setAttributeNode(attr);

			attr = doc.createAttribute("xmlns:xlink");
			attr.setValue("http://www.w3.org/1999/xlink");
			rootElement.setAttributeNode(attr);

			attr = doc.createAttribute("xmlns:xml");
			attr.setValue("http://www.w3.org/XML/1998/namespace");
			rootElement.setAttributeNode(attr);

			attr = doc.createAttribute("xmlns:xsi");
			attr.setValue("http://www.w3.org/2001/XMLSchema-instance");
			rootElement.setAttributeNode(attr);

			attr = doc.createAttribute("xsi:schemaLocation");
			attr.setValue("http://www.fgdc.gov/schemas/address/addr addr.xsd");
			rootElement.setAttributeNode(attr);

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		}

		return doc;

	};

	public Document writeXMLRow(Document doc, Map<String, Object> row,
			Map<String, Object> rowplc, Map<String, Object> rowocc,
			Map<String, Object> rowalias) {

		Object s = null;
		try {

			Element rootElement = doc.getDocumentElement();
			// incident elements
			Element address = doc.createElement("NumberedThoroughfareAddress");
			rootElement.appendChild(address);

			Element can, cac;
			Attr attr;

			can = doc.createElement("CompleteAddressNumber");
			s = row.get("addressnumberprefix");
			if (s != null) {
				cac = doc.createElement("AddressNumberPrefix");
				cac.appendChild(doc.createTextNode(s.toString()));
				can.appendChild(cac);
			}
			s = row.get("addressnumber");
			if (s != null) {
				cac = doc.createElement("AddressNumber");
				cac.appendChild(doc.createTextNode(s.toString()));
				can.appendChild(cac);
			}
			s = row.get("addressnumbersuffix");
			if (s != null) {
				cac = doc.createElement("AddressNumberSuffix");
				cac.appendChild(doc.createTextNode(s.toString()));
				can.appendChild(cac);
			}
			address.appendChild(can);

			can = doc.createElement("CompleteStreetName");
			s = row.get("StreetNamePreModifier");
			if (s != null) {
				cac = doc.createElement("StreetNamePreModifier");
				cac.appendChild(doc.createTextNode(s.toString()));
				can.appendChild(cac);
			}
			s = row.get("streetnamepredirectional");
			if (s != null) {
				cac = doc.createElement("StreetNamePreDirectional");
				cac.appendChild(doc.createTextNode(s.toString()));
				can.appendChild(cac);
			}
			s = row.get("streetnamepretype");
			if (s != null) {
				cac = doc.createElement("StreetNamePreType");
				cac.appendChild(doc.createTextNode(s.toString()));
				can.appendChild(cac);
			}
			s = row.get("streetname");
			if (s != null) {
				cac = doc.createElement("StreetName");
				cac.appendChild(doc.createTextNode(s.toString()));
				can.appendChild(cac);
			}
			s = row.get("streetnameposttype");
			if (s != null) {
				cac = doc.createElement("StreetNamePostype");
				cac.appendChild(doc.createTextNode(s.toString()));
				can.appendChild(cac);
			}
			s = row.get("streetnamepostdirectional");
			if (s != null) {
				cac = doc.createElement("StreetNamePostDirectional");
				cac.appendChild(doc.createTextNode(s.toString()));
				can.appendChild(cac);
			}
			s = row.get("streetnamepostmodifier");
			if (s != null) {
				cac = doc.createElement("StreetNamePostModifier");
				cac.appendChild(doc.createTextNode(s.toString()));
				can.appendChild(cac);
			}
			address.appendChild(can);

			can = doc.createElement("CompleteSubaddress");
			s = row.get("addressid");
			if (s != null) {
				cac = doc.createElement("SubaddressElement");
				cac.appendChild(doc.createTextNode(s.toString()));
			}
			s = row.get("subaddresstype");
			if (s != null) {
				cac = doc.createElement("SubaddressType");
				cac.appendChild(doc.createTextNode(s.toString()));
			}
			address.appendChild(can);

			if (row.get("PlaceStateZip") != null) {
				can = doc.createElement("PlaceStateZip");
				can.appendChild(doc.createTextNode(s.toString()));
				s = row.get("PlaceStateZip");
				if (s != null) {
					can.appendChild(doc.createTextNode(s.toString()));
				}
			} else {
				can = doc.createElement("CompletePlaceName");
				s = rowplc.get("CommunityName");
				if (s != null) {
					cac = doc.createElement("PlaceName");
					cac.appendChild(doc.createTextNode(s.toString()));
					attr = doc.createAttribute("PlaceNameType");
					attr.setValue("USPSCommunity");
					cac.setAttributeNode(attr);
				}
				s = rowplc.get("CityName");
				if (s != null) {
					cac = doc.createElement("PlaceName");
					cac.appendChild(doc.createTextNode(s.toString()));
					attr = doc.createAttribute("PlaceNameType");
					attr.setValue("MunicipalJurisdiction");
					cac.setAttributeNode(attr);
				}
				s = rowplc.get("County");
				if (s != null) {
					cac = doc.createElement("PlaceName");
					cac.appendChild(doc.createTextNode(s.toString()));
					attr = doc.createAttribute("PlaceNameType");
					attr.setValue("County");
					cac.setAttributeNode(attr);
				}

				s = row.get("statename");
				if (s != null) {
					cac = doc.createElement("StateName");
					cac.appendChild(doc.createTextNode(s.toString()));
				}
				s = row.get("zipcode");
				if (s != null) {
					cac = doc.createElement("ZipCode");
					cac.appendChild(doc.createTextNode(s.toString()));
				}
				s = row.get("zipplus4");
				if (s != null) {
					cac = doc.createElement("ZipPlus4");
					cac.appendChild(doc.createTextNode(s.toString()));
				}
				s = row.get("countryname");
				if (s != null) {
					cac = doc.createElement("CountryName");
					cac.appendChild(doc.createTextNode(s.toString()));
				}

			}
			address.appendChild(can);

			s = row.get("addressid");
			if (s != null) {
				can = doc.createElement("AddressId");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addressauthority");
			if (s != null) {
				can = doc.createElement("AddressAuthority");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addressauthority");
			if (s != null) {
				can = doc.createElement("AddressAuthority");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}

			s = rowalias.get("relatedaddress1");
			if (s != null) {
				can = doc.createElement("RelatedAddressID");
				can.appendChild(doc.createTextNode(s.toString()));
				attr = doc.createAttribute("RelatedAddressType");
				attr.setValue(rowalias.get("relationrole1").toString());
				can.setAttributeNode(attr);
				address.appendChild(can);
			}
			s = rowalias.get("relatedaddress2");
			if (s != null) {
				can = doc.createElement("RelatedAddressID");
				can.appendChild(doc.createTextNode(s.toString()));
				attr = doc.createAttribute("RelatedAddressType");
				attr.setValue(rowalias.get("relationrole2").toString());
				can.setAttributeNode(attr);
				address.appendChild(can);
			}

			s = row.get("addressxcoordinate");
			if (s != null) {
				can = doc.createElement("AddressXCoordinate");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addressycoordinate");
			if (s != null) {
				can = doc.createElement("AddressYCoordinate");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addresslonigtude");
			if (s != null) {
				can = doc.createElement("AddressLongitude");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addresslatitude");
			if (s != null) {
				can = doc.createElement("AddressLatitude");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("usnationalgridcoordinate");
			if (s != null) {
				can = doc.createElement("USNationalGridCoordinate");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addresselevation");
			if (s != null) {
				can = doc.createElement("AddressElevation");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addresscoordinatreferencesystem");
			if (s != null) {
				can = doc.createElement("AddressCoordinateReferenceSystem");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addressparcelidentifiersource");
			if (s != null) {
				can = doc.createElement("AddressParcelIdentifierSource");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addressparcelidentifier");
			if (s != null) {
				can = doc.createElement("AddressParcelIdentifier");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addresstransportationsystemname");
			if (s != null) {
				can = doc.createElement("AddressTransportationSystemName");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addresstransportationsystemauthority");
			if (s != null) {
				can = doc.createElement("AddressTransportationSystemAuthority");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addresstransportationfeaturetype");
			if (s != null) {
				can = doc.createElement("AddressTransportationFeatureType");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addresstransportationfeatureid");
			if (s != null) {
				can = doc.createElement("AddressTransportationFeatureId");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("relatedaddresstransportationfeatureid");
			if (s != null) {
				can = doc
						.createElement("RelatedAddressTransportationFeatureId");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addressclassification");
			if (s != null) {
				can = doc.createElement("AddressClassification");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addressfeaturetype");
			if (s != null) {
				can = doc.createElement("AddressFeatureType");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addresslifecyclestatus");
			if (s != null) {
				can = doc.createElement("AddressLifecycleStatus");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("officialstatus");
			if (s != null) {
				can = doc.createElement("OfficialStatus");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addressanomalystatus");
			if (s != null) {
				can = doc.createElement("AddressAnomalyStatus");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addresssideofstreet");
			if (s != null) {
				can = doc.createElement("AddressSideofStreet");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addresszlevel");
			if (s != null) {
				can = doc.createElement("AddressZLevel");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addressfeaturetype");
			if (s != null) {
				can = doc.createElement("AddressFeatureType");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("locationdescription");
			if (s != null) {
				can = doc.createElement("LocationDescription");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addressfeaturetype");
			if (s != null) {
				can = doc.createElement("AddressFeatureType");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("mailableaddress");
			if (s != null) {
				can = doc.createElement("MailableAddress");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addressstartdate");
			if (s != null) {
				can = doc.createElement("AddressStartDate");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addressenddate");
			if (s != null) {
				can = doc.createElement("AddressEndDate");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("datasetid");
			if (s != null) {
				can = doc.createElement("DatasetId");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addressreferencesystemid");
			if (s != null) {
				can = doc.createElement("AddressReferenceSystemId");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}
			s = row.get("addressreferencesystemauthority");
			if (s != null) {
				can = doc.createElement("AddressReferenceSystemAuthority");
				can.appendChild(doc.createTextNode(s.toString()));
				address.appendChild(can);
			}

		} finally {
		}

		return doc;
	}

	public int writeXMLFtr(Document doc) throws Exception {

		try {
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(workDir + "/"
					+ evt_xml));

			// Output to console for testing
			// StreamResult result = new StreamResult(System.out);

			transformer.transform(source, result);

			System.out.println("Local copy of xml file saved");

		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		}

		return 1;
	}

	public Document exportXML(Document doc) throws Exception {

		Statement stmt = dbconn.createStatement();
		Statement stmt2 = dbconn.createStatement();

		String sql = "";

		//
		ResultSet srs = stmt.executeQuery("SELECT * from address_core");
		ResultSetMetaData rsm = srs.getMetaData();

		// Start Feed
		Map<String, Object> row = new HashMap<String, Object>();
		Map<String, Object> rowplc = new HashMap<String, Object>();
		Map<String, Object> rowocc = new HashMap<String, Object>();
		Map<String, Object> rowalias = new HashMap<String, Object>();

		while (srs.next()) {
			row.clear();
			for (int i = 1; i <= rsm.getColumnCount(); i++) {
				row.put(rsm.getColumnName(i).toLowerCase(), srs.getObject(i));
			}
			if (srs.isFirst())
				doc = writeXMLHdr(row);

			doc = writeXMLRow(doc, row, rowplc, rowocc, rowalias);
		}

		writeXMLFtr(doc);

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

		return doc;

	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

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
				if ( g.initProject() != 1 ) {
				if (g.operationMode != "init") {
					g.importGIS("data/Export_Output.shp");
					g.loadMaps("address");
					g.mapRawFlds("address");
					g.applyAbbr("address");
					g.applyTransforms("address");

		//			g.buildInfoTables("address", "MSAG_gr");

					g.runQA("address");

					if (g.operationMode != "qa") {
						if (g.operationMode == "exportfiles") {
				//			g.exportTable(g.workDir + "MSAG", "address.MSAG_gr");
							
						}

						if (g.operationMode == "publish") {
							Document doc = null;
							g.exportXML(doc);
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
