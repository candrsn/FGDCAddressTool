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
import org.geotools.coverage.processing.operation.Log;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.shapefile.shp.JTSUtilities;
import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;

import com.vividsolutions.jts.geom.Geometry;



public class Geodata {
    static Connection dbconn = null;
    String  work_dir = "projects";
    String proj_name = null;
    String sproj_name = null;
    String operationMode = "";
    String evt_xml = "address_pkg.xml";
    String evt_met = "address_met.xml";
    String evt_rpt = "address_rpt.txt";
    String AddressData, AbbrvData, AddrFldMap, SubAddrFldMap, PlaceFldMap, AliasData, ExpertParsingData;
	String QATests, Transforms;
	
    public Geodata(String longName, String shortName) throws Exception {

    	// Get some default names
    	proj_name = longName;
		sproj_name = shortName;
		
		// Load the user settings
		getParams();
		
	}
    
	public void getParams() throws Exception {
        boolean paramsOk = true;
        String e = "\n\n";
        
		java.util.Properties configFile = new java.util.Properties();
		InputStream pf = this.getClass().getClassLoader().getResourceAsStream("AddressTool.properties");
		if ( pf != null ) {
			configFile.load(pf);
		
			operationMode = configFile.getProperty("OperationMode");
			work_dir = configFile.getProperty("work_dir");		
			proj_name = configFile.getProperty("ProjectName");	
			sproj_name = configFile.getProperty("ShortName");		
			AddressData = configFile.getProperty("AddressData");		
			AbbrvData = configFile.getProperty("AbbreviationsFile");		
			AddrFldMap = configFile.getProperty("AddressFieldMap");
			PlaceFldMap = configFile.getProperty("PlaceFieldMap");
			SubAddrFldMap = configFile.getProperty("SubAddressFieldMap");
			AliasData = configFile.getProperty("AliasData");

			ExpertParsingData = configFile.getProperty("ExpertParsingData");
			
			QATests = configFile.getProperty("QATests");
			Transforms = configFile.getProperty("Transforms");
			
			File f = new File(AliasData);
			if ( ! f.exists()) {
				e = e.concat("Address Alias Data "+ AliasData + " not found \n");
		    	paramsOk = false;
			}
			f = new File(AbbrvData);
			if ( ! f.exists()) {
				e = e.concat("Abbreviations Data "+ AbbrvData + " not found \n");
		    	paramsOk = false;
			}
			f = new File(AddrFldMap);
			if ( ! f.exists()) {
				e = e.concat("Address Field Mapping Data "+ AddrFldMap + " not found \n");
		    	paramsOk = false;
			}
			f = new File(SubAddrFldMap);
			if ( ! f.exists()) {
				e = e.concat("Occupancy Field Mapping Data "+ SubAddrFldMap + " not found \n");
		    	paramsOk = false;
			}
			f = new File(PlaceFldMap);
			if ( ! f.exists()) {
				e = e.concat("PlaceName Field Mapping Data "+ PlaceFldMap + " not found \n");
		    	paramsOk = false;

			}
			
		} else {
			
			AbbrvData = "resources/sql/common_abbr.csv";
			AddrFldMap = "resources/sql/mapaddress.csv";
			SubAddrFldMap = "resources/sql/mapsubaddress.csv";
			AliasData = "resources/sql/common_alias.csv";
			ExpertParsingData = null;

		}
		
		File f = new File(AddressData);
	    if (! f.exists()) {
	    	e.concat("Address Data "+ AddressData + " not found \n");
	    	paramsOk = false;
	    }
	    if (! operationMode.equals("publish") ) {
	    	e.concat("Operational Mode "+ operationMode + " not supported\n");
	    	paramsOk = false;
	    }
	    if (! paramsOk ) {
	    	throw new FileNotFoundException(e + "\n" + "   Exiting");
	    }
		evt_xml = sproj_name + "_pkg.xml";
		evt_met = sproj_name + "_met.xml";
		evt_rpt = sproj_name + "_rpt.txt";

	
	}
	
	public int Init() throws SQLException, Exception {
		int needInit = 1;
		File f = new File(work_dir + "/" + sproj_name + ".h2.db");
		
		if ( f.exists()) {
			needInit = 1;
			f.delete();
			f = new File(work_dir + "/" + sproj_name + ".h2.db");
		}
		if ( dbconn == null || ! dbconn.isValid(500) ) {
	  	  Class.forName("org.h2.Driver");
		  dbconn = DriverManager.getConnection("jdbc:h2:" + work_dir + "/" + sproj_name, "sa", "");

		  if (needInit == 1) {
		    createSupportTables();
		  }
		}
		return 1;
	}

	public int createSupportTables() throws Exception {
		 
			Statement stmt2 = dbconn.createStatement();
			
			File f = new File("resources/sql/support_tables.sql");
			if ( ! f.exists() ) {
				System.out.println("required table templates not found");
			
				return 0;
			}
			
			StringBuilder sqlText = new StringBuilder();
		    String NL = System.getProperty("line.separator");
		    Scanner scanner = new Scanner(new FileInputStream(f));
		    try {
		      while (scanner.hasNextLine()){
		        sqlText.append(scanner.nextLine() + NL);
		      }
		    }
		    finally{
		      scanner.close();
		    }
		    
			stmt2.execute(sqlText.toString());
			stmt2.execute("INSERT INTO project_log (log) values('Address Tool - 0.0.4')");
			stmt2.execute("INSERT INTO project_log (log) values('Project - " + proj_name + "')");
			stmt2.execute("INSERT INTO project_log (log) values('Short Name - " + sproj_name + "')");

			return 1;	 
	 }
	
	public int applyAbbr(String tbl) throws Exception {
		Statement stmt = dbconn.createStatement();
		String s = "";
		String[] aas = new String("PostAbbrev PreAbbrev").split(" ");
		
		for(int i =0; i < aas.length ; i++) {
			s = "";
			File af = new File("resources/sql/transforms/" + aas[i] + ".sql");
			if ( af.exists() ) {
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
		String[] tas = Transforms.split(" ");
		
		for(int i =0; i < tas.length ; i++) {
			s = "";
			File tf = new File("resources/sql/transforms/" + tas[i] + ".sql");
			if ( tf.exists() ) {
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
    
    public int runQA() throws Exception {
		Statement stmt = dbconn.createStatement();
		String s = "";
		String[] qas = QATests.split(" ");
		File qar = new File(work_dir + "/" + evt_rpt);
		FileWriter qafw = new FileWriter(qar);
		
		for(int i =0; i < qas.length ; i++) {
			s = "";
			File qf = new File("resources/sql/qa/" + qas[i] + ".sql");
			if ( qf.exists() ) {
			try {
			    BufferedReader in = new BufferedReader(new FileReader(qf));
			    String str;
			    while ((str = in.readLine()) != null) {
			        s = s.concat(str + "\n");
			    }
			    in.close();
			} catch (IOException e) {
			}
			
			String resv = "";
			ResultSet rsm = stmt.executeQuery(s);     
			if ( rsm.next() ) {
			  resv = rsm.getObject(1).toString();
			  }	
			System.out.println("QA Test " + qas[i] + " returned: " + resv);
			qafw.write("QA Test " + qas[i] + " returned: " + resv);
			}
		}
		if ( qafw != null) {
		  qafw.flush();
	  	  qafw.close();
		}
    	
    	return 1;
    }
    
    public static Integer h2_stmt(String smt) throws Exception {
			
			if ( dbconn.createStatement().execute(smt) )
				return 0;

			// return a failure code
			return 1;
		}
		
	public static SimpleFeatureSource readGIS(String shpFilepath) throws Exception {
			
		     SimpleFeatureSource featureSource = null;
		     File shpFile = new File(shpFilepath);

		     try {
		         if((! shpFile.exists()) && (! shpFile.isFile())) {
		             String message = "file is not found";
		   //          throw new FileNotFoundException(message);
		   // the geometry field is not strictly required          
		         }

		         Map<String, Serializable> connect = new HashMap<String, 
		Serializable>();
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

	public void importGIS(String shpFilePath) throws Exception {
	    int warningCount = 0;
		String swkt = null;
		
		SimpleFeatureSource featureSource =  readGIS(shpFilePath);
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
         
	     while (fTi.hasNext()){
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
		     if(! type.isPolygonType() && ! type.isLineType() && !type.isPointType()) {
		    	 System.out.println("The file contains unexpected geometry types");
				 warningCount++;
		         break;
		     }

		     if(geometry.isEmpty()) {
		         swkt = "";
		     } else {
		    	 swkt = geometry.toText();
		     }

		     Collection<Property> props = feature.getProperties();
		     
		     Iterator<Property> it = props.iterator();
             sep = "";
		     while(it.hasNext()) {
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
		
		h2_import("rawdata", "tmp/workread.csv");
		
		File f = new File("tmp/workread.csv");
		f.deleteOnExit();
		
		Statement stmt = dbconn.createStatement();
		stmt.execute("INSERT INTO table_info( id,name,role,status) values (0,'RAWDATA','address','R')");

}
		
	public static int h2_import(String ctable, String cfile) throws Exception {

			h2_stmt("Drop TABLE if Exists " + ctable + ";");
			h2_stmt("CREATE TABLE " + ctable + " as (SELECT *  FROM CSVREAD('" + cfile + "', null, 'fieldSeparator=' || CHAR(9)));");
			
			return 1;
		}

	public int LoadMaps(String tbl) throws Exception {
		Statement stmt = dbconn.createStatement();
		Scanner scanner;
		
		File f = new File("resources/sql/address_table_tmpl.sql");
		if ( ! f.exists() ) {
			BufferedReader fin = new BufferedReader(
				    new InputStreamReader(
				        this.getClass().getClassLoader().getResourceAsStream(
				            "resources/sql/address_table_tmpl.sql")));
			scanner = new Scanner(new FileInputStream(f));
		} else {
			scanner = new Scanner(new FileInputStream(f));   
		}
		
		StringBuilder sqlText = new StringBuilder();
	    String NL = System.getProperty("line.separator");
	    
        String s = null;
        
	    try {
	      while (scanner.hasNextLine()){
	    	sqlText.append(scanner.nextLine() + NL);
	      }
	    }
	    finally{
	      scanner.close();
	    }
	    s = sqlText.toString().replaceAll("%tablename%",tbl );
		stmt.execute(s);
		
		stmt.execute("INSERT INTO table_info( id,name,role,status) values (1,'"+ tbl + "_core" + "','address','I')");
		
		stmt.execute("INSERT INTO table_info( id,name,role,status) values (1,'"+ tbl + "_prelim" + "','address','I')");

		h2_import("abbrmap", AbbrvData);
		h2_import("addressmap", AddrFldMap);
	    h2_import("subaddressmap", SubAddrFldMap);
	    h2_import("aliasmap", AliasData);
	    h2_import("placemap", PlaceFldMap);
	    h2_import("expertparsing", ExpertParsingData);	    
	    	    
        return 1;		
	}
	
	public int MapRawFlds (String tbl) throws Exception {

		Statement stmt = dbconn.createStatement();
		Statement stmt2 = dbconn.createStatement();
		String s;

		//Build a SQL string to populate the intermediate format table		
		String si = "";
		String sr = "";
		String sep = "";
		String sra = "ADDRESSID";
        
        ResultSet rs = stmt.executeQuery("Select * from addressmap where tableid = " +
                  "(SELECT id from table_info where name = '" + tbl + "_prelim')");  
		
		while ( rs.next() ) {
			si = si + sep + rs.getObject("MAP");
			sr = sr + sep + rs.getObject("RAW");
			if (rs.getObject("MAP").toString().equals("ADDRESSID") ) {
				sra = rs.getObject("RAW").toString();
			}
			sep = " ,";
		}
		
		s = "INSERT INTO " + tbl + "_prelim" + "(" + si + " )" +
		   "(SELECT " + sr + " FROM RawData )";
		
		stmt.execute(s);
		
		// Now Subaddress elements
        rs = stmt.executeQuery("Select * from subaddressmap where tableid = " +
                "(SELECT id from table_info where name = '" + tbl + "_core')");  
        
        sep = ",";
		while ( rs.next() ) {
			si = "ADDRESSID" + sep + rs.getObject("MAP") + ",subaddresstype ";
			sr = sra + sep + rs.getObject("RAW") + ",'" + rs.getObject("RAW") + "'";
			
			s = "INSERT INTO " + tbl + "_subaddress" + "(" + si + " )" +
					   "(SELECT " + sr + " FROM RawData )";
			stmt2.execute(s);
		}
		
		stmt.execute("INSERT INTO " + tbl + "_core (SELECT * from " + tbl + "_prelim )" );
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
			Map<String, Object> rowplc,
			Map<String, Object> rowocc,
			Map<String, Object> rowalias ) {

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
			  if ( s != null ) {
			    cac = doc.createElement("AddressNumberPrefix");
			    cac.appendChild(doc.createTextNode(s.toString()));
                can.appendChild(cac);
			  } 
			  s = row.get("addressnumber");
			  if ( s != null ) {
			    cac = doc.createElement("AddressNumber");
			    cac.appendChild(doc.createTextNode(s.toString()));
                can.appendChild(cac);
			  } 
			  s = row.get("addressnumbersuffix");
			  if ( s != null ) {
			    cac = doc.createElement("AddressNumberSuffix");
			    cac.appendChild(doc.createTextNode(s.toString()));
                can.appendChild(cac);
			  } 
			address.appendChild(can);
			
			can = doc.createElement("CompleteStreetName");
			  s = row.get("StreetNamePreModifier");
			  if ( s != null ) {
			    cac = doc.createElement("StreetNamePreModifier");
			    cac.appendChild(doc.createTextNode(s.toString()));
                can.appendChild(cac);
			  } 
			  s = row.get("streetnamepredirectional");
			  if ( s != null ) {
			    cac = doc.createElement("StreetNamePreDirectional");
			    cac.appendChild(doc.createTextNode(s.toString()));
                can.appendChild(cac);
			  } 			
			  s = row.get("streetnamepretype");
			  if ( s != null ) {
			    cac = doc.createElement("StreetNamePreType");
			    cac.appendChild(doc.createTextNode(s.toString()));
                can.appendChild(cac);
			  }
			  s = row.get("streetname");
			  if ( s != null ) {
			    cac = doc.createElement("StreetName");
			    cac.appendChild(doc.createTextNode(s.toString()));
                can.appendChild(cac);
			  }
			  s = row.get("streetnameposttype");
			  if ( s != null ) {
			    cac = doc.createElement("StreetNamePostype");
			    cac.appendChild(doc.createTextNode(s.toString()));
                can.appendChild(cac);
			  }
			  s = row.get("streetnamepostdirectional");
			  if ( s != null ) {
			    cac = doc.createElement("StreetNamePostDirectional");
			    cac.appendChild(doc.createTextNode(s.toString()));
                can.appendChild(cac);
			  } 			
			  s = row.get("streetnamepostmodifier");
			  if ( s != null ) {
			    cac = doc.createElement("StreetNamePostModifier");
			    cac.appendChild(doc.createTextNode(s.toString()));
                can.appendChild(cac);
			  } 
			address.appendChild(can);
			
			can = doc.createElement("CompleteSubaddress");
			  s = row.get("addressid");
			  if ( s != null ) {
			    cac = doc.createElement("SubaddressElement");
			    cac.appendChild(doc.createTextNode(s.toString()));
			  } 
			  s = row.get("subaddresstype");
			  if ( s != null ) {
			    cac = doc.createElement("SubaddressType");
			    cac.appendChild(doc.createTextNode(s.toString()));
			  } 
			address.appendChild(can);			
			
			if ( row.get("PlaceStateZip") != null ) {
				can = doc.createElement("PlaceStateZip");
				can.appendChild(doc.createTextNode(s.toString()));
				  s = row.get("PlaceStateZip");
				  if ( s != null ) {
				    can.appendChild(doc.createTextNode(s.toString()));
				  } 
			} else {
			can = doc.createElement("CompletePlaceName");
			  s = rowplc.get("CommunityName");
			  if ( s != null ) {
			    cac = doc.createElement("PlaceName");
			    cac.appendChild(doc.createTextNode(s.toString()));
			    attr = doc.createAttribute("PlaceNameType");
				attr.setValue("USPSCommunity");
				cac.setAttributeNode(attr);
			  } 
			  s = rowplc.get("CityName");
			  if ( s != null ) {
			    cac = doc.createElement("PlaceName");
			    cac.appendChild(doc.createTextNode(s.toString()));
			    attr = doc.createAttribute("PlaceNameType");
				attr.setValue("MunicipalJurisdiction");
				cac.setAttributeNode(attr);
			  } 
			  s = rowplc.get("County");
			  if ( s != null ) {
			    cac = doc.createElement("PlaceName");
			    cac.appendChild(doc.createTextNode(s.toString()));
			    attr = doc.createAttribute("PlaceNameType");
				attr.setValue("County");
				cac.setAttributeNode(attr);
			  } 
				
  			    s = row.get("statename");
			    if ( s != null ) {
			      cac = doc.createElement("StateName");
			      cac.appendChild(doc.createTextNode(s.toString()));
			    } 
			    s = row.get("zipcode");
			    if ( s != null ) {
			      cac = doc.createElement("ZipCode");
			      cac.appendChild(doc.createTextNode(s.toString()));
			   } 
			    s = row.get("zipplus4");
			    if ( s != null ) {
			      cac = doc.createElement("ZipPlus4");
			      cac.appendChild(doc.createTextNode(s.toString()));
			   } 
			    s = row.get("countryname");
			    if ( s != null ) {
			      cac = doc.createElement("CountryName");
			      cac.appendChild(doc.createTextNode(s.toString()));
			   } 
			    
			}  
			address.appendChild(can);

			s = row.get("addressid");
			if ( s != null ) {
			  can = doc.createElement("AddressId");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			} 
			s = row.get("addressauthority");
			if ( s != null ) {
			  can = doc.createElement("AddressAuthority");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			} 
			s = row.get("addressauthority");
			if ( s != null ) {
			  can = doc.createElement("AddressAuthority");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			} 
					
			s = rowalias.get("relatedaddress1");
			if ( s != null ) {
			  can = doc.createElement("RelatedAddressID");
			  can.appendChild(doc.createTextNode(s.toString()));
			  attr = doc.createAttribute("RelatedAddressType");
			  attr.setValue(rowalias.get("relationrole1").toString());
			  can.setAttributeNode(attr);
			  address.appendChild(can);
			} 
			s = rowalias.get("relatedaddress2");
			if ( s != null ) {
			  can = doc.createElement("RelatedAddressID");
			  can.appendChild(doc.createTextNode(s.toString()));
			  attr = doc.createAttribute("RelatedAddressType");
			  attr.setValue(rowalias.get("relationrole2").toString());
			  can.setAttributeNode(attr);
			  address.appendChild(can);
			} 			
			
			s = row.get("addressxcoordinate");
			if ( s != null ) {
			  can = doc.createElement("AddressXCoordinate");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}			
			s = row.get("addressycoordinate");
			if ( s != null ) {
			  can = doc.createElement("AddressYCoordinate");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("addresslonigtude");
			if ( s != null ) {
			  can = doc.createElement("AddressLongitude");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			} 
			s = row.get("addresslatitude");
			if ( s != null ) {
			  can = doc.createElement("AddressLatitude");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			} 
			s = row.get("usnationalgridcoordinate");
			if ( s != null ) {
			  can = doc.createElement("USNationalGridCoordinate");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			} 
			s = row.get("addresselevation");
			if ( s != null ) {
			  can = doc.createElement("AddressElevation");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			} 
			s = row.get("addresscoordinatreferencesystem");
			if ( s != null ) {
			  can = doc.createElement("AddressCoordinateReferenceSystem");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			} 
			s = row.get("addressparcelidentifiersource");
			if ( s != null ) {
			  can = doc.createElement("AddressParcelIdentifierSource");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("addressparcelidentifier");
			if ( s != null ) {
			  can = doc.createElement("AddressParcelIdentifier");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("addresstransportationsystemname");
			if ( s != null ) {
			  can = doc.createElement("AddressTransportationSystemName");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("addresstransportationsystemauthority");
			if ( s != null ) {
			  can = doc.createElement("AddressTransportationSystemAuthority");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("addresstransportationfeaturetype");
			if ( s != null ) {
			  can = doc.createElement("AddressTransportationFeatureType");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("addresstransportationfeatureid");
			if ( s != null ) {
			  can = doc.createElement("AddressTransportationFeatureId");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("relatedaddresstransportationfeatureid");
			if ( s != null ) {
			  can = doc.createElement("RelatedAddressTransportationFeatureId");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("addressclassification");
			if ( s != null ) {
			  can = doc.createElement("AddressClassification");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("addressfeaturetype");
			if ( s != null ) {
			  can = doc.createElement("AddressFeatureType");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("addresslifecyclestatus");
			if ( s != null ) {
			  can = doc.createElement("AddressLifecycleStatus");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("officialstatus");
			if ( s != null ) {
			  can = doc.createElement("OfficialStatus");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("addressanomalystatus");
			if ( s != null ) {
			  can = doc.createElement("AddressAnomalyStatus");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("addresssideofstreet");
			if ( s != null ) {
			  can = doc.createElement("AddressSideofStreet");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("addresszlevel");
			if ( s != null ) {
			  can = doc.createElement("AddressZLevel");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("addressfeaturetype");
			if ( s != null ) {
			  can = doc.createElement("AddressFeatureType");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("locationdescription");
			if ( s != null ) {
			  can = doc.createElement("LocationDescription");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("addressfeaturetype");
			if ( s != null ) {
			  can = doc.createElement("AddressFeatureType");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("mailableaddress");
			if ( s != null ) {
			  can = doc.createElement("MailableAddress");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("addressstartdate");
			if ( s != null ) {
			  can = doc.createElement("AddressStartDate");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("addressenddate");
			if ( s != null ) {
			  can = doc.createElement("AddressEndDate");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("datasetid");
			if ( s != null ) {
			  can = doc.createElement("DatasetId");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("addressreferencesystemid");
			if ( s != null ) {
			  can = doc.createElement("AddressReferenceSystemId");
			  can.appendChild(doc.createTextNode(s.toString()));
			  address.appendChild(can);
			}
			s = row.get("addressreferencesystemauthority");
			if ( s != null ) {
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
			StreamResult result = new StreamResult(new File(work_dir + "/" + evt_xml));

			// Output to console for testing
			// StreamResult result = new StreamResult(System.out);

			transformer.transform(source, result);

			System.out.println("Local copy of xml file saved");

		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		}

		return 1;
	}

	public Document export_address(Document doc) throws Exception {

		Statement stmt = dbconn.createStatement();
		Statement stmt2 = dbconn.createStatement();
		
		String sql = "";
		
		// 
		ResultSet srs = stmt.executeQuery("SELECT * from address_core");
		ResultSetMetaData rsm = srs.getMetaData();
		
		// Start Feed
		Map<String, Object> row = new HashMap<String, Object >();
		Map<String, Object> rowplc = new HashMap<String, Object >();
		Map<String, Object> rowocc = new HashMap<String, Object >();
		Map<String, Object> rowalias = new HashMap<String, Object >();

		
		while ( srs.next() ) {
			row.clear();
			for ( int i = 1; i <= rsm.getColumnCount(); i++ ) {
				row.put(rsm.getColumnName(i).toLowerCase(), srs.getObject(i) );
			}
			if ( srs.isFirst() )
				doc = writeXMLHdr(row);
			
			doc = writeXMLRow(doc, row, rowplc, rowocc, rowalias);
		}

		writeXMLFtr(doc);
		
		srs = stmt.executeQuery("SELECT count(*) from address_core");
		srs.first();
		String recs;
		if ( srs.isFirst() ) {
    		 recs = srs.getObject(1).toString();
	   	    /* log the export */		

		    boolean srs2 = stmt2.execute("INSERT INTO publication_info (pub_records) values " +
                "(" + recs + ")");
		}
		
		return doc;

	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		Geodata g = new Geodata("AddressDB", "Addressdb");
        
	    try {				
			g.Init();
		    g.importGIS("data/Export_Output.shp");
		    g.LoadMaps("address");
		    g.MapRawFlds("address");
		    g.applyAbbr("address");
		    g.applyTransforms("address");
		    
		    g.runQA();
		    
			Document doc = null;
			g.export_address(doc);

		    System.out.println("Complete"); 
		    
		} catch (SQLException e) {
			System.out.print(e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {		   
			System.out.print(e.getMessage());
			e.printStackTrace();
		}

 
	};

}
