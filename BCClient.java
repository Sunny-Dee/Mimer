/* file is: BCClient.java   5-5-07  1.0
 
 For use with webserver back channel. Written for Windows.
 
 This program may contain bugs. Note: version 1.0.
 
 To compile:
 
 rem jcxclient.bat
 rem java compile BCClient.java with xml libraries...
 rem Here are two possible ways to compile. Uncomment one of them:
 rem set classpath=%classpath%C:\dp\435\java\mime-xml\;c:\Program Files\Java\jdk1.5.0_05\lib\xstream-1.2.1.jar;c:\Program Files\Java\jdk1.5.0_05\lib\xpp3_min-1.1.3.4.O.jar;
 rem javac -cp "c:\Program Files\Java\jdk1.5.0_05\lib\xstream-1.2.1.jar;c:\Program Files\Java\jdk1.5.0_05\lib\xpp3_min-1.1.3.4.O.jar" BCClient.java
 
 Note that both classpath mechanisms are included. One should work for you.
 
 Requires the Xstream libraries contained in .jar files to compile, AND to run.
 See: http://xstream.codehaus.org/tutorial.html
 
 
 To run:
 
 rem rxclient.bat
 rem java run BCClient.java with xml libraries:
 set classpath=%classpath%C:\dp\435\java\mime-xml\;c:\Program Files\Java\jdk1.5.0_05\lib\xstream-1.2.1.jar;c:\Program Files\Java\jdk1.5.0_05\lib\xpp3_min-1.1.3.4.O.jar;
 java BCClient
 
 This is a standalone program to connect with MyWebServer.java through a
 back channel maintaining a server socket at port 2570.
 
 ----------------------------------------------------------------------*/

import java.io.*; // Get the Input Output libraries
import java.net.*; // Get the Java networking libraries
import java.util.Properties;

import com.thoughtworks.xstream.XStream;

public class BCClient {
    private static String XMLfileName;
    private static PrintWriter toXmlOutputFile;
    private static File xmlFile;
    private static BufferedReader fromMimeDataFile;
    private static String dirRoot;
    
    public static void main(String args[]) {
        String serverName;
        String argOne = "WillBeFileName";
        if (args.length < 1)
            serverName = "localhost";
        else
            serverName = args[0];
        
        XStream xstream = new XStream();
        String[] testLines = new String[4];
        int i;
        myDataArray da = new myDataArray();
        myDataArray daTest = new myDataArray();
        
        // Start a file in the current directory to get it's path
        File f = new File(".");
        
        // Get the file's path and append the name we want to the output file
        try {
            dirRoot = f.getCanonicalPath();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        
        XMLfileName = dirRoot + "/temp/mimer.output";
        
        System.out.println("Deliana's barely stitched together back channel Client.\n");
        System.out.println("Using server: " + serverName + ", Port: 2540 / 2570");
        //Start reader to read inputs from the keyboard
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        try {
            //userData will store the string in the buffer from the user
            String userData;
            
            System.out.println("Executing the java application.");
            System.out.flush();
            //Start the system properties to later add our arguments
            //Start is with this System's default values
            Properties p = new Properties(System.getProperties());
            
            //Get the value of the first argument
            //Then print it to the screen
            argOne = p.getProperty("firstarg");
            System.out.println("First var is: " + argOne);
            
            //Start an input steam buffer to capture the data
            fromMimeDataFile = new BufferedReader(new FileReader(argOne));
            do {
                
                
                //Prompt the user to enter something
                System.out.print("Enter a string to send to back channel of webserver, (quit) to end: ");
                System.out.flush();
                //store it in userData
                userData = in.readLine();
                //save the formatted response in the data array
                da.lines[0] = "You ";
                da.lines[1] = "typed ";
                da.lines[2] = userData;
                da.num_lines = 3;
                //Serialize data: Convert the data array to xml
                String xml = xstream.toXML(da);
                
                //if the user doesn't quit...
                if (userData.indexOf("quit") < 0) {
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
                    
                    //Create output file
                    xmlFile = new File(XMLfileName);
                    if (xmlFile.exists() == true && xmlFile.delete() == false) {
                        throw (IOException) new IOException("XML file delete failed.");
                    }
                    xmlFile = new File(XMLfileName);
                    //if file creation doesn't fail...
                    if (xmlFile.createNewFile() == false) {
                        throw (IOException) new IOException("XML file creation failed.");
                        // ...place the data there
                    } else {
                        toXmlOutputFile = new PrintWriter(new BufferedWriter(new FileWriter(XMLfileName)));
                        toXmlOutputFile.println("First arg to Handler is: " + argOne + "\n");
                        toXmlOutputFile.println(xml);
                        toXmlOutputFile.close();
                    }
                }
            } while (userData.indexOf("quit") < 0);
            System.out.println("Cancelled by user request.");
            
        } catch (IOException x) {
            x.printStackTrace();
        }
    }
    
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
            
            toServer.println(sendData);
            toServer.println("end_of_xml");
            toServer.flush();
            // Read two or three lines of response from the server,
            // and block while synchronously waiting:
            System.out.println("Blocking on acknowledgment from Server... ");
            textFromServer = fromServer.readLine();
            if (textFromServer != null) {
                System.out.println(textFromServer);
            }
            sock.close();
        } catch (IOException x) {
            System.out.println("Socket error.");
            x.printStackTrace();
        }
    }
    
}