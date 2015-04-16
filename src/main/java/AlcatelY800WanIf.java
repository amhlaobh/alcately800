
import org.rrd4j.ConsolFun;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeSet;

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
//                app.dataMap.put(sdf.format(statsObject.timestamp), statsObject);
//                try {
//                    callRRD(statsObject, app.rrdfile);
//                } catch (IOException ioe) {
//                    System.err.println("RRD could not be updated: " + ioe);
//                }
            }

            try {
                RrdDb db = app.createRrd();
                app.createRrdDbFromMap(db);
                db.close();
                app.createRrdGraphs(app.createRrdGraphDef());
            } catch (IOException e) {
                e.printStackTrace();
            }

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

    /**
     * Retrieve usage data from distributed map and feed it into RRD database.
     * @param rrdDb The rrd4j RRD database object
     * @throws IOException
     */
    private void createRrdDbFromMap(final RrdDb rrdDb) throws IOException {
        String today = sdf.format(new Date());
        TreeSet<String> usageData = dataMap.get(today);
        if (usageData != null) {
            for (String anUsageData : usageData) {
                StatsObject statsObject = StatsObject.fromString(anUsageData);
                if (verbose) {
                    System.out.printf("%s Consumed from map: %s :: %s :: %s bytes%n",
                            sdftime.format(new Date()),
                            statsObject.timestamp,
                            sdftime.format(statsObject.timestamp * 1000L), statsObject.usage);
                }
                addRrdSample(rrdDb, statsObject);
            }
        }
    }

    private void addRrdSample(final RrdDb rrdDb, StatsObject statsObject) throws IOException {
        final Sample sample = rrdDb.createSample();
//        sample.setTime(statsObject.timestamp);
        try {
            String updateStr = String.format("%s:%s:%s", statsObject.timestamp, statsObject.usage, statsObject.usage);
            sample.setAndUpdate(updateStr);
        } catch (IllegalArgumentException e) {
            System.err.println(e);
        }
    }

    private RrdDb createRrd() throws IOException {
        RrdDef rrdDef = new RrdDef(RRD_FILE, 1381816217, 5);

        rrdDef.addDatasource("DS:usage:GAUGE:20:0:U");
        rrdDef.addDatasource("DS:usageRate:COUNTER:20:0:U");

        rrdDef.addArchive("RRA:AVERAGE:0.5:1:17280");
        rrdDef.addArchive("RRA:AVERAGE:0.5:1440:112");
        rrdDef.addArchive("RRA:AVERAGE:0.5:17280:30");
        rrdDef.addArchive("RRA:MAX:0.5:1:17280");
        rrdDef.addArchive("RRA:MAX:0.5:1440:112");
        rrdDef.addArchive("RRA:MAX:0.5:17280:30");

        return new RrdDb(rrdDef);
    }

    private RrdGraphDef createRrdGraphDef() {
        RrdGraphDef gDef = new RrdGraphDef();
        gDef.setWidth(300);
        gDef.setHeight(100);

        gDef.setFilename("alcatel-abs5min.png");
        gDef.setStartTime(-60 * 5);
        gDef.setTitle("Usage");
        gDef.setVerticalLabel("bytes");

        gDef.datasource("usage", RRD_FILE, "usage", ConsolFun.MAX);
        gDef.line("usage", Color.GREEN, "Usage");

        gDef.setImageFormat("png");

        return gDef;
    }

    private void createRrdGraphs(final RrdGraphDef rrdGraphDef) throws IOException {
        new RrdGraph(rrdGraphDef); // will create the graph in the path specified
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
        if (verbose) System.out.printf ("Usagediff %d ...%n", usageDiff);
        return lastUsage;
    }

}
