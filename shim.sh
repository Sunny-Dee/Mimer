#!/bin/bash
# This is shim.bat
echo "We are now in a shim called from the Web Browser"
echo Arg one is: "$1"
# Change this to point to your MyWebServer directory:
cd /Users/delianaescobari/Documents/Mimer/

# pass the name of the first argument to java:
java -classpath ./:./xstream-1.2.1.jar:./xpp3_min-1.1.4c.jar -Dfirstarg=$1 BCHandler

read -p "Called Java BCHandler: press [Enter] key to continue..."