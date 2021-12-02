#!/bin/bash
# This script is run by stop primary pgsql.

set -o xtrace

PRIMARY_NODE_HOST="$1"

PGHOME=/usr/pgsql-14
REPL_SLOT_NAME=${FAILED_NODE_HOST//[-.]/_}


echo stop.sh: start: primary_node_host=$PRIMARY_NODE_HOST

## Test passwordless SSH
ssh -T postgres@${PRIMARY_NODE_HOST} ls /tmp > /dev/null

if [ $? -ne 0 ]; then
    echo stop.sh: passwordless SSH to postgres@${PRIMARY_NODE_HOST} failed. Please setup passwordless SSH.
    exit 1
fi

## Stop primary node.
ssh -T postgres@${PRIMARY_NODE_HOST} ${PGHOME}/bin/pg_ctl -D /var/lib/pgsql/14/data -m immediate stop

if [ $? -ne 0 ]; then
    echo ERROR: stop.sh: end: stop failed
    exit 1
fi

echo stop.sh: end: ${PRIMARY_NODE_HOST} is stoped
exit 0
