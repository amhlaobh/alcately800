import java.io.*;
import java.net.*;
import java.text.*;
import java.util.regex.*;
import java.util.*;


// javac -source 1.6 -target 1.6 AlcatelY800WanIf.java

public class AnalyseData {
    private static final String FILE_COOKED = "wanif.rrdinput"; 
    private static final Pattern pattern = Pattern.compile("(\\d+?):(\\d+?):(\\d+?)");
    private static File cookedFile;

    private static final boolean verbose = isNotNullAndNotEmpty(System.getProperty("VERBOSE"));
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
	
    public static void main(String[] args) {
        System.out.printf(":%10s %9s %8s %11s %12s %12s %12s%n", "timestamp", "Date", "", "usage", 
                "", "Sum", "Total"); 
        long total = 0L;
        for (String inFile : args) {
            BufferedReader br = null;
            long sum = 0;
            try {
                InputStream in = new FileInputStream(inFile);
                InputStreamReader isr = new InputStreamReader(in, "ISO-8859-1");
                br = new BufferedReader(isr);
                String line = null;
				String lastday = null;
                long lastusage = 0L;
                long timestamp = 0L;
                long lasttimestamp = 0L;
                long usage = 0;
                while ((line = br.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        try {
                            timestamp = Long.parseLong(matcher.group(1));
							String day = sdf.format(new Date(timestamp));
							if (lastday == null) lastday = day;
                            usage = Long.parseLong(matcher.group(2));
                            if (/*usage < lastusage || */!day.equals(lastday)) {
                                sum += lastusage;
                                total += lastusage;
                                Date date = new Date(lasttimestamp*1000);
                                if (verbose) {
                                    System.out.printf(":%d %tF %TT %11d %12s %12s %12s%n", lasttimestamp, date, date, lastusage, 
                                        formatKiBMiBGiB(lastusage), formatKiBMiBGiB(sum), formatKiBMiBGiB(total)); 
                                }
                            }
                        } catch (NumberFormatException nfe) {
                            System.err.println(line + nfe);
                        }
                    }
                    lastusage = usage;
					lasttimestamp = timestamp;
                }
                Date date = new Date(lasttimestamp*1000);
                sum += lastusage;
                total += lastusage;
                if (verbose) {
                    //System.out.printf(":%d %tF %TT %11d %12s%n", lasttimestamp, date, date, lastusage, formatKiBMiBGiB(lastusage)); 
                    System.out.printf(":%d %tF %TT %11d %12s %12s %12s%n", lasttimestamp, date, date, lastusage, 
                            formatKiBMiBGiB(lastusage), formatKiBMiBGiB(sum), formatKiBMiBGiB(total)); 
                }
            } catch (IOException ioe) {
                System.err.println ("Can't open " + inFile + ioe);
            } finally {
                closeSilently (br);
            }
            if (!verbose) System.out.printf("%d%n", sum);
        }
        if (verbose) {
            System.out.printf(":TOTAL = %d %s %s%n", total, formatKiBMiBGiB(total), formatKiBMiBGiB(total));
        }
    }

    private static void closeSilently (Closeable... closeables) {
        for (Closeable c : closeables) {
            try {
                if (c != null) c.close();
            } catch (IOException e) {
                // don't care
            }
        }
    }

    private static String formatKiBMiBGiB(long bytes){
        String unit = "";
        double formattedBytes = 0.0;
        
        if (bytes < 1024 ) {
            unit = "bytes";
            formattedBytes = bytes;
        } else if (bytes < 1024.0*1024.0) {
            unit = "KiB";
            formattedBytes = bytes / 1024.0;
        } else if (bytes < 1024.0*1024.0*1024.0) {
            unit = "MiB";
            formattedBytes = bytes / (1024.0 * 1024.0);
        } else if (bytes < 1024.0*1024.0*1024.0*1024.0) {
            unit = "GiB";
            formattedBytes = bytes / (1024.0 * 1024.0 * 1024.0);
        }

        return String.format ("%4.2f %s", formattedBytes, unit);
    }

    private static boolean isNotNullAndNotEmpty (String s){
        if (s == null) return false;
        if ("".equals(s)) return false;
        return true;
    }
}
