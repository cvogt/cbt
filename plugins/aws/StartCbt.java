import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.security.Permission;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class StartCbt {
    @SuppressWarnings("unchecked")
    public static int run(String[] args) {
        System.out.println("In startCbt");
        System.setSecurityManager(new NoExitSecurityManager());
        System.getProperties().setProperty("user.dir", "/tmp/code");
        System.getProperties().setProperty("zinc.home", "/tmp");
        System.getProperties().setProperty("zinc.dir", "/tmp");
        int res = 0;
        Map<String, String> env = new HashMap<String, String>();
        env.put("CBT_HOME", "/tmp");
        env.put("NAILGUN", "/tmp/nailgun_launcher/");
        env.put("TARGET", "target/scala-2.11/classes/");
        setEnv(env);
        String[] nailgunArgs = new String[args.length];
        // FIXME: is it okay to manually set the time taken to 0?
        nailgunArgs[0] = "0.0";
        nailgunArgs[1] = System.getProperty("user.dir");
        for (int i = 2; i < nailgunArgs.length; i++) {
            nailgunArgs[i] = args[i - 2];
        }

        Object[] params = { nailgunArgs };
        String[] parameters = {"0.0", "/tmp/deploy", "run", args[args.length - 2], args[args.length - 1] };
        Object[] deployParams = { parameters }; 

        File file = new File("/tmp/nailgun_launcher/target/scala-2.11/classes/");

        // FIXME: currently no way of differentiating between a compile exception and trap exit so solution is to swallow both
        try {
            URL url = file.toURI().toURL(); 
            URL[] urls = new URL[]{url};
            ClassLoader cl = new URLClassLoader(urls);
            Class<?> cls = cl.loadClass("cbt.NailgunLauncher");
            System.getProperties().setProperty("user.dir", "/tmp/code");
            System.out.println("Now compiling code with cwd as " + System.getProperty("user.dir"));
            cls.getMethod("main", String[].class).invoke((Object) null, params);
        } catch (ExitException e) {
            //
        } catch (Exception e) {
            
        }

        try {
            URL url = file.toURI().toURL(); 
            URL[] urls = new URL[]{url};
            ClassLoader cl = new URLClassLoader(urls);
            Class<?> cls = cl.loadClass("cbt.NailgunLauncher");
            System.getProperties().setProperty("user.dir", "/tmp/deploy"); 
            System.out.println("Now running deploy with cwd as " + System.getProperty("user.dir"));
            cls.getMethod("main", String[].class).invoke((Object) null, deployParams);
        } catch (ExitException e) {
            //
        } catch (Exception e) {
            
        }
        return res;
    }

    private static HashSet<String> getFileList(File file) {
        return getFileList(file, new HashSet<String>());
    }

    private static HashSet<String> getFileList(File file, HashSet<String> zipped) {
        if (file.isDirectory()) {
            for (File f: file.listFiles()) {
                getFileList(f, zipped);
            }
        } else {
            if (!file.toString().contains("/httpclient/4.5.2")) {
                System.out.println("Adding: " + file.toString() + " to class path.");
                zipped.add(file.toString());
            }
        }
        return zipped;
    }

    @SuppressWarnings("unchecked")
    protected static void setEnv(Map<String, String> newenv) {
      try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>)     theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        }
        catch (NoSuchFieldException e) {
          try {
            Class<?>[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for(Class<?> cl : classes) {
                if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(newenv);
                }
            }
          } catch (Exception e2) {
            e2.printStackTrace();
          }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    protected static class ExitException extends SecurityException {
        public final int status;
        
        public ExitException(int status) {
            //super("Caught exit.");
            this.status = status;
        }
        
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }

        @Override
        public String toString() {
            return "";
        }
    }

    private static class NoExitSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(Permission perm) {
            // allow anything.
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // allow anything.
        }

        @Override
        public void checkExit(int status) {
            throw new ExitException(status);
        }
    }
}
