CREATE OR REPLACE NETWORK RULE external_mq_network_rule
  TYPE = HOST_PORT
  VALUE_LIST = ('2.tcp.ngrok.io:14640') 
  MODE= EGRESS
;

CREATE OR REPLACE EXTERNAL ACCESS INTEGRATION external_mq_int
  ALLOWED_NETWORK_RULES = (external_mq_network_rule)
  ENABLED = true;

SELECT SEND_MESSAGE('2.tcp.ngrok.io','14640','DEV.APP.SVRCONN','QM1','DEV.QUEUE.1','app','passw0rd','my message 2');

SELECT READ_MESSAGE('2.tcp.ngrok.io','14640','DEV.APP.SVRCONN','QM1','DEV.QUEUE.1','app','passw0rd');





CREATE OR REPLACE FUNCTION SEND_MESSAGE(
 mqHost String, 
 mqPort String,
 mqChannel String, 
 mqQMgr String,  mqQueue String,
 userID String,  password String, 
 message_str String)
RETURNS STRING
LANGUAGE JAVA
 EXTERNAL_ACCESS_INTEGRATIONS = (external_mq_int)
 
HANDLER = 'Main.sendMessage'
IMPORTS = ('@mystage/com.ibm.mq.allclient-9.3.4.0.jar','@mystage/json-20231013.jar')
AS $$
import com.ibm.mq.MQC;
import com.ibm.mq.MQEnvironment;
import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;

import java.io.IOException;

public class Main {
    public static String sendMessage(
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
            qm.disconnect();
            return "done";
        } catch (MQException e) {
            return e.toString();
        } catch (IOException e) {
            return e.toString();
        }
    }
}
$$;


CREATE OR REPLACE FUNCTION READ_MESSAGE(
 mqHost String, 
 mqPort String,
 mqChannel String, 
 mqQMgr String,  mqQueue String,
 userID String,  password String)
RETURNS STRING
LANGUAGE JAVA
 EXTERNAL_ACCESS_INTEGRATIONS = (external_mq_int)
 
HANDLER = 'Main.readMessage'
IMPORTS = ('@mystage/com.ibm.mq.allclient-9.3.4.0.jar','@mystage/json-20231013.jar')
AS $$
import com.ibm.mq.MQC;
import com.ibm.mq.MQEnvironment;
import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;

import java.io.IOException;

public class Main {
   

    public static String readMessage(
            String mqHost, String mqPort,
            String mqChannel, String mqQMgr, String mqQueue,
            String userID, String password) {
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
            return e.toString();
        } catch (IOException e) {
            return e.toString();
        }
    }
}
$$;
