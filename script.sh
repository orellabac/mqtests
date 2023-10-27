docker pull icr.io/ibm-messaging/mq:latest
docker images
docker volume create qm1data

docker run --env LICENSE=accept --env MQ_QMGR_NAME=QM1 --volume qm1data:/mnt/mqm --publish 1414:1414 --publish 9443:9443 --detach --env MQ_APP_PASSWORD=passw0rd --name QM1 icr.io/ibm-messaging/mq:latest

docker ps

docker exec -ti QM1 bash

dspmqver

dspmq