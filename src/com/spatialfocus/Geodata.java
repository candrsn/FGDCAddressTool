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

import org.h2.tools.Csv;
import org.h2.util.DateTimeUtils;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;

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
	
    public Geodata(String longName, String shortName) throws Exception {

		proj_name = longName;
		sproj_name = shortName;
		getParams();
	}
    
	public void getParams() throws Exception {

		java.util.Properties configFile = new java.util.Properties();
		InputStream pf = this.getClass().getClassLoader().getResourceAsStream("AddressTool.properties");
		if ( pf != null ) {
			configFile.load(pf);
		
			operationMode = configFile.getProperty("operationMode");

			work_dir = configFile.getProperty("work_dir");		
		
		}
	
	}
	public int Init() throws SQLException, Exception {
		int needInit = 1;
		File f = new File(work_dir + "/" + sproj_name + ".h2.db");
		
		if ( f.exists()) {
			needInit = 0;
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
			
			File f = new File("sql/support_tables.sql");
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
	
	public static Integer h2_stmt(String smt) throws Exception {
			
			if ( dbconn.createStatement().execute(smt) )
				return 0;

			// return a failure code
			return 1;
		}
		
	public static SimpleFeatureSource readSHP(String shpFilepath) throws Exception {
			
		     SimpleFeatureSource featureSource = null;
		     File shpFile = new File(shpFilepath);

		     try {
		         if((! shpFile.exists()) && (! shpFile.isFile())) {
		             String message = "SHP file is not found";
		             throw new FileNotFoundException(message);
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

	static void importSHP(String shpFilePath) throws Exception {
	    int warningCount = 0;
		String swkt = null;
		
		SimpleFeatureSource featureSource =  readSHP(shpFilePath);
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
		    	 System.out.println("The SHPfile contains unexpected geometry types");
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
		
		h2_import("RawData", "tmp/workread.csv");
		
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
		
		File f = new File("sql/address_table_tmpl.sql");
		if ( ! f.exists() ) {
			BufferedReader fin = new BufferedReader(
				    new InputStreamReader(
				        this.getClass().getClassLoader().getResourceAsStream(
				            "sql/address_table_tmpl.sql")));
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
	    s = sqlText.toString().replaceAll("%tablename%",tbl);
		stmt.execute(s);
		
		stmt.execute("INSERT INTO table_info( id,name,role,status) values (1,'"+ tbl + "','address','I')");

		h2_import("addressmap", "sql/mapaddress.csv");
		return h2_import("subaddressmap", "sql/mapsubaddress.csv");
		
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
                  "(SELECT id from table_info where name = '" + tbl + "')");  
		
		while ( rs.next() ) {
			si = si + sep + rs.getObject("MAP");
			sr = sr + sep + rs.getObject("RAW");
			if (rs.getObject("MAP").toString().equals("ADDRESSID") ) {
				sra = rs.getObject("RAW").toString();
			}
			sep = " ,";
		}
		
		s = "INSERT INTO " + tbl + "_core" + "(" + si + " )" +
		   "(SELECT " + sr + " FROM RawData )";
		
		stmt.execute(s);
		
		// Now Subaddress elements
        rs = stmt.executeQuery("Select * from subaddressmap where tableid = " +
                "(SELECT id from table_info where name = '" + tbl + "')");  
        
        sep = ",";
		while ( rs.next() ) {
			si = "ADDRESSID" + sep + rs.getObject("MAP") + ",subaddresstype ";
			sr = sra + sep + rs.getObject("RAW") + ",'" + rs.getObject("RAW") + "'";
			
			s = "INSERT INTO " + tbl + "_subaddress" + "(" + si + " )" +
					   "(SELECT " + sr + " FROM RawData )";
			stmt2.execute(s);

		}
		
		return 1;
		
	}
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		Geodata g = new Geodata("TestDB", "testdb");
        
	    try {				
			g.Init();
		} catch (SQLException e) {

			e.printStackTrace();
		} catch (Exception e) {		   
			e.printStackTrace();
		}
	    
	    importSHP("/home/candrsn/data/sfi/cap/test/Export_Output.shp");
	    g.LoadMaps("itab1");
	    g.MapRawFlds("itab1");
	    
	    System.out.println("Complete");  
	};

}
