# this script creates a docker container to test mq usage
# https://developer.ibm.com/tutorials/mq-connect-app-queue-manager-containers/

# pulls the image
docker pull icr.io/ibm-messaging/mq:latest
# displays current images
docker images
# creates a volume
docker volume create qm1data
#starts the docker container
docker run --env LICENSE=accept --env MQ_QMGR_NAME=QM1 --volume qm1data:/mnt/mqm --publish 1414:1414 --publish 9443:9443 --detach --env MQ_APP_PASSWORD=passw0rd --name QM1 icr.io/ibm-messaging/mq:latest
# display the current docker info
docker ps

#to connect to the container
# docker exec -ti QM1 bash

#this command can be used to display info: dspmqver
#this command shows the queue: dspmq

# this command can be used to get the public URL
gp url 1414 

# connecting to queue
runmqsc QM1