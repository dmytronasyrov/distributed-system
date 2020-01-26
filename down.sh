#!/bin/bash

rm -rf docker.log

docker-compose -f docker-compose.yaml down --remove-orphans