#!/bin/bash

sbt "project discoveryx-docs" paradox

pushd web-console/
yarn
yarn build
popd

pushd scripts/
sh publish-docs.sh
sh publish-dist.sh
popd

sbt "project discoveryx-server" dist
