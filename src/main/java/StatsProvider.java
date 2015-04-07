import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Headless base class to collect statistics from Alcatel Y800 mobile hotspot.
 * Sends stats over the network to a distributed queue where it can be picked
 * up by the GUI version of this collector.
 */
public class StatsProvider {
    // carriage return character
    private static final char CR = 13;
    // line feed character
    private static final char LF = 10;
    // sleep time in milliseconds between queries of device; must be same as rrdtool create "--step" value
    static final int SLEEP_TIME = 5000;
    static final long ONE_SECOND = SLEEP_TIME / 1000;

    // factory IP address of mobile hotspot device
    private static final String DEFAULT_IP_ADDRESS = "192.168.1.1";
    // factory port number of mobile hotspot device
    private static final int DEFAULT_PORT = 80;

    // REST resource to query on device
    private static final String WANIF_STR = "/goform/getWanInfo";
    // file prefix for raw pulled data from URL:
    private static final String OUTPUT_FILE_RAW = "wanif.data";
    // file prefix for only "usage" data, prepared for input into "rrdtool update":
    private static final String OUTPUT_FILE_COOKED = "wanif.rrdinput";
    // file name of round robin database
    private static final String RRD_FILE = "alcately800.rrd";

    // statistics string pattern from REST return value (JSON)
    private static final Pattern usagePattern = Pattern.compile("\"wan_state\":2.*\"usage\":(\\d+?),");
    // if system property VERBOSE is set, print more information
    static final boolean verbose = isNotNullAndNotEmpty(System.getProperty("VERBOSE"));
    // date formatter in the form 20141230
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    // date formatter in the form 20141230 12:34:56
    static SimpleDateFormat sdftime = new SimpleDateFormat("yyyyMMdd HH:mm:ss z");

    // Hazelcast distributed queue info
    static final String QUEUE_NAME = "y800stats";
    private final HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();
    final IQueue<String> queue = hazelcastInstance.getQueue( QUEUE_NAME );

    private int modemPort = DEFAULT_PORT;
    private String ipAddr = DEFAULT_IP_ADDRESS;
    String rrdfile = RRD_FILE;

    public static void main(String[] args) {
        StatsProvider app = new StatsProvider();

        while (true) {
            // signal to the process to stop
            File stopFile = new File("stop");
            if (stopFile.exists()) {
                if (!stopFile.delete()) {
                    System.err.println("Stop file " + stopFile.getAbsolutePath() + " could not be deleted");
                }
                System.exit(0);
            }

            StatsObject statsObject = app.scrapeIndexPage();
            if (statsObject != null && statsObject.usage >= 0) {
                app.updateQueue(statsObject);
            }

            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException inte) {
                // don't care
            }

        }
    }

    /**
     * Initialize this class with the user provided system properties for port
     * and IP address of device and RRD database file name
     */
    public StatsProvider() {
        final String propPort = System.getProperty("PORT");
        if (isNotNullAndNotEmpty(propPort)) modemPort = Integer.parseInt(propPort);

        final String propIpAddr = System.getProperty("ADDR");
        if (isNotNullAndNotEmpty(propIpAddr)) ipAddr = propIpAddr;

        final String proprrdfile = System.getProperty("RRDFILE");
        if (isNotNullAndNotEmpty(proprrdfile)) rrdfile = proprrdfile;
    }

    /**
     * Sends statsObject to distributed queue.
     */
    void updateQueue (StatsObject statsObject) {
        try {
            if (verbose) {
                System.out.println ("Sending to queue: " + statsObject);
            }
            queue.put(statsObject.toString());
        } catch (InterruptedException e) {
            System.err.printf("%s %s Hazelcast queue put was interrupted%n", sdftime.format(new Date()), e);
        }
    }

    /**
     * Writes statsObject into RRD database using external rrdtool command.
     */
    static void callRRD(StatsObject statsObject, String rrdfile) throws IOException {
        String updateString = String.format("%d:%d:%d", statsObject.timestamp, statsObject.usage, statsObject.usage);
        if (verbose) System.out.print(statsObject);

        // only the relevant data for rrdtool
        BufferedWriter cookedFw = new BufferedWriter (
                new OutputStreamWriter(new FileOutputStream(OUTPUT_FILE_COOKED + getDateSuffix(), true/*append*/), "ISO-8859-1"));
        writeLineAndClose(cookedFw, String.format("%s%n", updateString));
        if (cmd("rrdtool", "update", rrdfile, updateString)) {
            cmd("alcatel-graph.bat");
        }
    }

    /**
     * Retrieves mobile hotspot data usage page and extracts usage statistics.
     * @return a container object that holds the data usage at a certain timestamp
     */
    StatsObject scrapeIndexPage () {
        InetAddress modemIpAddr = null;
        try {
            modemIpAddr = InetAddress.getByName(ipAddr);
        } catch (UnknownHostException uhe) {
            System.err.printf ("%s %s%n", sdftime.format(new Date()), uhe);
        }

        BufferedReader socketReader = null;
        BufferedWriter socketWriter = null;
        Socket socket = null;

        if (verbose) System.out.printf ("Connect to %s:%d ... ", modemIpAddr, modemPort);
        StatsObject statsObject = null;

        try {
            socket = new Socket(modemIpAddr, modemPort);
            socket.setSoTimeout(10000);

            OutputStream out = socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(out, "ISO-8859-1" );
            socketWriter = new BufferedWriter(osw);

            String req = String.format("GET %s HTTP/1.0%c%c%c%c", WANIF_STR, CR, LF, CR, LF);
            socketWriter.write(req);
            socketWriter.flush();

            InputStream in = socket.getInputStream();
            socketReader = new BufferedReader(new InputStreamReader(in, "ISO-8859-1"));

            String line;
            boolean inHeader = true; // skip HTTP header
            while ((line = socketReader.readLine()) != null) {
                if (!inHeader) {
                    long now = System.currentTimeMillis() / 1000;
                    if (verbose) System.out.println(line);

                    // save raw JSON data
                    BufferedWriter rawFw = new BufferedWriter (
                            new OutputStreamWriter(new FileOutputStream(OUTPUT_FILE_RAW + getDateSuffix(), true/*append*/), "ISO-8859-1"));
                    writeLineAndClose(rawFw, String.format("%d:%s%n", now, line));
                    Matcher matcher = usagePattern.matcher(line);
                    if (matcher.find()) {
                        statsObject = new StatsObject(now, Long.parseLong(matcher.group(1)));
                    }
                }
                if ("".equals(line)) inHeader = false;
            }
        } catch (IOException ex) {
            System.err.printf ("%s %s%n", sdftime.format(new Date()), ex);
        } finally {
            closeSilently (socketWriter, socketReader, socket);
        }
        return statsObject;

    }

    // make sure for each day one file
    private static String getDateSuffix() {
        return "." + sdf.format(new Date());
    }

    /** Writes out the input String to the BufferedWriter and closes this afterwards.
     *
     * @param bw BufferedWriter to write to
     * @param line String to write out
     */
    private static void writeLineAndClose(BufferedWriter bw, String line) {
        try {
            bw.write(line);
            bw.flush();
        } catch (IOException ioe) {
            System.err.printf ("%s %s%n", sdftime.format(new Date()), ioe);
        } finally {
            closeSilently(bw);
        }
    }

    /** Close the Closeables and only writes out a message to SysErr if an IOException occurrs.
     *
     * @param closeables List of Closeable resources to close.
     */
    private static void closeSilently (Closeable... closeables) {
        for (Closeable c : closeables) {
            try {
                if (c != null) c.close();
            } catch (IOException e) {
                // don't care
                System.err.printf ("%s %s%n", sdftime.format(new Date()), e);
            }
        }
    }

    /**
     * Checks for !Null !empty
     * @param s String to test
     * @return true if String is not null and not empty, false otherwise
     */
    static boolean isNotNullAndNotEmpty (String s){
        return s != null && !"".equals(s);
    }

    /** Executes an operating system command.
     *
     * @param command operating system command to execute
     * @return true if execution was successful, false if an IOException occurred or something was written to STDERR
     */
    private static boolean cmd (String... command) {
        BufferedReader inStreamReader = null;
        BufferedReader errStreamReader = null;
        try {
            if (verbose) System.out.println(" calling: " + Arrays.asList(command));

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            inStreamReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errStreamReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String s;
            StringBuilder sb = new StringBuilder();
            while ((s = inStreamReader.readLine()) != null){
                sb.append(s);
            }
            if (verbose && sb.length() > 0) {
                System.err.println("STDOUT: " + sb.toString());
            }

            sb = new StringBuilder();
            while ((s = errStreamReader.readLine()) != null){
                sb.append(s);
            }
            if (sb.length() > 0) {
                if (verbose) System.err.println("STDERR: " + sb.toString());
                return false;
            }
        } catch (IOException e) {
            System.err.printf ("%s %s%n", sdftime.format(new Date()), e);
            return false;
        } finally {
            closeSilently (inStreamReader, errStreamReader);
        }

        return true;
    }


    /** Turns the input string usage into a number and converts this into a String that is based on MiBytes.
     *
     * @param usage number in String form to format, must be parseable as long
     * @return input number formatted as MiB
     */
    static String formatUsage(String usage) {
        return formatUsage(Long.parseLong(usage));
    }

    /** Turns the input into a String that is based on MiBytes, i.e. divided by 1024 divided by 1024.
     *
     * @param usageLong number to format
     * @return input number formatted as MiB
     */
    static String formatUsage(long usageLong) {
        long usageFloat = (long)(usageLong / 1024F / 1024F);
        return String.valueOf(usageFloat) + "MiB";
    }

}

