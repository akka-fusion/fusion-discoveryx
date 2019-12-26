#!/bin/sh

directory=../docs/

if [ ! -d $directory ]; then
  mkdir -p $directory
fi

rm -rf ${directory}/*
cp -r ../discoveryx-docs/target/paradox/site/main/* ${directory}/
