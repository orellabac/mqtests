import com.ibm.mq.MQC;
import com.ibm.mq.MQEnvironment;
import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;

import java.io.IOException;

public class Main {
    public static void sendMessage(
            String mqHost, String mqPort,
            String mqChannel, String mqQMgr, String mqQueue,
            String userID, String password, String message_str) {
        try {
            MQEnvironment.hostname = mqHost;
            MQEnvironment.port = Integer.valueOf(mqPort).intValue();
            MQEnvironment.channel = mqChannel;
            MQEnvironment.properties.put(MQC.TRANSPORT_PROPERTY, MQC.TRANSPORT_MQSERIES);
            MQEnvironment.userID = userID;
            MQEnvironment.password = password;
            MQQueueManager qm = new MQQueueManager(mqQMgr);
            MQQueue outputQueue = qm.accessQueue(mqQueue, CMQC.MQOO_OUTPUT);
            MQMessage message = new MQMessage();
            message.writeUTF(message_str);
            outputQueue.put(message);
            outputQueue.close();
            MQQueue inputQueue = qm.accessQueue(mqQueue, CMQC.MQOO_INPUT_SHARED);
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

    public static String readMessage(
            String mqHost, String mqPort,
            String mqChannel, String mqQMgr, String mqQueue,
            String userID, String password, String message_str) {
        try {
            MQEnvironment.hostname = mqHost;
            MQEnvironment.port = Integer.valueOf(mqPort).intValue();
            MQEnvironment.channel = mqChannel;
            MQEnvironment.properties.put(MQC.TRANSPORT_PROPERTY, MQC.TRANSPORT_MQSERIES);
            MQEnvironment.userID = userID;
            MQEnvironment.password = password;
            MQQueueManager qm = new MQQueueManager(mqQMgr);

            MQQueue inputQueue = qm.accessQueue(mqQueue, CMQC.MQOO_INPUT_SHARED);
            MQMessage readMessage = new MQMessage();
            inputQueue.get(readMessage);
            inputQueue.close();
            qm.disconnect();
            return readMessage.readUTF();
        } catch (MQException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        String mqHost = "4.tcp.us-cal-1.ngrok.io";//"localhost"; // Hostname
        String mqPort = "16800";//"1414"; // Port
        String mqChannel = "DEV.APP.SVRCONN"; // Channel
        String mqQMgr = "QM1"; // Queue Manager
        String mqQueue = "DEV.QUEUE.1";// Queue
        String user = "app";
        String pass = "passw0rd";
        sendMessage(mqHost, mqPort, mqChannel, mqQMgr, mqQueue, user, pass, "mymessage");
    }
}
