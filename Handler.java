/* Handler.java 1.0
 Clark Elliott MIMER shim java example
 
 Capture Environment Variables passed from .bat file through java.exe.
 
 Assuming the first argument is a valid file name, read five lines
 of data from the file, and display the data on the console.
 
 Note that NO XML is used in this preliminary program, although some
 variable names refer to XML for consistency with other programs
 in this assignment.
 
 Here is the DOS .bat file to run this Java program:
 rem This is shim.bat
 rem Have to set classpath in batch, passing as arg does not work:
 set classpath=%classpath%;c:/dp/435/java/mime-xml/
 rem Pass the name of the first argument to java:
 java -Dfirstarg="%1" Handler
 
 To run:
 
 > shim mimer-data.xyz
 
 ...where mimer-data.xyz has five lines of ascii data in it.
 
 */

import java.io.*;
import java.util.*;

public class Handler {
    
    private static String XMLfileName;
    private static PrintWriter toXmlOutputFile;
    private static File xmlFile;
    private static BufferedReader fromMimeDataFile;
    private static String dirRoot;
    
    public static void main(String args[]) {
        
        //Start a counter to read the lines of data from the file
        int i = 0;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        myDataArray da = new myDataArray();
        
        //Start a file in the current directory to get it's path
        File f = new File(".");
        
        //Get the file's path and append the name we want to the output file
        try {
            dirRoot = f.getCanonicalPath();
        } catch (Throwable e){e.printStackTrace();}
        
        XMLfileName = dirRoot + "/temp/mimer.output";
        
        
        try {
            System.out.println("Executing the java application.");
            System.out.flush();
            //Start the system properties to later add our arguments
            //Start is with this System's default values
            Properties p = new Properties(System.getProperties());
            
            //Get the value of the first argument
            //Then print it to the screen
            String argOne = p.getProperty("firstarg");
            System.out.println("First var is: " + argOne);
            
            //Start an input steam buffer to capture the data
            fromMimeDataFile = new BufferedReader(new FileReader(argOne));
            
            /*
             * Up to 8 times: read line from the file
             * while storing it into our special array class
             * and also print it to the screen.
             */
            // Only allows for five lines of data in input file plus safety:
            while (((da.lines[i++] = fromMimeDataFile.readLine()) != null) && i < 8) {
                System.out.println("Data is: " + da.lines[i - 1]);
            }
            
            //Adjust i to reflect the number of lines read and print it.
            da.num_lines = i - 1;
            System.out.println("i is: " + i);
            
            //Open a new file with the mime.output name and extension
            xmlFile = new File(XMLfileName);
            System.out.println("filename: " + XMLfileName);
            
            //Throw an exception if that file already exists and
            //if trying to delete it fails...
            if (xmlFile.exists() == true && xmlFile.delete() == false) {
                throw (IOException) new IOException("XML file delete failed.");
            }
            //... otherwise create a new file with mime.output name
            xmlFile = new File(XMLfileName);
            
            //Throw exception if creating a file fails...
            if (xmlFile.createNewFile() == false) {
                throw (IOException) new IOException("XML file creation failed.");
                
                //...otherwise use the PrintWriter to output the first argument variable witch 
                // Should be the file name. 
            } else {
                toXmlOutputFile = new PrintWriter(new BufferedWriter(new FileWriter(XMLfileName)));
                toXmlOutputFile.println("First arg to Handler is: " + argOne + "\n");
                toXmlOutputFile.println("<This where the persistent XML will go>");
                toXmlOutputFile.close();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
