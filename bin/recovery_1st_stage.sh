#!/bin/bash
# This script is executed by "recovery_1st_stage" to recovery a Standby node.

set -o xtrace

DEST_NODE_ID="$1"
DEST_NODE_HOST="$2"
DEST_NODE_PORT="$3"
DEST_NODE_PGDATA="$4"
PRIMARY_NODE_HOST="$5"
PRIMARY_NODE_PORT="$6"

PGHOME=/usr/pgsql-14
ARCHIVEDIR=/var/lib/pgsql/archivedir
REPLUSER=repuser
REPLPASS=repuser
REPL_SLOT_NAME=${DEST_NODE_HOST//[-.]/_}

echo recovery_1st_stage: start: pg_basebackup for Standby node $DEST_NODE_ID

## Test passwordless SSH
ssh -T postgres@${DEST_NODE_HOST} ls /tmp > /dev/null

if [ $? -ne 0 ]; then
    echo recovery_1st_stage: passwordless SSH to postgres@${DEST_NODE_HOST} failed. Please setup passwordless SSH.
    exit 1
fi

## Get PostgreSQL major version
PGVERSION=`${PGHOME}/bin/initdb -V | awk '{print $3}' | sed 's/\..*//' | sed 's/\([0-9]*\)[a-zA-Z].*/\1/'`
if [ $PGVERSION -ge 12 ]; then
    RECOVERYCONF=${DEST_NODE_PGDATA}/myrecovery.conf
else
    RECOVERYCONF=${DEST_NODE_PGDATA}/recovery.conf
fi

## Create replication slot "${REPL_SLOT_NAME}"
${PGHOME}/bin/psql -h ${PRIMARY_NODE_HOST} -p ${PRIMARY_NODE_PORT} \
    -c "SELECT pg_create_physical_replication_slot('${REPL_SLOT_NAME}');"  >/dev/null 2>&1

if [ $? -ne 0 ]; then
    echo ERROR: recovery_1st_stage: create replication slot \"${REPL_SLOT_NAME}\" failed. You may need to create replication slot manually.
fi

## Execute pg_basebackup to recovery Standby node
ssh -T postgres@$DEST_NODE_HOST "

    set -o errexit

    rm -rf $DEST_NODE_PGDATA
    rm -rf $ARCHIVEDIR/*

    ${PGHOME}/bin/pg_basebackup -h $PRIMARY_NODE_HOST -U $REPLUSER -p $PRIMARY_NODE_PORT -D $DEST_NODE_PGDATA -X stream

    cat > ${RECOVERYCONF} << EOT
primary_conninfo = 'host=${PRIMARY_NODE_HOST} port=${PRIMARY_NODE_PORT} user=${REPLUSER} application_name=${DEST_NODE_HOST} password=${REPLPASS}'
recovery_target_timeline = 'latest'
restore_command = 'scp ${PRIMARY_NODE_HOST}:${ARCHIVEDIR}/%f %p'
primary_slot_name = '${REPL_SLOT_NAME}'
EOT

    if [ ${PGVERSION} -ge 12 ]; then
        sed -i -e \"\\\$ainclude_if_exists = '$(echo ${RECOVERYCONF} | sed -e 's/\//\\\//g')'\" \
               -e \"/^include_if_exists = '$(echo ${RECOVERYCONF} | sed -e 's/\//\\\//g')'/d\" ${DEST_NODE_PGDATA}/postgresql.conf
        touch ${DEST_NODE_PGDATA}/standby.signal
    else
        echo \"standby_mode = 'on'\" >> ${RECOVERYCONF}
    fi

    sed -i \"s/#*port = .*/port = ${DEST_NODE_PORT}/\" ${DEST_NODE_PGDATA}/postgresql.conf
"

if [ $? -ne 0 ]; then

    ${PGHOME}/bin/psql -h ${PRIMARY_NODE_HOST} -p ${PRIMARY_NODE_PORT} \
        -c "SELECT pg_drop_replication_slot('${REPL_SLOT_NAME}');"  >/dev/null 2>&1

    if [ $? -ne 0 ]; then
        echo ERROR: recovery_1st_stage: drop replication slot \"${REPL_SLOT_NAME}\" failed. You may need to drop replication slot manually.
    fi

    echo ERROR: recovery_1st_stage: end: pg_basebackup failed. online recovery failed
    exit 1
fi

echo recovery_1st_stage: end: recovery_1st_stage is completed successfully
exit 0
