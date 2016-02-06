#!/bin/sh

# is there a realiable cross-platform was to do this without relying on compiling C code?

DIR=$(dirname $(readlink "$0") 2>/dev/null || dirname "$0" 2>/dev/null )
which realpath 2>&1 > /dev/null
REALPATH_INSTALLED=$?

if [ ! $REALPATH_INSTALLED -eq 0 ]; then
	if [ ! -f $DIR/realpath ]; then
	    >&2 echo "Compiling realpath" 
	    gcc $DIR/realpath.c -o $DIR/realpath
	    chmod u+x $DIR/realpath
	fi
	$DIR/realpath $1
else
	realpath $1
fi
