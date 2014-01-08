#!/bin/bash
SCRIPTDIR=$( cd $(dirname $0) ; pwd -P )
ant -f $SCRIPTDIR/build.xml -Divy.offline=true
