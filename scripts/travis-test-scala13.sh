#!/usr/bin/env bash

cd "$BASEDIR"

start test "RUNNING TESTS FOR SCALA 2.13"

runSbt test

end test "ALL TESTS PASSED"
