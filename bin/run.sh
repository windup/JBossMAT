#!/bin/sh

echo "Setting Configuration & ClassPaths ...."
LIBDIR=../lib
CONFDIR=../conf
LOGDIR=../logs
CONFIG_XML=../conf/config.xml
DEPENDENCYFINDER_HOME=$MAT_HOME/tools/depfinder



CP=../build/dist/mat.jar:$LIBDIR/log4j-1.2.15.jar:$LIBDIR/commons-logging-1.1.1.jar:$LIBDIR/commons-io-1.4.jar:$LIBDIR/groovy-all-1.5.6.jar:$LIBDIR/middleware.jar:$DEPENDENCYFINDER_HOME/lib/DependencyFinder.jar:$LIBDIR/truezip-6.jar:$CONFDIR


cd ../build
ant
cd ../bin
echo "_________________________________________________________________________"

VMARGS="-Xms512m -Xmx1024m"
export DEPENDENCYFINDER_OPTS=$VMARGS



java $VMARGS -cp $CP -Dredhatlogdir=$LOGDIR -Dconfig.xml=$CONFIG_XML com.mwc.mat.ToolMain
