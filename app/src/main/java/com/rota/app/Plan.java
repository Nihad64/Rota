package com.rota.app;

public class Plan {
    public static final String HOURLY = "Saatli";
    public static final String ALL_DAY = "Tüm gün";
    public static final String FLEX = "Esnek";

    public long id;
    public String date;
    public String title;
    public String type;
    public String time;
    public String duration;
    public boolean done;

    public Plan(long id, String date, String title, String type, String time, String duration, boolean done) {
        this.id = id;
        this.date = date;
        this.title = title;
        this.type = type;
        this.time = time == null ? "" : time;
        this.duration = duration == null ? "" : duration;
        this.done = done;
    }
}
