#!/bin/bash

curl -s "$1/schemas" > schemas.json

rm -rf ../src/main/java/io/rancher/service
rm -rf ../src/main/java/io/rancher/type

go run generator.go

rm schemas.json
