import com.ibm.mq.MQC;
import com.ibm.mq.MQEnvironment;
import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {

            String mqHost = "localhost"; // Hostname
            String mqPort = "1414"; // Port
            String mqChannel = "DEV.APP.SVRCONN"; // Channel 
            String mqQMgr = null;   // Queue Manager
            MQQueueManager qMgr = null;
            MQEnvironment.hostname = mqHost;
            MQEnvironment.port = Integer.valueOf(mqPort).intValue();
            MQEnvironment.channel = mqChannel;
            MQEnvironment.properties.put(MQC.TRANSPORT_PROPERTY, MQC.TRANSPORT_MQSERIES);
            MQEnvironment.userID = "app";
            MQEnvironment.password = "passw0rd";
            //java.util.Hashtable props = new java.util.Hashtable();
            //props.put(MQC.TRANSPORT_PROPERTY, MQC.TRANSPORT_MQSERIES_CLIENT);            
            MQQueueManager qm = new MQQueueManager("QM1");
            MQQueue outputQueue = qm.accessQueue("DEV.QUEUE.1", CMQC.MQOO_OUTPUT);
            MQMessage message = new MQMessage();
            message.writeUTF("This is a test message");
            outputQueue.put(message);
            outputQueue.close();
            MQQueue inputQueue = qm.accessQueue("DEV.QUEUE.1", CMQC.MQOO_INPUT_SHARED);
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
