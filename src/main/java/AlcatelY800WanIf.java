import java.io.*;

import java.awt.Color;
import java.awt.TextArea;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


import javax.swing.JFrame;

// javac -source 1.6 -target 1.6 AlcatelY800WanIf.java

/**
 * Program to collect network statistics from an Alcatel Y800 mobile hotspot: data transferred and data rate.
 * Saves statistics in an external RRD (round robin database) for graph creation.
 */
public class AlcatelY800WanIf extends StatsProvider {
    // thresholds for download rate calculation
    private static final long USAGE_LEVEL_0 = 0;
    /**1KB*/
    private static final long USAGE_LEVEL_1 = 1024;
    /** 100KB*/
    private static final long USAGE_LEVEL_2 = 1024 * 100;

    private final JFrame jframe;
    private final TextArea textArea;
    long lastUsage = 0L;

    public static void main(String[] args) {

        if (args.length > 0 && args[0].startsWith("-h")){
            System.out.println("java [-DPORT=<port>] [-DADDR=<ipaddr>] [-DVERBOSE=1] [-DRRDFILE=xyz.rrd] AlcatelY800WanIf");
            System.exit(0);
        }

        AlcatelY800WanIf app = new AlcatelY800WanIf();

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
                app.updateGui(statsObject);
                try {
                    callRRD(statsObject, app.rrdfile);
                } catch (IOException ioe) {
                    System.err.println("RRD could not be updated: " + ioe);
                }
            }

            app.updateDataFromQueue();
            
            try {
                Thread.sleep (SLEEP_TIME);
            } catch (InterruptedException inte) {
                // don't care
            }
        }
    }

    public AlcatelY800WanIf() {
        super();

        jframe = new JFrame();
        jframe.setTitle("Alcatel Y800");
        jframe.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });

        textArea = new TextArea(20,20);
        textArea.setBackground(Color.white);
        jframe.getContentPane().add(textArea);
    }

    private void updateDataFromQueue() {
        try {
            if (queue.size() > 0) {
                StatsObject statsObject = StatsObject.fromString(queue.take());
                if (statsObject != null) {
                    if (verbose) {
                        System.out.printf("Consumed from queue: %s :: %s bytes%n",
                                sdftime.format(statsObject.timestamp * 1000L), statsObject.usage);
                    }
                    callRRD(statsObject, rrdfile);
                }
            }
        } catch(InterruptedException e) {
            e.printStackTrace();
        } catch(IOException ioe) {
            System.err.println("RRD could not be updated from queue: " + ioe);
        }
    }

    /**
     * Update GUI with current statistics.
     */
    void updateGui(StatsObject statsObject) {
        jframe.setSize(200, 65);
        jframe.setVisible(true);
        jframe.setAutoRequestFocus(false);
        jframe.setTitle(formatUsage(String.valueOf(statsObject.usage)));
        lastUsage = updateUsageColour(textArea, lastUsage, statsObject.usage);
    }

    /** colour the textarea according to the per-second usage rate*/
    private static long updateUsageColour(TextArea textArea, long lastUsage, long usage) {
        long usageDiff = usage - lastUsage;
        lastUsage = usage;
        if (usageDiff / ONE_SECOND > USAGE_LEVEL_2) {
            textArea.setBackground(Color.red);
        } else if (usageDiff / ONE_SECOND > USAGE_LEVEL_1) {
            textArea.setBackground(Color.yellow);
        } else if (usageDiff / ONE_SECOND > USAGE_LEVEL_0) {
            textArea.setBackground(Color.green);
        } else {
            textArea.setBackground(Color.white);
        }
        textArea.setText(String.valueOf(usageDiff / ONE_SECOND + " bytes/sec"));
        if (verbose) System.out.printf ("Usagediff %d ... ", usageDiff);
        return lastUsage;
    }

}
