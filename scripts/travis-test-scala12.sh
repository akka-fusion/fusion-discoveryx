#!/usr/bin/env bash

cd "$BASEDIR"

start test "RUNNING TESTS FOR SCALA 2.12"

runSbt "++2.12.10 test:compile"

end test "ALL TESTS PASSED"
