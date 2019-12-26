#!/bin/sh

directory=docs/

if [ ! -d $directory ]; then
  mkdir -p $directory
fi

rm -rf docs/*
mv discoveryx-docs/target/paradox/site/main/* docs/

