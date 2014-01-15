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
. ${WL_HOME}/server/bin/setWLSEnv.sh > /dev/null

${JAVA_HOME}/bin/java weblogic.WLST ${1}