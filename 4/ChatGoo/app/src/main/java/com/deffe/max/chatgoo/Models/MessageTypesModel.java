package com.deffe.max.chatgoo.Models;

public class MessageTypesModel
{
    private String message;
    private String from;
    private String type;
    private String key;
    private String time;
    private String today_date;
    private String date;
    private String size;
    private String video_thumb_url;

    public String getVideo_thumb_url() {
        return video_thumb_url;
    }

    public void setVideo_thumb_url(String video_thumb_url) {
        this.video_thumb_url = video_thumb_url;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getToday_date() {
        return today_date;
    }

    public void setToday_date(String today_date) {
        this.today_date = today_date;
    }
}
