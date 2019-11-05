package dmpfileinventory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

public class MonitorDirectory
{
    private static String OS = "W";//"L" : W = Windows, L = Linux
    private static String DB_CONN_URL = null;
    private static String DB_USER = null;
    private static String DB_PASS = null;
    private static String SRC_FOLDER = null;
    private static final Logger LOGGER = Logger.getLogger(MonitorDirectory.class);
    
    public static void main(String[] args)
    {
        createLog();
        LOGGER.info("File inventory started");
        
        readConfigFile();
        readDirectory();
    }
    
    private static void createLog()
    {
        // creates pattern layout
        PatternLayout layout = new PatternLayout();
        String conversionPattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%t] %c %x - %m%n";
        layout.setConversionPattern(conversionPattern);
        
        // creates daily rolling file appender
        RollingFileAppender rollingAppender = new RollingFileAppender();
        rollingAppender.setFile("./logs/app_file_inventory.log");
        rollingAppender.setLayout(layout);
        rollingAppender.activateOptions();
        rollingAppender.setMaxFileSize("10MB");
        rollingAppender.setMaxBackupIndex(4);
        
        // configures the root logger
        Logger rootLogger = Logger.getRootLogger();
        rootLogger.setLevel(Level.DEBUG);
        rootLogger.addAppender(rollingAppender);
    }
    
    private static void readConfigFile()
    { 
        BufferedReader reader = null;
        String[] splitLine;
        
        try
        {
            String absPath = new File(MonitorDirectory.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
            
            String line = null;
            
            if("L".equals(OS))
            {
                reader = new BufferedReader(new FileReader(absPath.substring(0, absPath.length() - "dmpFileInventory.jar".length())+ "/config.txt"));
            }
            else
            {
                reader = new BufferedReader(new FileReader("F:\\projects\\dmp\\config.txt"));
            }
            
            while((line = reader.readLine()) != null)
            {
                splitLine = line.split("#");
                
                if("SRC_FOLDER".equals(splitLine[0]))
                {
                    SRC_FOLDER = splitLine[1];
                }
                else if("DB_CONN_URL".equals(splitLine[0]))
                {
                    DB_CONN_URL = splitLine[1];
                }
                else if("DB_USER".equals(splitLine[0]))
                {
                    DB_USER = splitLine[1];
                }
                else if("DB_PASS".equals(splitLine[0]))
                {
                    DB_PASS = splitLine[1];
                }
            }
            
            reader.close();
        }
        catch (Exception ex)
        {
            LOGGER.error("Read Config File# " + ex.toString());
            saveErrorLog("Read Config File", ex.toString());
            System.out.println(ex.toString());
        }
        finally
        {
            try
            {
                reader.close();
            }
            catch (IOException ex)
            {
                LOGGER.error("Read Config File# " + ex.toString());
                saveErrorLog("Read Config File", ex.toString());
                System.out.println(ex.toString());
            }
        }
    }
    
    public static void readDirectory()
    {
        try
        {
            String osSlash = "";
            
            if("L".equals(OS))
            {
                osSlash = "/";
            }
            else if("W".equals(OS))
            {
                osSlash = "\\";
            }
            
            //Path faxFolder = Paths.get("./fax/");
            Path faxFolder = Paths.get(SRC_FOLDER);
            WatchService watchService = FileSystems.getDefault().newWatchService();
            faxFolder.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            
            boolean valid = true;
            String fileName = null;
            File file = null;
            
            do
            {
                WatchKey watchKey = watchService.take();
                
                for (WatchEvent event : watchKey.pollEvents())
                {
                    //WatchEvent.Kind kind = event.kind();
                    if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind()))
                    {
                        fileName = event.context().toString();
                        //System.out.println("File Name:" + fileName);
                        
                        file = new File(SRC_FOLDER + osSlash + fileName);
                        System.out.println("File Size:" + file.length());
                        
                        //attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                        //FileTime time = attrs.creationTime();
                        
                        /*
                        String pattern = "yyyy-MM-dd HH:mm:ss";
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
                        String formatted = simpleDateFormat.format(new Date(file.lastModified()));
                        System.out.println( "The file creation date and time is: " + formatted );
                        */
                        
                        saveRegister(fileName, new Timestamp(file.lastModified()), file.length());
                    }
                }
                
                valid = watchKey.reset();
                
                TimeUnit.SECONDS.sleep(2);
                
            } while (valid);
        }
        catch (IOException | InterruptedException ex)
        {
            LOGGER.error("Read Directory# " + ex.toString());
            saveErrorLog("Read Directory", ex.toString());
            System.out.println(ex.toString());
        }
    }
    
    public static void saveRegister(String fileName, Timestamp lastModified, long size)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            
            //Insert data
            try (Connection conn = DriverManager.getConnection(DB_CONN_URL, DB_USER, DB_PASS))
            {
                String query = " insert into file_register (name, last_modified_at, size, total_line, status, created_at, updated_at)"
                        + " values (?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement preparedStmt = conn.prepareStatement(query);
                preparedStmt.setString(1, fileName);
                preparedStmt.setTimestamp(2, lastModified);
                preparedStmt.setLong(3, size);
                preparedStmt.setInt(4, 0);
                preparedStmt.setInt(5, FileStatus.NEW.ordinal());
                
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                
                preparedStmt.setTimestamp(6, timestamp);
                preparedStmt.setTimestamp(7, timestamp);
                
                preparedStmt.execute();
            }
        }
        catch (ClassNotFoundException | SQLException ex)
        {
            LOGGER.error("Save Register# " + ex.toString());
            System.out.println(ex.toString());
        }
    }
    
    public static void saveErrorLog(String errorFor, String message)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            
            //Insert data
            try (Connection conn = DriverManager.getConnection(DB_CONN_URL, DB_USER, DB_PASS))
            {
                String query = " insert into error_log (error_for, message, created_at)"
                        + " values (?, ?, ?)";
                PreparedStatement preparedStmt = conn.prepareStatement(query);
                preparedStmt.setString(1, errorFor);
                preparedStmt.setString(2, message);
                
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                preparedStmt.setTimestamp(3, timestamp);
                
                preparedStmt.execute();
            }
        }
        catch (ClassNotFoundException | SQLException ex)
        {
            LOGGER.error("Save error log# " + ex.toString());
            System.out.println(ex.toString());
        }
    }
}
