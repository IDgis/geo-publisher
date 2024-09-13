#!/bin/bash

set -eu

if [ -f /RUNNING_PID ]; then
	echo RUNNING_PID exists, removing...
	rm /RUNNING_PID
else
	echo RUNNING_PID does not exist
fi

/opt/bin/playBinary
