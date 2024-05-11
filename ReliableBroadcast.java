import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ReliableBroadcast {


    public static void main(String[] args) throws IOException, InterruptedException {
        FileWriter fileWriter = new FileWriter("log.txt");
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.print("Some String");
        printWriter.printf("Product name is %s and its price is %d $", "iPhone", 1000);
        printWriter.close();

    }
}