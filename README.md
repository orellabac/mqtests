# Testing Accessing MQ Series from Snowflake


I was discussing about the possibility of using MQ Series directly from Snowflake due to the fact that (External Network Access)[https://docs.snowflake.com/developer-guide/external-network-access/creating-using-external-network-access] is now available.

In order to do that I create a simple script to setup MQ. I am doing that with docker following the code from (here)[https://developer.ibm.com/tutorials/mq-connect-app-queue-manager-containers/]

I have put all the instructions is `script.sh`, if your system has docker, you can simply do: `source script.sh``

That creates a MQ Server with these settings:
```java
        String mqHost = "localhost"; // Hostname
        String mqPort = "1414"; // Port
        String mqChannel = "DEV.APP.SVRCONN"; // Channel
        String mqQMgr = "QM1"; // Queue Manager
        String mqQueue = "DEV.QUEUE.1";// Queue
        String user = "app";
        String pass = "passw0rd";
```

I create some test code that you can see at `src/main/java/Main.java`. And then create some sql snippets to create a couple of UDFs.

The main challenge is that External Network Access can also point to public facing IP addresses. In my case I am using (ngrok)[https://ngrok.com/] to expose my local server to the public facing internet.

So I will walk step by step.

First you need a network rule. Network rules are used to identify the hosts and ports that you want to expose.

As I said I was using ngrok to expose my local docker, so this is a volatile URL but I can register it for my purpose.

```
CREATE OR REPLACE NETWORK RULE external_mq_network_rule
  TYPE = HOST_PORT
  VALUE_LIST = ('2.tcp.ngrok.io:14640') 
  MODE= EGRESS
;
```

After that I need to create an integration. The integration will be needed to associate the network rules (and secrets) so then can be associated with an UDF or proc.

``````
CREATE OR REPLACE EXTERNAL ACCESS INTEGRATION external_mq_int
  ALLOWED_NETWORK_RULES = (external_mq_network_rule)
  ENABLED = true;
````

Perfect. 
Now we need some code to write to the queue. I will create a sendMessage function:

```sql
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
```

To invoke this code I will call it like this:
```
SELECT SEND_MESSAGE('2.tcp.ngrok.io','14640','DEV.APP.SVRCONN','QM1','DEV.QUEUE.1','app','passw0rd','my message 2');
```

> NOTE: in a real example I might have sensitive settings like host, user, password in secrets.


And to read the message you need a similar mechanism:

```
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

```

And call it like this:

```
SELECT READ_MESSAGE('2.tcp.ngrok.io','14640','DEV.APP.SVRCONN','QM1','DEV.QUEUE.1','app','passw0rd');
```

In my tests everything worked and I was able to reach the queue and write and also read messages.








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



