package com.rota.app;

public class Plan {
    public static final String HOURLY = "Saatli";
    public static final String ALL_DAY = "Tüm gün";
    public static final String FLEX = "Esnek";

    public long id;
    public String date;
    public String title;
    public String type;
    // Kept for compatibility with the first Rota data format.
    public String time;
    public String duration;
    public boolean done;

    // New scheduling fields.
    public String startTime;
    public String endTime;
    public String note;

    public Plan(long id, String date, String title, String type, String time, String duration, boolean done) {
        this.id = id;
        this.date = date == null ? "" : date;
        this.title = title == null ? "" : title;
        this.type = type == null ? FLEX : type;
        this.time = time == null ? "" : time;
        this.duration = duration == null ? "" : duration;
        this.done = done;
        this.startTime = this.time;
        this.endTime = "";
        this.note = "";
    }

    public static String durationBetween(String start, String end) {
        if (start == null || end == null || !start.matches("\\d{2}:\\d{2}") || !end.matches("\\d{2}:\\d{2}")) {
            return "";
        }
        try {
            int sh = Integer.parseInt(start.substring(0, 2));
            int sm = Integer.parseInt(start.substring(3));
            int eh = Integer.parseInt(end.substring(0, 2));
            int em = Integer.parseInt(end.substring(3));
            int minutes = (eh * 60 + em) - (sh * 60 + sm);
            if (minutes <= 0) minutes += 24 * 60;
            int hours = minutes / 60;
            int mins = minutes % 60;
            if (hours > 0 && mins > 0) return hours + "s " + mins + "dk";
            if (hours > 0) return hours + "s";
            return mins + "dk";
        } catch (Exception ignored) {
            return "";
        }
    }
}
