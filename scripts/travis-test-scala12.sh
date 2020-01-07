#!/usr/bin/env bash

cd "$BASEDIR"

start "RUNNING TESTS FOR SCALA 2.12"

runSbt "++2.12.10 test:compile"

end "ALL TESTS PASSED"
