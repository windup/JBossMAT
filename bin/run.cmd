echo "Setting Configuration & ClassPaths ...."
set LIBDIR=%MAT_HOME%\lib
set CONFDIR=%MAT_HOME%\conf
set LOGDIR=%MAT_HOME%\logs
set CONFIG_XML=%MAT_HOME%\conf\config.xml
set DEPENDENCYFINDER_HOME=%MAT_HOME%\tools\depfinder

set CP=%MAT_HOME%\build\dist\mat.jar;%LIBDIR%\log4j-1.2.15.jar;%LIBDIR%\commons-logging-1.1.1.jar;%LIBDIR%\commons-io-1.4.jar;%LIBDIR%\groovy-all-1.5.6.jar;%LIBDIR%\middleware.jar;%DEPENDENCYFINDER_HOME%\lib\DependencyFinder.jar;%LIBDIR%\truezip-6.jar;%CONFDIR%
set CLASSPATH=%CP%
cd ..\build
call ant
cd ..\bin

echo "_________________________________________________________________________"

set DEPENDENCYFINDER_OPTS=-Xms512m -Xmx1024m

java -Xms512m -Xmx1024m -cp "%CP%" -Dredhatlogdir="%LOGDIR%" -Dconfig.xml="%CONFIG_XML%" com.mwc.mat.ToolMain
