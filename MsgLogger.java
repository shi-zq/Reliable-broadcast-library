import java.io.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MsgLogger {

    private static Logger msgLogger = Logger.getLogger("log");
    private ReliableMsg msg;
    private String state;

    public MsgLogger() {}

    public synchronized void printLog(ReliableMsg reliableMsg, String type, IOException exp, String state) throws IOException {
        if (type.equals(Constants.MSG_SUCC)){
            if(reliableMsg != null){
                this.state = state;
                this.msg = reliableMsg;
                // Default level set as INFO
                msgLogger.setLevel(Level.INFO);
                printMsg();
            }
        } else {
            if(exp != null) {
                msgLogger.setLevel(Level.SEVERE);
                printException(exp);
            }
        }

    }
    private void printMsg() throws IOException {
        File file = new File("log_msg.txt");
        if (!file.exists()) {
            file.createNewFile();
        }
        PrintWriter printWriter = new PrintWriter(new FileOutputStream(file, true /* append = true */));
        String info = "[" + timeFormat(msg.getTimestamp()) + " " + state + "] :"
                + "msg type : " + msg.getType() + " , "
                + "from : " + msg.getFrom() + " , "
                + "view : " + msg.getView() + " , "
                + "body : " + msg.getBody() + " , "
                + "\n";
        printWriter.print(info);
        System.out.println(info);
        printWriter.close();
    }

    private void printException(IOException exp) throws IOException {
        File file = new File("log_msg.txt");
        // Create the file if it is not exist
        if(!file.exists()) {
            file.createNewFile();
        }
        PrintWriter printWriter = new PrintWriter(new FileOutputStream(file, true /* append = true */));
        String info = exp.toString();
        printWriter.print(info);
        printWriter.close();
    }

    private String timeFormat(long timestamp){
        Date date = new Timestamp(timestamp);
        SimpleDateFormat sdf= new SimpleDateFormat(Constants.DATE_PATTERN_FULL);
        return sdf.format(date);
    }
}
