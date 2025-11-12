package view;

import javax.swing.*;
import java.awt.*;
import model.Task;
import java.util.List;

/**
 * Polished time-based Gantt panel.
 * - Left column: "Task N: Title"
 * - Rounded light-blue bars positioned using parsed start/end times
 * - Bottom timeline with ticks and formatted labels
 * - Preferred size computed so JScrollPane shows scrollbars when needed
 */
public class GanttChartPanel extends JPanel {
    private List<Task> tasks;

    public GanttChartPanel(List<Task> tasks) {
        this.tasks = tasks;
        setPreferredSize(new Dimension(900, 300));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (tasks == null || tasks.isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));

        int n = tasks.size();
        int leftWidth = 220;
        int topMargin = 30;
        int barHeight = 22;
        int vGap = 18;
        int bottomMargin = 80;

        // Parse times to epoch millis
        long[] starts = new long[n];
        long[] ends = new long[n];
        long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            Task t = tasks.get(i);
            long s = parseToMillis(t.getStart());
            long e = parseToMillis(t.getEnd());
            if (s <= 0) s = System.currentTimeMillis();
            if (e <= 0) e = s + 60*60*1000;
            starts[i] = s; ends[i] = e;
            if (s < min) min = s;
            if (e > max) max = e;
        }
        if (min >= max) max = min + 60*60*1000;

        // Adjust left label column width so chart bars never overlap task names
        FontMetrics fm = g2.getFontMetrics();
        int maxLabelWidth = 0;
        for (int i = 0; i < n; i++) {
            Task t = tasks.get(i);
            String title = t.getTitle() == null ? "" : t.getTitle();
            String label = "Task " + (i+1) + ": " + title;
            maxLabelWidth = Math.max(maxLabelWidth, fm.stringWidth(label));
        }
        // Padding for the label column and a small gap before the chart
        int labelPadding = 24;
        int labelToChartGap = 20;
        leftWidth = Math.max(140, Math.min(420, maxLabelWidth + labelPadding));

        
        int viewWidth = getWidth();
        java.awt.Container p = getParent();
        if (p instanceof JViewport) {
            viewWidth = ((JViewport) p).getExtentSize().width;
        }
        int xStart = leftWidth + labelToChartGap;
        int windowAvailable = Math.max(300, viewWidth - xStart - 50);
        double totalMs = (double) (max - min);

        // Candidate tick intervals (from 15 minutes up to a week)
        long[] candidates = new long[] {
            15*60*1000L, 30*60*1000L, 60*60*1000L, 3*60*60*1000L,
            6*60*60*1000L, 12*60*60*1000L, 24*60*60*1000L,
            3*24*60*60*1000L, 7*24*60*60*1000L
        };
        int desiredTicks = 7;
        long tickMs = candidates[candidates.length-1];
        for (long c : candidates) {
            double cnt = totalMs / c;
            if (cnt <= desiredTicks + 1) { tickMs = c; break; }
        }

        // Determine pixels-per-tick from the visible area and clamp it so labels remain readable and chart stays compact
        final double MIN_PX_PER_TICK = 60;  // minimum space for a tick label
        final double MAX_PX_PER_TICK = 140; // tighter max to keep chart compact
        double idealPxPerTick = windowAvailable / (double) desiredTicks;
        double pxPerTick = Math.max(MIN_PX_PER_TICK, Math.min(MAX_PX_PER_TICK, idealPxPerTick));
        double pxPerMs = pxPerTick / (double) tickMs;
        int availableWidth = (int) Math.ceil(pxPerMs * totalMs);

    int chartHeight = topMargin + n * (barHeight + vGap) + bottomMargin;
    setPreferredSize(new Dimension(xStart + availableWidth + 50, chartHeight));

        // Draw task labels and bars
        int y = topMargin;
        for (int i = 0; i < n; i++) {
            Task t = tasks.get(i);
            String title = t.getTitle() == null ? "" : t.getTitle();
            String label = "Task " + (i+1) + ": " + title;
            g2.setColor(Color.BLACK);
            g2.drawString(label, 12, y + barHeight/2 + 6);

            int x1 = xStart + (int)Math.round((starts[i] - min) * pxPerMs);
            int x2 = xStart + (int)Math.round((ends[i] - min) * pxPerMs);
            if (x2 <= x1) x2 = x1 + 6;
            int w = x2 - x1;

            g2.setColor(new Color(158, 209, 255));
            g2.fillRoundRect(x1, y, w, barHeight, 8, 8);
            g2.setColor(new Color(30, 90, 160));
            g2.drawRoundRect(x1, y, w, barHeight, 8, 8);

            y += barHeight + vGap;
        }

        // Draw bottom timeline
    int axisY = topMargin + n * (barHeight + vGap) + 10;
    int xEnd = xStart + availableWidth;
        g2.setColor(new Color(130,130,130));
        g2.drawLine(xStart, axisY, xEnd, axisY);

    // tickMs and pxPerMs were computed above based on the viewport and clamped px-per-tick

        java.time.ZoneId zid = java.time.ZoneId.systemDefault();
        java.time.format.DateTimeFormatter fmt = (tickMs >= 24*60*60*1000L)
                ? java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")
                : java.time.format.DateTimeFormatter.ofPattern("dd-MM HH:mm");

        long firstTick = (min / tickMs) * tickMs;
        if (firstTick < min) firstTick += tickMs;

        int lastLabelRight = -10000;
        for (long tms = firstTick; tms <= max + tickMs; tms += tickMs) {
            int x = xStart + (int)Math.round((tms - min) * pxPerMs);
            // no vertical grid lines: only small tick marker
            g2.setColor(new Color(120,120,120));
            g2.drawLine(x, axisY-6, x, axisY+6);
            java.time.ZonedDateTime zdt = java.time.Instant.ofEpochMilli(tms).atZone(zid);
            String lbl = zdt.format(fmt);
            int lw = fm.stringWidth(lbl);
            int lblLeft = x - lw/2;
            if (lblLeft > lastLabelRight + 8) {
                g2.drawString(lbl, lblLeft, axisY + 20);
                lastLabelRight = lblLeft + lw;
            }
        }
    }

    private long parseToMillis(String dateStr) {
        if (dateStr == null) return -1;
        dateStr = dateStr.trim();
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\d{8})(\\d{4})?([+-]\\d{4})?$").matcher(dateStr);
            if (m.matches()) {
                String dpart = m.group(1);
                String tpart = m.group(2);
                String off = m.group(3);
                java.time.LocalDate d = java.time.LocalDate.parse(dpart, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
                java.time.LocalDateTime ldt;
                if (tpart != null) {
                    int hh = Integer.parseInt(tpart.substring(0,2));
                    int mm = Integer.parseInt(tpart.substring(2,4));
                    ldt = d.atTime(hh, mm);
                } else {
                    ldt = d.atStartOfDay();
                }
                if (off != null) {
                    java.time.ZoneOffset zo = java.time.ZoneOffset.of(off);
                    java.time.OffsetDateTime odt = java.time.OffsetDateTime.of(ldt, zo);
                    return odt.toInstant().toEpochMilli();
                } else {
                    return ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                }
            }
        } catch (Exception ex) {}
        String[] patterns = new String[]{"yyyyMMddHHmm","yyyyMMddHH","yyyy-MM-dd HH:mm","yyyy-MM-dd"};
        for (String p : patterns) {
            try {
                java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern(p);
                if (p.equals("yyyy-MM-dd")) {
                    java.time.LocalDate ld = java.time.LocalDate.parse(dateStr, f);
                    return ld.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                } else {
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(dateStr, f);
                    return ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                }
            } catch (Exception e) {}
        }
        return -1;
    }
}
