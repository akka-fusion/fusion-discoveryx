#!/bin/bash

directory=../discoveryx-server/src/main/resources/dist

if [ ! -d $directory ]; then
  mkdir -p $directory
fi

rm -rf ${directory}/*
cp -r ../web-console/dist/* ${directory}/
