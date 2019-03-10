package client;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message {

    private String userFrom;

    private String userTo;

    private String text;

    private LocalDateTime dateTime;

    public Message() {
        this.userFrom = "";
        this.userTo = "";
        this.text = "";
        this.dateTime = LocalDateTime.now();
    }

    public Message(String userFrom, String userTo, String text) {
        this.userFrom = userFrom;
        this.userTo = userTo;
        this.text = text;
        this.dateTime = LocalDateTime.now();
    }

    public Message(String userFrom, String userTo, String text, LocalDateTime dateTime) {
        this.userFrom = userFrom;
        this.userTo = userTo;
        this.text = text;
        this.dateTime = dateTime;
    }

    private static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

    public String getDateTime() {
        return dateTime.format(dateFormat);
    }

    public String getUserFrom() {
        return userFrom;
    }

    public String getUserTo() {
        return userTo;
    }

    public String getText() {
        return text;
    }

    public LocalDate getDate() {
        return dateTime.toLocalDate();
    }
}
