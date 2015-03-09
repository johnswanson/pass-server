#!/bin/bash

mkdir -p log

source PROD

/usr/bin/jsvc                                           \
  -java-home "/usr/lib/jvm/java-7-openjdk-amd64"        \
  -cp "$(pwd)/target/pw-0.1.0-SNAPSHOT-standalone.jar"  \
  -outfile "$(pwd)/log/out.txt"                         \
  -errfile "$(pwd)/log/err.txt"                         \
  pw.core
