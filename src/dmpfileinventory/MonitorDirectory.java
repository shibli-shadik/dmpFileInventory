package dmpfileinventory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MonitorDirectory
{
    private static String SRC_FOLDER = "F:\\projects\\dmp\\testing\\src";
    
    public static void main(String[] args)
    {
        register();
    }
    
    public static void register()
    {
        try 
        {
            //Path faxFolder = Paths.get("./fax/");
            Path faxFolder = Paths.get(SRC_FOLDER);
            WatchService watchService = FileSystems.getDefault().newWatchService();
            faxFolder.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            
            boolean valid = true;
            String fileName = null;
            File file = null;
            BasicFileAttributes attrs = null;
            
            do 
            {
                WatchKey watchKey = watchService.take();
                
                for (WatchEvent event : watchKey.pollEvents()) 
                {
                    //WatchEvent.Kind kind = event.kind();
                    if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind())) 
                    {
                        fileName = event.context().toString();
                        System.out.println("File Name:" + fileName);
                        
                        file = new File(SRC_FOLDER + "\\" + fileName);
                        System.out.println("File Size:" + file.length());
                        
                        //attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                        //FileTime time = attrs.creationTime();
                        
                        String pattern = "yyyy-MM-dd HH:mm:ss";
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
                        String formatted = simpleDateFormat.format(new Date(file.lastModified()));

                        System.out.println( "The file creation date and time is: " + formatted );
                    }
                }
                
                valid = watchKey.reset();
                
                TimeUnit.SECONDS.sleep(1);
                
            } while (valid);
        } 
        catch (IOException | InterruptedException ex) 
        {
            Logger.getLogger(MonitorDirectory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
