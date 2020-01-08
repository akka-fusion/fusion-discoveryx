#!/usr/bin/env bash

cd "$BASEDIR"

start test "RUNNING Generate paradox"

runSbt "discoveryx-docs/paradox"

end test "ALL TESTS PASSED"
