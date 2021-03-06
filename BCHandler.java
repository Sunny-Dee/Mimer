import java.io.*;  // Get the Input Output libraries
import java.net.*; // Get the Java networking libraries
import java.util.*;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;


/**
 * Name: Deliana Escobari Date: Saturday February 20th, 2016 Java version used:
 * 1.8 Compile with script: jcbchandler.sh
 *
 * How to run this program: You don't need to! Our own Mime-reader.app calls it 
 * from the browser for us.
 *
 * Files needed to run this program. myDataArray.java (compiles within the same
 * script as this program)
 *
 * Notes: Handles files with application/xyz MIME-Type. 
 */

public class BCHandler {
    private static String XMLfileName;
    private static PrintWriter toXmlOutputFile;
    private static File xmlFile;
    private static BufferedReader fromMimeDataFile;
    private static String dirRoot;
    
    public static void main(String args[]){
        String serverName;
        if (args.length < 1)
            serverName = "localhost";
        else
            serverName = args[0];
        
        //Start a counter to read the lines of data from the file
        int i = 0;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        myDataArray da = new myDataArray();
        myDataArray daTest = new myDataArray();// From BCClient
        
        //Start a file in the current directory to get it's path
        File f = new File(".");
        
        //Get the file's path and append the name we want to the output file
        try {
            dirRoot = f.getCanonicalPath();
        } catch (Throwable e){e.printStackTrace();}
        
        XMLfileName = dirRoot + "/temp/mimer.output";
        
        try {
            //From Handler
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
            System.out.println("filename: " + XMLfileName); //this was a check for me.
            
            //Serialize data: Convert the data array to xml
            XStream xstream = new XStream(new DomDriver());
            String xml = xstream.toXML(da);
            
            //From BCClient
            //send the message to the back channel server
            sendToBC(xml, serverName);
            //and print to terminal
            System.out.println("\n\nHere is the XML version:");
            System.out.print(xml);
            
            // deserialize data
            daTest = (myDataArray) xstream.fromXML(xml);
            //print that as well
            System.out.println("\n\nHere is the deserialized data: ");
            for (i = 0; i < daTest.num_lines; i++) {
                System.out.println(daTest.lines[i]);
            }
            System.out.println("\n");
            
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
                toXmlOutputFile.println("Persistent XML with the help of XStream");
                toXmlOutputFile.println("First arg to Handler is: " + argOne + "\n");
                toXmlOutputFile.println(xml);
                toXmlOutputFile.close();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        
    }
    
    /**
     * Data goes full circle and it's displayed in the servers console through the back
     * channel connection.
     * @param sendData likely xml formatted string to write into client's back channel
     * @param serverName either server's IP address or 'localhost'
     */
    static void sendToBC(String sendData, String serverName) {
        Socket sock;
        BufferedReader fromServer;
        PrintStream toServer;
        String textFromServer;
        try {
            // Open our connection Back Channel on server:
            sock = new Socket(serverName, 2570);
            toServer = new PrintStream(sock.getOutputStream());
            // Will be blocking until we get ACK from server that data sent
            fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            
            //Print the data to the server console
            toServer.println(sendData);
            toServer.println("end_of_xml");
            toServer.flush();
            // Read two or three lines of response from the server,
            // and block while synchronously waiting:
            System.out.println("Blocking on acknowledgment from Server... ");
            //Store data sent from server in a string variable
            textFromServer = fromServer.readLine();
            //print it out to the console.
            if (textFromServer != null) {
                System.out.println(textFromServer);
            }
            //Never forget to close a socket!
            sock.close();
        } catch (IOException x) {
            System.out.println("Socket error.");
            x.printStackTrace();
        }
    }
    
}
