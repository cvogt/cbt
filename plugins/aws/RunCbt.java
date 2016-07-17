import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;

public class RunCbt {
    public static void main(String[] args) {
        return;
    }

    @SuppressWarnings("unchecked")
    public String cbtHandler(String args) {
        System.out.println("In runCbt");
        File dir = new File("."); //to test locally change this to "target"
        File[] filesList = dir.listFiles();
        for (File file : filesList) {
            File src = file;
            File dest = new File("/tmp/" + file.getName());
            try {    
                copy(src, dest);
            } catch (Exception e) {
                e.printStackTrace();
                return "Copy failed.";
            }
        }

        File start = new File("/tmp/startCbt.class");
        String[] splitArgs = args.split("\\s+");
        Object[] params = { splitArgs };
        File file = new File("/tmp");
        int res = 0;
        try {
            // Convert File to a URL
            URL url = file.toURI().toURL();  
            URL[] urls = new URL[]{url};
            ClassLoader cl = new URLClassLoader(urls);
            Class<?> cls = cl.loadClass("StartCbt");
            res = (int) cls.getMethod("run", String[].class).invoke((Object) null, params);
        } catch (StartCbt.ExitException e) {
            res = 0;
        } catch (Exception e) {
            res = 0;
        }
        
        return String.valueOf(res);
    }

    public static void copy(File source, File destination) throws IOException {
        if (!destination.exists()) {
            if (source.isDirectory()) {
                destination.mkdir();
                for (File file: source.listFiles()) {
                    copy(file, new File(destination.toString() + "/" + file.getName()));
                }
            } else {
                InputStream inStream = new FileInputStream(source);
                OutputStream outStream = new FileOutputStream(destination);
                
                byte[] buffer = new byte[1024];
              
                int length;
                //copy the file content in bytes 
                while ((length = inStream.read(buffer)) > 0){
              
                  outStream.write(buffer, 0, length);
             
                } 
                inStream.close();
                outStream.close();
            }
        }
    }
}
