#!/bin/bash

help ()
{
echo "To call this script properly, please append it with arguments of:"
echo "build, clean, or run"
}


if [[ $1 == 'build' ]]; then
	if [[ "${OSTYPE//[0-9.]/}" == darwin ]]; then 
		sh .build.sh
		exit 0
	else
		./.build.sh
		exit 0
	fi
fi

if [[ $1 == 'clean' ]]; then
	if [[ "${OSTYPE//[0-9.]/}" == darwin ]]; then 
		sh .clean.sh
		exit 0
	else
		./.clean.sh
		exit 0
	fi
fi

if [[ $1 == 'run' ]]; then
	if [[  "${OSTYPE//[0-9.]/}" == darwin ]]; then 
		sh .run.sh
		exit 0
	else
		./.run.sh
		exit 0
	fi
fi


if [[ $1 == "help" ]]; then

	help
	exit 0
fi

help



