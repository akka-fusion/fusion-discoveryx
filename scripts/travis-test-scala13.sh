#!/usr/bin/env bash

cd "$BASEDIR"

start "RUNNING TESTS FOR SCALA 2.13"

runSbt test:compile

end "ALL TESTS PASSED"
