#!/bin/bash

# test if any environment variable
# exists with name starting with $1
# and terminates if none are round
assert_environment_variables() {
	if [ $( \
		( set -o posix ; set ) \
			| grep $1 \
			| wc -l) -eq 0 ]; then
			
		echo $1 environment variable missing >&2
		exit 1
	fi
}

# produce all values of environment
# variables with a name starting with $1
all_environment_values() {
	( set -o posix ; set ) \
		| grep $1 \
		| cut -d = -f 2
}

# create a apache address filter expression
# based on environment variables 
# with a name starting with $1
create_expression() {
	EXPR=
	for addr in $(all_environment_values $1); do
		if [ -n "$EXPR" ]; then
			EXPR="$EXPR && "
		fi
		EXPR="$EXPR-R '$addr'"
	done
	echo $EXPR
}

assert_environment_variables INTERNAL_ADDR
assert_environment_variables TRUSTED_ADDR

INTERNAL_EXPR=$(create_expression INTERNAL_ADDR)
TRUSTED_EXPR=$(create_expression TRUSTED_ADDR)

export INTERNAL_EXPR
export TRUSTED_EXPR

exec /usr/sbin/apache2ctl -D FOREGROUND
