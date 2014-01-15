#
#
#   Copyright 2009 Middleware Connections, a division of Cloudware Connections Inc.
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
# 
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#

#!/bin/sh

echo "Setting Configuration & ClassPaths ...."
LIBDIR=../lib
CONFDIR=../conf
LOGDIR=../logs
CONFIG_XML=../conf/config.xml



CP=../build/dist/mat.jar:$LIBDIR/log4j-1.2.15.jar:$LIBDIR/commons-logging-1.1.1.jar:$LIBDIR/commons-io-1.4.jar:$LIBDIR/groovy-all-1.5.6.jar:$LIBDIR/middleware.jar:$DEPENDENCYFINDER_HOME/lib/DependencyFinder.jar:$LIBDIR/truezip-6.jar:$CONFDIR


VMARGS="-Xms512m -Xmx1024m"
java $VMARGS -cp $CP -Dredhatlogdir=$LOGDIR -Dconfig.xml=$CONFIG_XML com.mwc.mat.WLShellDownloadScript
chmod +x ../tools/wlshell/bin/*.sh
