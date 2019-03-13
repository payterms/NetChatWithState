package client;

import java.io.*;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.input.ReversedLinesFileReader;

public class ChatLogger{
    private static String logFileName;
    private static BufferedWriter logFileWriter;
    private static ReversedLinesFileReader logFileReader;
    private static final Pattern MSG_PATTERN = Pattern.compile("^[\\[]([1-2][0-9][0-9][0-9][.][0-1][0-9][.][0-9][0-9][ ][0-9][0-9][:][0-9][0-9][:][0-9][0-9])[\\]][ ](\\w+)[-][>](\\w+)[:]((\\w+(\\s|$))+)");
    private static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

    public ChatLogger(String logFileName) throws IOException {
        this.logFileName = logFileName;
        this.logFileWriter = new BufferedWriter(new FileWriter(logFileName, true));
        this.logFileReader = new ReversedLinesFileReader(new File(logFileName));
        // 2.5 this.logFileReader = new ReversedLinesFileReader(new File(logFileName), Charset.forName("UTF-8"));
    }
    public void logWriteString(String logMessage) throws IOException{
        logFileWriter.write(logMessage);
    }
    public String[] logReadLastNLines(int linesCount) throws IOException{
        String[] lines = {"",""};
        for (int i = 0; i <linesCount ; i++) {
            lines[i] = logFileReader.readLine();
        }
        return lines;
    }
    public Message restoreMsgFromLog(){
        String line=null;
        try {
            line = logFileReader.readLine();
            if(line != null) {
                Matcher matcher = MSG_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String dateTime = matcher.group(1);
                    String userFrom = matcher.group(2);
                    String userTo = matcher.group(3);
                    String msgText = matcher.group(4);
                    LocalDateTime localDateTime = LocalDateTime.parse(dateTime, dateFormat);
                    Message msg = new Message(userFrom, userTo, msgText, localDateTime);
                    return msg;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e1) {
            e1.printStackTrace();
        }
        return null;
    }
    public static void logClose(){
        try {
            logFileWriter.close();
            logFileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logWriteMsg(Message msg) throws IOException{
        logFileWriter.write(String.format("[%s] %s->%s:%s\n", msg.getDateTime(), msg.getUserFrom(),msg.getUserTo(),msg.getText()));
        logFileWriter.flush();
    }




}
