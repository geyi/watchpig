#!/bin/bash
# This script is run by failover_command.

set -o xtrace

FAILED_NODE_ID="$1"
FAILED_NODE_HOST="$2"
NEW_MAIN_NODE_ID="$3"
NEW_MAIN_NODE_HOST="$4"
OLD_PRIMARY_NODE_ID="$5"

PGHOME=/usr/pgsql-14
REPL_SLOT_NAME=${FAILED_NODE_HOST//[-.]/_}


echo failover.sh: start: failed_node_id=$FAILED_NODE_ID failed_host=$FAILED_NODE_HOST \
    old_primary_node_id=$OLD_PRIMARY_NODE_ID new_main_node_id=$NEW_MAIN_NODE_ID new_main_host=$NEW_MAIN_NODE_HOST

## Test passwordless SSH
ssh -T postgres@${NEW_MAIN_NODE_HOST} ls /tmp > /dev/null

if [ $? -ne 0 ]; then
    echo failover.sh: passwordless SSH to postgres@${NEW_MAIN_NODE_HOST} failed. Please setup passwordless SSH.
    exit 1
fi

## Promote Standby node.
echo failover.sh: primary node is down, promote new_main_node_id=$NEW_MAIN_NODE_ID on ${NEW_MAIN_NODE_HOST}.

ssh -T postgres@${NEW_MAIN_NODE_HOST} ${PGHOME}/bin/pg_ctl -D /var/lib/pgsql/14/data -w promote

if [ $? -ne 0 ]; then
    echo ERROR: failover.sh: end: failover failed
    exit 1
fi

echo failover.sh: end: new_main_node_id=$NEW_MAIN_NODE_ID on ${NEW_MAIN_NODE_HOST} is promoted to a primary
exit 0
