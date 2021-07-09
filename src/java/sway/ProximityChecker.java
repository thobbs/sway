package sway;

import java.util.ArrayList;

public class ProximityChecker {

    public ArrayList<Point> points;

    public ProximityChecker() {
        points = new ArrayList<>();
    }

    public boolean checkForCollision(double x, double y, double margin, int segID) {
        for (Point p : points) {
            double effectiveMargin = margin + p.margin;
            if (segID != p.segID) {
                double dist = Math.sqrt(Math.pow(x - p.x, 2) + Math.pow(y - p.y, 2));
                if (dist < effectiveMargin)
                    return true;
            }
        }
        return false;
    }

    public void addPoint(double x, double y, double margin, int segID) {
        points.add(new Point(x, y, margin, segID));
    }

    private class Point {
        public double x;
        public double y;
        public double margin;
        public int segID;

        public Point (double x, double y, double margin, int segID) {
            this.x = x;
            this.y = y;
            this.margin = margin;
            this.segID = segID;
        }
    }
}
