#!/bin/bash

rm -rf docker.log

docker-compose -f docker-compose.yaml down --remove-orphans
docker-compose -f docker-compose.yaml pull
docker-compose -f docker-compose.yaml up --force-recreate -d