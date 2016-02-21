import java.io.IOException;
import java.net.*;
import java.io.*;
import java.util.*;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Name: Deliana Escobari Date: Saturday February 20th, 2016 Java version used:
 * 1.8 Compile with script: jcx.sh
 *
 * How to run this server: In separate shell window open: rx.sh
 *
 * To send a request: type $ http://<machine's IP address or 'localhost' if this
 * is the same machine MyWebServer is running>:2540 $ You can add $ /'filename'$
 * if you know what file you are looking for. Otherwise the server will display
 * a listing of all the files in the directory.
 *
 * Files needed to run this program. myDataArray.java (compiles within the same
 * script as this program)
 *
 * Notes: This is a class start the server and calls a thread to fulfill the
 * client's request sent by the browser. It also starts a back channel
 * communication with the client to receive files selected by the client, read
 * them, and display them into the console.
 */

public class MyWebServer {
    public static void main(String args[]) throws IOException {
        int q_len = 6;
        // port specified for this assignment
        int port = 2540;
        Socket sock;
        
        // This is the admin that spawns the back channel worker
        BCLooper AL = new BCLooper(); // create a DIFFERENT thread for Back Door
        // Channel
        Thread t = new Thread(AL);
        t.start(); // ...and start it, waiting for Back Channel input
        
        /*
         * This guy is going to start the connection to the port then it will
         * pass on the request to the worker
         */
        ServerSocket servsock = new ServerSocket(port, q_len);
        
        System.out.println("World's Greatest Web Sever \nStarting up, " + "listening at port " + port + ".");
        try {
            while (true) {
                sock = servsock.accept(); // Wait for the next client connection
                new WebWorker(sock).start(); // Spawn worker to handle it
            }
        } finally {
            servsock.close();
        }
    }
}

/**
 * Notes: This is the thread class that is started every time the client sends
 * an http request using a browser connected to the local port. Class is invoked
 * by the server and doesn't need to be started.
 */
class WebWorker extends Thread {
    Socket sock;
    String dirRoot;
    File f;
    List<String> requestElements;
    
    public WebWorker(Socket socket) {
        sock = socket;
        f = new File("."); // to get the file for the current directory
        requestElements = new ArrayList<String>();
    }
    
    public void run() {
        // Get I/O streams in/out from the socket
        DataOutputStream out = null;
        BufferedReader in = null;
        // UPDATE:I added this because DataOutputStream is not writing to file.
        PrintStream sendClient = null;
        
        try {
            // input to the socket
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            // output from the socket
            out = new DataOutputStream(sock.getOutputStream());
            sendClient = new PrintStream(sock.getOutputStream());// this writes
            // to the
            // temp file
            
            try {
                dirRoot = f.getCanonicalPath();
            } catch (Throwable e) {
                e.printStackTrace();
            }
            
            String request;
            
            try {
                request = in.readLine();
                
                // if the request is valid, split it to get the parts
                if (requestIsValid(request)) {
                    System.out.println("Fulfilling client request");
                    for (String part : request.split(" ")) {
                        requestElements.add(part);
                    }
                    
                    // determine if requested output is file or directory
                    File requestedFile = new File(dirRoot + requestElements.get(1));
                    
                    if (requestElements.get(1).contains("addnums.fake-cgi"))
                        addNumms(requestElements.get(1), out);
                    
                    /*
                     * UPDATE: added this statement to handle with new MIME-type
                     * but could also be more efficient by updating my writeFile
                     * method. I thought this was easier for debugging.
                     */
                    else if (requestElements.get(1).endsWith(".xyz")) {
                        
                        writeXYZ(requestedFile, out, sendClient);
                    } else if (requestedFile.isDirectory())
                        writeDir(requestedFile, out);
                    else
                        writeFile(requestedFile, out);
                } else {
                    System.out.println("Client request not valid.\n" + "Are you trying to hack this?");
                }
                
            } catch (IOException e) {
                System.out.println(e);
            }
            sock.close(); // close this connection, but not the server
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
    
    /*
     * ******************************** Helper Methods
     * ********************************
     */
    
    /**
     * Starts an AddNums object to create a dynamic HTML page that responds to
     * the AddNums form
     *
     * @param request:
     *            the GET request with the name:value pairs
     * @param out:
     *            the output stream/ web browser
     */
    private void addNumms(String request, DataOutputStream out) {
        AddNums form = new AddNums(request);
        String html = form.generateResponse();
        String header = createHeader(html.length(), request);
        try {
            out.write(header.getBytes());
            out.write(html.getBytes());
        } catch (IOException io) {
            System.out.println(io);
        }
    }
    
    /**
     * Handles requests for .xyz files. Very similar to my writeFile method, but
     * they are separated to make debugging simpler.
     *
     * @param requestedFile
     *            file requested by the client with .xyz extension
     * @param out
     *            output stream to write into header into browser and trigger
     *            handler
     * @param sendClient
     *            prints the lines of the requested file to a temporary file
     *            saved in a default folder. (Downloads folder in Mac
     *            computers.)
     */
    public void writeXYZ(File requestedFile, OutputStream out, PrintStream sendClient) {
        
        // create header
        String header = requestElements.get(2) + " 200 OK\r\n" + "Content-Length: " + requestedFile.length() + "\r\n"
        + "Content-Type: " + guessContentType(".xyz") + "\r\n" + "Date: " + new Date() + "\r\n\r\n";
        
        System.out.println(header);
        
        try {
            /*
             * Send header to the browser. This will trigger the script that
             * handles this MIME-type and it will run the shell script that has
             * the handler.
             */
            out.write(header.getBytes());
            // Read the requested file line by line and print to temp file
            BufferedReader readFile = new BufferedReader(new FileReader(requestedFile.getName())); // open
            // appropriate
            // file
            String line = null;
            while ((line = readFile.readLine()) != null) {
                System.out.println(line); // to client.
                sendClient.println(line); // print to file //send content of
                // file to client line by line
            }
            readFile.close();
        } catch (Exception e) { // catch errors
            e.printStackTrace();
            sendClient.println("File is not in this directory");
        }
    }
    
    /**
     * Opens a file and writes its content to the browser
     *
     * @param requestedFile
     *            the file user requested to open
     * @param out:
     *            the browser window that requested the file.
     */
    public void writeFile(File requestedFile, DataOutputStream out) {
        try {
            
            // create an input stream to read/get the requested file
            InputStream file = new FileInputStream(requestedFile);
            
            // format the header correctly based on request
            String header = createHeader(requestedFile.length(), requestElements.get(1));
            
            try {
                // send the header to the client/browser
                out.write(header.getBytes());
                
                // write the file to the output stream
                sendFile(file, out);
                
                // Grandma always said: remember to close your files!
                file.close();
            } catch (IOException io) {
                System.out.println(io);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * List the files and sub directories of the requested parent directory
     *
     * @param requestedDirectory:
     *            the parent directory
     * @param out:
     *            the output stream. In other words the browser.
     */
    public void writeDir(File requestedDirectory, DataOutputStream out) {
        File[] dirFiles = requestedDirectory.listFiles();
        
        String header = requestElements.get(requestElements.size() - 1) + " 200 OK\r\n" + "Date: " + new Date()
        + "\r\n\r\n";
        
        try {
            out.write(header.getBytes());
            
            for (int i = 0; i < dirFiles.length; i++) {
                
                if (dirFiles[i].isFile()) {
                    
                    out.write(formatHTMLFile(dirFiles[i]));
                } else if (dirFiles[i].isDirectory()) {
                    StringBuilder hdr = new StringBuilder();
                    
                    hdr.append("<a href=\"");
                    hdr.append(dirFiles[i].getName());
                    hdr.append("/\">[DIR]       ");
                    hdr.append(dirFiles[i].getName() + "</a> <br>");
                    
                    String str = hdr.toString();
                    
                    System.out.println("Fetching directory");
                    out.write(str.getBytes());
                    
                }
            }
            
        } catch (IOException io) {
            System.out.println(io);
        }
        
    }
    
    /**
     * Guess content type based on the ending of the requested file
     * {@link} http://cs.au.dk/~amoeller/WWW/javaweb/server.html
     *
     * @param path:
     *            the name of the file. Only extension part is evaluated.
     * @return: the content type to fill HTTP response value for content-type
     */
    private static String guessContentType(String path) {
        if (path.endsWith(".html") || path.endsWith(".htm"))
            return "text/html";
        // UPDATE: returns an "application/xyz" MIME header for files with type
        // ".xyz"
        else if (path.endsWith(".xyz"))
            return "application/xyz";
        else if (path.endsWith(".txt") || path.endsWith(".java"))
            return "text/plain";
        else if (path.endsWith(".gif"))
            return "image/gif";
        else if (path.endsWith(".class"))
            return "application/octet-stream";
        else if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
            return "image/jpeg";
        else
            return "text/html";
    }
    
    /**
     * Checks if request has correct format for a GET request (can be modified
     * to accept other types) and that no evil hackers are trying to hack the
     * server {@code} found at http://cs.au.dk/~amoeller/WWW/javaweb/server.html
     *
     * @param request
     * @return
     */
    private boolean requestIsValid(String request) {
        // make sure this is a GET request.
        if (!request.startsWith("GET") || request.length() < 14
            || !(request.endsWith("HTTP/1.0") || request.endsWith("HTTP/1.1"))) {
            System.out.println("bad request");
            return false;
        } else {
            /*
             * For evil hackers, check if request is not trying to enter other
             * directories
             */
            String req = request.substring(4, request.length() - 9).trim();
            if (req.indexOf("..") != -1 || req.indexOf("/.ht") != -1 || req.endsWith("~"))
                return false;
        }
        
        return true;
    }
    
    /**
     * Read the file then write its bytes to output stream
     *
     * @param file:
     *            input stream to read/get the requested file
     * @param out:
     *            the output streem to write the file to
     */
    private static void sendFile(InputStream file, OutputStream out) {
        try {
            System.out.println("Sending file to browser");
            
            // read the file
            byte[] buffer = new byte[1000];
            while (file.available() > 0)
                // and write the bytes the output stream
                out.write(buffer, 0, file.read(buffer));
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }
    
    /**
     * Format a file to show as a hotlink on a web browser
     *
     * @param file:
     *            a file in a directory
     * @return the bytes to be written on the output stream
     */
    public byte[] formatHTMLFile(File file) {
        String htmlFormat = "<a href=\"" + file.getName() + "\">[FILE]   " + file.getName() + "</a> <br>";
        
        return htmlFormat.getBytes();
    }
    
    /**
     * Produces an appropriate header for a file. Not meant to be used by a
     * directory but it could be adapted to do so.
     *
     * @param fileLength:
     *            the length of the file. Input 0 if directory.
     * @param file:
     *            is the name of the file to get its content type
     * @return a string containing the appropriate HTML header
     */
    public String createHeader(long fileLength, String file) {
        
        String header;
        
        // In case option needs to be adapted for directories
        String h1 = requestElements.get(2) + " 200 OK\r\n" + "Content-Type: " + guessContentType(file) + "\r\n"
        + "Date: " + new Date() + "\r\n\r\n";
        
        // Most common option for files
        String h2 = requestElements.get(2) + " 200 OK\r\n" + "Content-Legth: " + fileLength + "\r\n" + "Content-Type: "
        + guessContentType(file) + "\r\n" + "Date: " + new Date() + "\r\n\r\n";
        
        if (fileLength == 0)
            header = h1;
        else
            header = h2;
        
        return header;
        
    }
}

/**
 * Name: Deliana Escobari Date: Saturday February 6th, 2016 Java version used:
 * 1.8 Compile with command: javac MyWebServer.java or | javac *.java | once to
 * compile all files in the whole folder
 *
 * List of files needed for running the program. MyWebServer.java WebWorker.java
 * AddNums.java
 *
 * Notes: This helper class runs whenever the addNums method is invoked. It
 * facilitates the creation of the html page that responds to the form.
 */
class AddNums {
    String request;
    HashMap<String, Object> valuePairs;
    
    public AddNums(String request) {
        this.request = request;
        valuePairs = new HashMap<String, Object>();
    }
    
    /**
     * Formats the user's inputs as an HTML response.
     *
     * @return a string with HTML formatting showing the response to the user's
     *         request. Meant to be displayed in a browser.
     */
    public String generateResponse() {
        parse();
        String title = "<html><head><TITLE> " + "Response for AddNum request</TITLE></head>"
        + "<BODY style=\"background-color:lightgrey;\"><center> "
        + "<H1 style=\"font-family:verdana;\">Response for " + valuePairs.get("person")
        + "'s AddNum Request</H1>"
        + "<H2 style=\"font-family:verdana;\">Here are the numbers you wanted to add: </H2>";
        
        String data = "<H3>" + valuePairs.get("num1") + " and " + valuePairs.get("num2") + "<H2> Answer: " + addNums()
        + "</H2>";
        
        String closingTags = "</center></BODY></html>";
        
        StringBuilder htmlPage = new StringBuilder();
        
        htmlPage.append(title);
        htmlPage.append(data);
        htmlPage.append(closingTags);
        
        return htmlPage.toString();
    }
    
    /*
     * ******************************** Helper Methods
     * ********************************
     */
    
    /**
     * Parses through the string containing the numbers and adds them up
     *
     * @return the addition of the two values.
     */
    private int addNums() {
        
        int val1 = Integer.parseInt(valuePairs.get("num1").toString());
        int val2 = Integer.parseInt(valuePairs.get("num2").toString());
        return val1 + val2;
        
    }
    
    /**
     * Parses through the name-value pairs in the request and updates the
     * valuePairs table, which is used to generate the response formatted for
     * HTML.
     */
    private void parse() {
        // throw out everything before '?' to get just the parameters
        String valuePairStr = request.substring(request.indexOf("?") + 1);
        
        // get all the name value pairs into to table
        for (String value : valuePairStr.split("&"))
            valuePairs.put(value.substring(0, value.indexOf("=")), value.substring(value.indexOf("=") + 1));
        
    }
    
}

/**
 * Worker thread that opens a back channel communication with the client through
 * a different port. It takes in the content of a file and marshals the data
 * into XML.
 */
class BCWorker extends Thread {
    private Socket sock;
    private int i;
    
    BCWorker(Socket s) {
        sock = s;
    }
    
    PrintStream out = null;
    BufferedReader in = null;
    
    String[] xmlLines = new String[15];
    String[] testLines = new String[10];
    String xml;
    String temp;
    
    // Instantiate the marshaling object
    XStream xstream = new XStream(new DomDriver());
    final String newLine = System.getProperty("line.separator");
    myDataArray da = new myDataArray();
    
    public void run() {
        System.out.println("Called BC worker.");
        try {
            //start an input buffer and a print steam to print what you get
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            out = new PrintStream(sock.getOutputStream());
            i = 0;
            xml = "";
            
            //Read the file and keep appending new lines to XML string var. 
            while (true) {
                temp = in.readLine();
                if (temp.indexOf("end_of_xml") > -1)
                    break;
                else
                    xml = xml + temp + newLine;
            }
            //Print the serialized data into the back channel console. 
            System.out.println("The XML marshaled data:");
            System.out.println(xml);
            out.println("Acknowledging Back Channel Data Receipt"); // send the
            // ack
            out.flush();
            sock.close();
            
            da = (myDataArray) xstream.fromXML(xml); // deserialize / unmarshal
            // data
            
            //print deserialized data into back channel console. 
            System.out.println("Here is the restored data: ");
            for (i = 0; i < da.num_lines; i++) {
                System.out.println(da.lines[i]);
            }
        } catch (IOException ioe) {
        } // end run
    }
}

/**
 * Admin class to spawn new back channel worker threads similar to ModeAdminServer in 
 * JokeServer
 */
class BCLooper implements Runnable {
    public static boolean adminControlSwitch = true;
    
    public void run() { // Running the Admin listen loop
        System.out.println("In BC Looper thread, waiting for 2570 connections");
        
        int q_len = 6; /* Number of requests for OpSys to queue */
        int port = 2570; // Listen here for Back Channel Connections
        Socket sock;
        
        try {
            @SuppressWarnings("resource")
            ServerSocket servsock = new ServerSocket(port, q_len);
            while (adminControlSwitch) {
                // wait for the next ADMIN client connection:
                sock = servsock.accept();
                new BCWorker(sock).start();
            }
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
}
