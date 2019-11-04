import java.sql.*;  
import java.math.BigDecimal;
import java.io.*;
import java.util.*;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.simple.*;

public class MysqlCon
{  
	public Connection getMySQLconnnection() throws Exception 
	{
		FileReader reader=new FileReader("db.properties");  
		Properties p=new Properties();  
		p.load(reader);  

		Class.forName(p.getProperty("mysql_driver"));
		return DriverManager.getConnection(p.getProperty("URL"),p.getProperty("Username"),p.getProperty("Password"));
	}
	
	/*This method returns number of Active DB connections*/	
	public String getActiveNumberOfConnections(Connection conn) throws Exception
	{
		String query = "SHOW GLOBAL STATUS LIKE '%Threads_running%'";
		return (String)getRequiredDBDetails(conn, "Value", query);
	}	

	/*This method returns number of DB connections*/	
	public String getNumberOfConnections(Connection conn) throws Exception
	{
		String query = "SHOW GLOBAL STATUS LIKE '%Threads_connected%'";
		return (String)getRequiredDBDetails(conn, "Value", query);
	}
	
	/*This method returns Free Space available*/
	public BigDecimal getFreeStorageSpaceInMB(Connection conn, String DBName) throws Exception
	{
		String query = "SELECT sum( data_free )/ 1024 / 1024 'Reclaimable Space in MB' FROM information_schema.TABLES where table_schema='mysql'";
		return (BigDecimal)getRequiredDBDetails(conn, "Reclaimable Space in MB", query);
	}
	/*This method returns write operations in Bytes*/
	public BigDecimal getDbSizeInMB(Connection conn, String DBName) throws Exception
	{
		String query = "SELECT sum( data_length + index_length ) / 1024 / 1024 'Database Size in MB' FROM information_schema.TABLES where table_schema='mysql'";
		return (BigDecimal)getRequiredDBDetails(conn, "Database Size in MB", query);
	}

	/*This method returns write operations in Bytes*/
	public String getIOReadWriteOperations(Connection conn, boolean read) throws Exception
	{
		String query = "SHOW GLOBAL STATUS LIKE 'Innodb_data_reads'";
		if(read)
			query = "SHOW GLOBAL STATUS LIKE 'Innodb_data_writes'";
		
		return (String)getRequiredDBDetails(conn, "Value", query);
	}
	
	/*This method returns value from database based on column name and mysql query*/
	public Object getRequiredDBDetails(Connection conn, String colSelect, String query) throws Exception { 
		Statement stmt 		= conn.createStatement();  
		ResultSet rs 		= stmt.executeQuery(query);
		Object returnVal 	= null;
		while(rs.next()) 
		{
			returnVal = rs.getObject(colSelect);
		}
		stmt.close();
		return returnVal;
	}	
	/*This method gives the CPU load with given process name*/
	public double getCPULoadByProcessName(String processName) throws Exception
	{
		String process;
		StringBuilder cmd = new StringBuilder("typeperf");
		cmd.append(" \"\\Process(");
		cmd.append(processName);
		cmd.append("*)");
		cmd.append("\\");
		cmd.append("%");
		cmd.append(" Processor Time\" ");
		cmd.append(" -sc 1");
		
		Process p = Runtime.getRuntime().exec(cmd.toString()); 
		LineNumberReader input = new  LineNumberReader(new InputStreamReader(p.getInputStream()));
		String[] arrOfStr = new String[10];
		while ((process = input.readLine()) != null) {
			if(input.getLineNumber() == 3)
			{
				process = process.replace('"',' ');
				arrOfStr = process.split(",");
			}
		} 
		double percentage = 0.0;
		for (int i = 1;i < arrOfStr.length; i++) {
			 percentage = percentage + Double.valueOf(arrOfStr[i]);
		}			
		input.close();
		return percentage;
	}
	
	public boolean isWebsiteActive(String targetUrl) throws Exception
	{
		HttpURLConnection httpUrlConn = null;
		try 
		{
			httpUrlConn =  (HttpURLConnection) new URL(targetUrl).openConnection();
			return (httpUrlConn.getResponseCode() == HttpURLConnection.HTTP_OK);
		}catch(Exception malformedURL) {
			//malURL.printStackTrace();
			return false;
		}
	}

	public JSONObject databaseMonitoring() throws Exception
	{
		Connection con = getMySQLconnnection();
		JSONObject obj = new JSONObject();
		obj.put("DatabaseName", "apac_bot_instance");
		obj.put("Connections", getNumberOfConnections(con));
		obj.put("Active Connection", getActiveNumberOfConnections(con));
		obj.put("Database Size(MB)", getDbSizeInMB(con, "apac_bot_instance"));
		obj.put("Free Memory Space(MB)", getFreeStorageSpaceInMB(con, "apac_bot_instance"));
		obj.put("CPU Load", getCPULoadByProcessName("mysqld"));
		System.out.print(obj);
		con.close();
		return obj;
	}
		
	public static void main(String args[]) throws Exception
	{  
		MysqlCon mysql = new MysqlCon();
		/*System.out.println("Connected to database");
 		System.out.println("Number of connections :::"+mysql.getNumberOfConnections(con));
		System.out.println("Number of Active connections :::"+mysql.getActiveNumberOfConnections(con));
		System.out.println("Free Memory Space(MB):::"+mysql.getFreeStorageSpaceInMB(con, "apac_bot_instance"));
		System.out.println("Database size(MB):::"+mysql.getDbSizeInMB(con, "apac_bot_instance"));
		System.out.println("MySql CPU load :::"+mysql.getCPULoadByProcessName("mysqld"));
		System.out.println("Chrome CPU load :::"+mysql.getCPULoadByProcessName("Chrome"));
		*/
		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++");
		System.out.println("Website Active :::"+mysql.isWebsiteActive("Chrome"));
		System.out.println("Website Active :::"+mysql.isWebsiteActive("http://www.test.org/"));
		System.out.println("Website Active :::"+mysql.isWebsiteActive("http://www.google.com/"));
		System.out.println("Website Active :::"+mysql.isWebsiteActive("http://www.google1.com/"));
		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++");
		mysql.databaseMonitoring();
	}			
}


