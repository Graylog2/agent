#!/bin/bash

# resolve links - $0 may be a softlink
COLLECTOR_BIN="$0"

while [ -h "$COLLECTOR_BIN" ]; do
    ls=$(ls -ld "$COLLECTOR_BIN")
    link=$(expr "$ls" : '.*-> \(.*\)$')
    if expr "$link" : '/.*' > /dev/null; then
        COLLECTOR_BIN="$link"
    else
        COLLECTOR_BIN=$(dirname "$COLLECTOR_BIN")/"$link"
    fi
done

COLLECTOR_ROOT="$(dirname $(dirname $COLLECTOR_BIN))"
COLLECTOR_DEFAULT_JAR="$COLLECTOR_ROOT/graylog-collector.jar"

COLLECTOR_JAVA_DEFAULT_OPTS="${collector.jvm-opts} -Djava.library.path=$COLLECTOR_ROOT/lib/sigar"

if [ -f "$COLLECTOR_ROOT/bin/graylog-collector-script-config.sh" ]; then
    . "$COLLECTOR_ROOT/bin/graylog-collector-script-config.sh"
fi

COLLECTOR_JAR=${COLLECTOR_JAR:="$COLLECTOR_DEFAULT_JAR"}

COLLECTOR_JAVA_CMD=${COLLECTOR_JAVA_CMD:=$(which java)}
COLLECTOR_JAVA_OPTS="${COLLECTOR_JAVA_OPTS:="$COLLECTOR_JAVA_DEFAULT_OPTS"}"

die() {
    echo $*
    exit 1
}

if [ -x `which java` ]; then
    COLLECTOR_JAVA_CMD=`which java`
else
    die "java cannot be found."
fi

exec $COLLECTOR_JAVA_CMD $COLLECTOR_JAVA_OPTS -jar $COLLECTOR_JAR "$@"
