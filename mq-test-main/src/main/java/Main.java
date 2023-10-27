import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            MQQueueManager qm = new MQQueueManager("BMOBILE");
            MQQueue outputQueue = qm.accessQueue("IN_QUEUE", CMQC.MQOO_OUTPUT);
            MQMessage message = new MQMessage();
            message.writeUTF("This is a test message");
            outputQueue.put(message);
            outputQueue.close();
            MQQueue inputQueue = qm.accessQueue("IN_QUEUE", CMQC.MQOO_INPUT_SHARED);
            MQMessage readMessage = new MQMessage();
            inputQueue.get(readMessage);
            inputQueue.close();
            qm.disconnect();
            System.out.println("Received message: " + readMessage.readUTF());
        } catch (MQException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
