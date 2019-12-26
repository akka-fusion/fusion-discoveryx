#!/bin/bash

directory=discoveryx-server/src/main/resources/dist/

if [ ! -d $directory ]; then
  mkdir -p $directory
fi

rm -rf discoveryx-server/src/main/resources/dist/*
cp -r web-console/dist/* discoveryx-server/src/main/resources/dist/

