#!/usr/bin/env bash

curl --request GET -v localhost:8081/something

curl --request GET -v localhost:8081/status

curl -v --data '50,100' localhost:8081/task

curl -v --header "X-Test: true" --data '50,100' localhost:8081/task

curl -v --header "X-Debug: true" --data '50,100' localhost:8081/task