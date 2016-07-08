import java.io.*;
import java.lang.reflect.Field;
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

public class CodeDeploy {
    public static int deploy(String bucketName) {
        File target = new File("/tmp/code/target/");
        File buildTarget = new File("/tmp/code/build/target/"); 
        ArrayList<String> codeFiles = getFileList(target);
        ArrayList<String> buildFiles = getFileList(buildTarget);

        zip("target", codeFiles);
        zip("buildTarget", buildFiles);
        upload(bucketName, "/tmp/target.zip");
        upload(bucketName, "/tmp/buildTarget.zip");
        return 0;
    }

    private static ArrayList<String> getFileList(File file) {
        return getFileList(file, new ArrayList<String>());
    }

    private static ArrayList<String> getFileList(File file, ArrayList<String> zipped) {
        if (file.isDirectory()) {
            for (File f: file.listFiles()) {
                getFileList(f, zipped);
            }
        } else {
            zipped.add(file.toString());
        }
        return zipped;
    }

    public static void upload(String bucketName, String uploadFileName) {

        AmazonS3 s3client = new AmazonS3Client();//new ProfileCredentialsProvider("/tmp/credentials", "default"));
        try {
            System.out.println("Uploading a new object to S3 from a file\n");
            File file = new File(uploadFileName);
            s3client.putObject(new PutObjectRequest(
                                     bucketName, file.getName(), file));

         } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which " +
                    "means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which " +
                    "means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }

    protected static void zip(String fileName, ArrayList<String> files) {
            byte[] buffer = new byte[1024];            
            try {
                FileOutputStream fos = new FileOutputStream("/tmp/" + fileName + ".zip");
                ZipOutputStream zos = new ZipOutputStream(fos);
                for (String file: files) {
                    try {
                        File fs = new File(file);
                        System.out.println(fs.getName());
                        FileInputStream fis = new FileInputStream(fs);
                        ZipEntry zipEntry = new ZipEntry(fs.getName());
                        zos.putNextEntry(zipEntry);

                        byte[] bytes = new byte[1024];
                        int length;
                        while ((length = fis.read(bytes)) >= 0) {
                            zos.write(bytes, 0, length);
                        }

                        zos.closeEntry();
                        fis.close();
                    } catch (Exception e) {

                    }
                }
                zos.close();
                fos.close();
            } catch(IOException ex){
                ex.printStackTrace();
            }
    }
}
