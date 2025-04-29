import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;


public class Driver {

    private static JMapViewer map;
    private static JComboBox<Integer> timeComboBox;
    private static JCheckBox stopsCheckBox;
    private static JButton playButton;
    private static Timer timer;
    private static ArrayList<TripPoint> points;
    private static int currentIndex = 0;
    private static IconMarker raccoonMarker;

    public static void main(String[] args) {
    	System.setProperty("http.agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) DriverApp/1.0");


        try {
            TripPoint.readFile("triplog.csv");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error reading triplog.csv!");
            return;
        }

        JFrame frame = new JFrame("Trip Animation Map");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        map = new JMapViewer();
        frame.add(map, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();

        Integer[] times = {15, 30, 60, 90}; // seconds
        timeComboBox = new JComboBox<>(times);
        controlPanel.add(new JLabel("Animation Time (sec):"));
        controlPanel.add(timeComboBox);

        stopsCheckBox = new JCheckBox("Include Stops");
        controlPanel.add(stopsCheckBox);

        playButton = new JButton("Play");
        controlPanel.add(playButton);

        frame.add(controlPanel, BorderLayout.SOUTH);

        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startAnimation();
            }
        });

        frame.setVisible(true);
    }

    private static void startAnimation() {
        // Stop existing animation
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }

        map.removeAllMapMarkers();
        map.removeAllMapPolygons();

        try {
            if (stopsCheckBox.isSelected()) {
                points = TripPoint.getTrip();
            } else {
                TripPoint.h1StopDetection(); // or h2StopDetection()
                points = TripPoint.getMovingTrip();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        if (points.isEmpty()) {
            return;
        }

        TripPoint first = points.get(0);
        map.setDisplayPosition(new Coordinate(first.getLat(), first.getLon()), 12);

        Image raccoonImage = new ImageIcon("raccoon.png").getImage();
        raccoonMarker = new IconMarker(new Coordinate(first.getLat(), first.getLon()), raccoonImage);
        map.addMapMarker(raccoonMarker);

        currentIndex = 1; // start at second point

        int totalAnimationTimeInMs = (int) timeComboBox.getSelectedItem() * 1000;
        int delay = totalAnimationTimeInMs / points.size();

        timer = new Timer(delay, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentIndex >= points.size()) {
                    timer.stop();
                    return;
                }

                TripPoint previous = points.get(currentIndex - 1);
                TripPoint current = points.get(currentIndex);

                // Draw a small segment between previous and current points
                ArrayList<Coordinate> segmentCoords = new ArrayList<>();
                segmentCoords.add(new Coordinate(previous.getLat(), previous.getLon()));
                segmentCoords.add(new Coordinate(current.getLat(), current.getLon()));
                segmentCoords.add(new Coordinate(previous.getLat(), previous.getLon())); // close polygon

                MapPolygonImpl segment = new MapPolygonImpl(segmentCoords);

                map.addMapPolygon(segment);

                // Move raccoon marker
                map.removeMapMarker(raccoonMarker);
                raccoonMarker = new IconMarker(new Coordinate(current.getLat(), current.getLon()), raccoonImage);
                map.addMapMarker(raccoonMarker);

                currentIndex++;
            }
        });

        timer.start();
    }
}
