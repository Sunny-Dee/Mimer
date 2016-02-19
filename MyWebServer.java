import java.io.IOException;
import java.net.*;
import java.io.*;
import java.util.*;


/**
 * Name: Deliana Escobari       Date: Saturday February 6th, 2016
 * Java version used: 1.8
 * Compile with command: javac MyWebServer.java
 * 		or | javac *.java | once to compile all files in the whole folder
 *
 * How to run this server:
 * 		In separate shell window open:
 * 				java MyWebServer
 *
 * To send a request: type
 * 		$ http://<machine's IP address or 'localhost' if this is the
 * 		same machine MyWebServer is running>:2540 $
 * 		You can add $ /'filename'$ if you know what file you are looking for.
 * 		Otherwise the server will display a listing
 * 		of all the files in the directory.
 *
 * List of files needed for running the program.
 * 				MyWebServer.java
 * 				WebWorker.java (in this file)
 * 				AddNums.java  (in this file)
 *
 * Notes: This is a class start the server and calls a thread to fulfill the
 * client's request sent by the browser.
 */

public class MyWebServer {
    public static void main(String args[]) throws IOException{
        int q_len = 6;
        //port speficied for this assignment
        int port = 2540;
        Socket sock;
        
        /*This guy is going to start the connection to the port
         * then it will pass on the request to the worker
         */
        ServerSocket servsock = new ServerSocket(port, q_len);
        
        System.out.println("World's Greatest Web Sever \nStarting up, "
                           + "listening at port " + port + ".");
        try {
            while(true){
                sock = servsock.accept(); //Wait for the next client connection
                new WebWorker(sock).start(); //Spawn worker to handle it
            }
        } finally { servsock.close();}
    }
}



/**
 * Name: Deliana Escobari       Date: Saturday February 6th, 2016
 * Java version used: 1.8
 * Compile with command: javac MyWebServer.java
 * 		or | javac *.java | once to compile all files in the whole folder
 
 * List of files needed for running the program.
 * 				MyWebServer.java
 * 				WebWorker.java
 * 				AddNums.java
 *
 * Notes: This is the thread class that is started every time the client
 * sends an http request using a browser connected to the local port.
 * Class is invoked by the server and doesn't need to be started.
 */
class WebWorker extends Thread {
    Socket sock;
    String dirRoot;
    File f;
    List<String> requestElements;
    
    public WebWorker(Socket socket){
        sock = socket;
        f = new File("."); //to get the file for the current directory
        requestElements = new ArrayList<String>();
    }
    
    public void run(){
        //Get I/O streams in/out from the socket
        DataOutputStream out = null;
        BufferedReader in = null;
        
        
        try{
            //input to the socket
            in = new BufferedReader
            (new InputStreamReader(sock.getInputStream()));
            //output from the socket
            out = new DataOutputStream(sock.getOutputStream());
            
            try{
                dirRoot = f.getCanonicalPath();
            }catch (Throwable e){e.printStackTrace();}
            
            String request;
            
            try{
                request = in.readLine();
                
                //if the request is valid, split it to get the parts
                if (requestIsValid(request)){
                    System.out.println("Fulfilling client request");
                    for (String part : request.split(" ")){
                        requestElements.add(part);
                    }
                    
                    //determine if requested output is file or directory
                    File requestedFile = new File(dirRoot + requestElements.get(1));
                    
                    if (requestElements.get(1).contains("addnums.fake-cgi"))
                        addNumms(requestElements.get(1), out);
                    else if (requestElements.get(1).endsWith(".xyz")){
                        //    					String header = createHeader(0, requestElements.get(1));
                        //    					out.write(header.getBytes());
                        //    					out.write(request.getBytes());
                        //    					System.out.println(header);
                        writeXYZ(requestedFile, out);
                    }
                    else if (requestedFile.isDirectory())
                        writeDir(requestedFile, out);
                    else
                        writeFile(requestedFile, out);
                }
                else{
                    System.out.println("Client request not valid.\n"
                                       + "Are you trying to hack this?");
                }
                
            }catch (IOException e) {
                System.out.println(e);
            }
            sock.close(); //close this connection, but not the server
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
    
    /* ********************************
     *      Helper Methods
     * ********************************
     */
    
    /**
     * Starts an AddNums object to create a dynamic HTML page
     * that responds to the AddNums form
     * @param request: the GET request with the name:value pairs
     * @param out: the output stream/ web browser
     */
    private void addNumms(String request, DataOutputStream out) {
        AddNums form = new AddNums(request);
        String html = form.generateResponse();
        String header = createHeader(html.length(), request);
        try{
            out.write(header.getBytes());
            out.write(html.getBytes());
        } catch (IOException io) {System.out.println(io);}
    }
    
    
    public void writeXYZ(File requestedFile, OutputStream out){
        //		try{
        //			InputStream file = new FileInputStream(requestedFile);
        String header = requestElements.get(2) +
        " 200 OK\r\n" +
        "Content-Legth: "+
        requestElements.get(1).length() +
        "\r\n" +
        "Content-Type: " +
        guessContentType(".xyz") +
        "\r\n" +
//        "Content-Enconding: Deflate \r\n" +
        "Date: " + new Date() + "\r\n\r\n";
        
        System.out.println(header);
        
        try {
            //send the header to the client/browser
            out.write(header.getBytes());
            
            //write the file to the output stream
            //		        sendFile(file, out);
            
            // Grandma always said: remember to close your files!
            //		        file.close();
        } catch (IOException io) {System.out.println(io);}
        //		} catch (FileNotFoundException e) {e.printStackTrace();}
    }
    
    
    /**
     * Opens a file and writes its content to the browser
     * @param requestedFile the file user requested to open
     * @param out: the browser window that requested the file.
     */
    public void writeFile(File requestedFile, DataOutputStream out){
        try {
            
            //create an input stream to read/get the requested file
            InputStream file = new FileInputStream(requestedFile);
            
            //format the header correctly based on request
            String header = createHeader(requestedFile.length(),
                                         requestElements.get(1));
            
            try {
                //send the header to the client/browser
                out.write(header.getBytes());
                
                //write the file to the output stream
                sendFile(file, out);
                
                // Grandma always said: remember to close your files!
                file.close();
            } catch (IOException io) {System.out.println(io);}
        } catch (FileNotFoundException e) {e.printStackTrace();}
    }
    
    /**
     * List the files and sub directories of the requested parent directory
     * @param requestedDirectory: the parent directory
     * @param out: the output stream. In other words the browser.
     */
    public void writeDir(File requestedDirectory, DataOutputStream out){
        File[] dirFiles = requestedDirectory.listFiles ( );
        
        String header = requestElements.get(requestElements.size()-1) +
        " 200 OK\r\n" +
        "Date: " + new Date() + "\r\n\r\n";
        
        try{
            out.write(header.getBytes());
            
            for ( int i = 0 ; i < dirFiles.length ; i ++ ) {
                
                if (dirFiles[i].isFile()){
                    
                    out.write(formatHTMLFile(dirFiles[i]));
                }
                else if (dirFiles[i].isDirectory()){
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
            
        } catch (IOException io) {System.out.println(io);}
        
    }
    
    
    /**
     * Guess content type based on the ending of the requested file
     * {@link} http://cs.au.dk/~amoeller/WWW/javaweb/server.html
     * @param path: the name of the file. Only extension part
     * is evaluated.
     * @return: the content type to fill HTTP response value for content-type
     */
    private static String guessContentType(String path)
    {
        if (path.endsWith(".html") || path.endsWith(".htm"))
            return "text/html";
        //returns an "application/xyz" MIME header for files with type ".xyz"
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
     * Checks if request has correct format for a
     * GET request (can be modified to accept other types)
     * and that no evil hackers are trying to hack the server
     * {@code} found at http://cs.au.dk/~amoeller/WWW/javaweb/server.html
     * @param request
     * @return
     */
    private boolean requestIsValid(String request){
        //make sure this is a GET request.
        if (!request.startsWith("GET") || request.length()<14 ||
            !(request.endsWith("HTTP/1.0") || request.endsWith("HTTP/1.1"))){
            System.out.println("bad request");
            return false;
        } else{
            /*For evil hackers, check if request is not trying
             * to enter other directories*/
            String req = request.substring(4, request.length()-9).trim();
            if (req.indexOf("..")!=-1 ||
                req.indexOf("/.ht")!=-1 || req.endsWith("~"))
                return false;
        }
        
        return true;
    }
    
    
    /**
     * Read the file then write its bytes to output stream
     * @param file: input stream to read/get the requested file
     * @param out: the output streem to write the file to
     */
    private static void sendFile(InputStream file, OutputStream out)
    {
        try {
            System.out.println("Sending file to browser");
            
            //read the file
            byte[] buffer = new byte[1000];
            while (file.available()>0)
                //and write the bytes the output stream
                out.write(buffer, 0, file.read(buffer));
        } catch (IOException ex) { System.out.println(ex); }
    }
    
    /**
     * Format a file to show as a hotlink on a web browser
     * @param file: a file in a directory
     * @return the bytes to be written on the output stream
     */
    public byte[] formatHTMLFile(File file){
        String htmlFormat = "<a href=\""
        + file.getName() + "\">[FILE]   "
        + file.getName() + "</a> <br>";
        
        return htmlFormat.getBytes();
    }
    
    /**
     * Produces an appropriate header for a file. Not meant to be used
     * by a directory but it could be adapted to do so.
     * @param fileLength: the length of the file. Input 0 if directory.
     * @param file: is the name of the file to get its content type
     * @return a string containing the appropriate HTML header
     */
    public String createHeader(long fileLength, String file){
        
        String header;
        
        //In case option needs to be adapted for directories
        String h1 = requestElements.get(2) +
        " 200 OK\r\n" +
        "Content-Type: " +
        guessContentType(file) +
        "\r\n" +
        "Date: " + new Date() + "\r\n\r\n";
        
        //Most common option for files
        String h2 = requestElements.get(2) +
        " 200 OK\r\n" +
        "Content-Legth: "+
        fileLength +
        "\r\n" +
        "Content-Type: " +
        guessContentType(file) +
        "\r\n" +
        "Date: " + new Date() + "\r\n\r\n";
        
        if (fileLength == 0) header = h1;
        else header = h2;
        
        return header;
        
    }
}


/**
 * Name: Deliana Escobari       Date: Saturday February 6th, 2016
 * Java version used: 1.8
 * Compile with command: javac MyWebServer.java
 * 		or | javac *.java | once to compile all files in the whole folder
 
 * List of files needed for running the program.
 * 				MyWebServer.java
 * 				WebWorker.java
 * 				AddNums.java
 *
 * Notes: This helper class runs whenever the addNums method is invoked.
 * It facilitates the creation of the html page that responds to the form.
 */
class AddNums {
    String request;
    HashMap<String, Object> valuePairs;
    
    public AddNums(String request){
        this.request = request;
        valuePairs = new HashMap<String, Object>();
    }
    
    /**
     * Formats the user's inputs as an HTML response. 
     * @return a string with HTML formatting showing the response 
     * to the user's request. Meant to be displayed in a browser. 
     */
    public String generateResponse(){
        parse();
        String title = "<html><head><TITLE> "
        + "Response for AddNum request</TITLE></head>"
        + "<BODY style=\"background-color:lightgrey;\"><center> "
        + "<H1 style=\"font-family:verdana;\">Response for " 
        + valuePairs.get("person") + "'s AddNum Request</H1>"
        + "<H2 style=\"font-family:verdana;\">Here are the numbers you wanted to add: </H2>";
        
        
        String data = "<H3>" + valuePairs.get("num1") + " and "
        + valuePairs.get("num2") 
        + "<H2> Answer: " + addNums() + "</H2>"; 
        
        String closingTags = "</center></BODY></html>";	
        
        StringBuilder htmlPage = new StringBuilder(); 
        
        htmlPage.append(title);
        htmlPage.append(data);
        htmlPage.append(closingTags);
        
        return htmlPage.toString();
    }
    
    
    /* ********************************
     *      Helper Methods
     * ********************************     
     */
    
    /**
     * Parses through the string containing the numbers and 
     * adds them up
     * @return the addition of the two values. 
     */
    private int addNums(){
        
        int val1 = Integer.parseInt(valuePairs.get("num1").toString());
        int val2 = Integer.parseInt(valuePairs.get("num2").toString());
        return val1 + val2;
        
    }
    
    /**
     * Parses through the name-value pairs in the request and 
     * updates the valuePairs table, which is used to generate the 
     * response formatted for HTML. 
     */
    private void parse(){
        //throw out everything before '?' to get just the parameters
        String valuePairStr = request.substring(request.indexOf("?")+1);
        
        //get all the name value pairs into to table
        for (String value : valuePairStr.split("&"))
            valuePairs.put(value.substring(0, value.indexOf("=")), 
                           value.substring(value.indexOf("=")+1));
        
    }
    
}


