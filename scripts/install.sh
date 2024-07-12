#!/bin/bash
#
# Licensed under the MIT license
# <LICENSE-MIT or https://opensource.org/licenses/MIT>, at your
# option. This file may not be copied, modified, or distributed
# except according to those terms.
#
# Inspired by https://github.com/apollographql/rover/blob/df243566702cb706381cdb5dcba6864d5b721eb7/installers/binstall/scripts/nix/install.sh

set -u

BINARY_DOWNLOAD_PREFIX="https://github.com/GradleUp/librarian/releases/download"
#https://github.com/GradleUp/librarian/releases/download/v0.0.4/librarian-0.0.4.zip

# Note: this line is built automatically
PACKAGE_VERSION="0.0.4"

download_binary_and_run_installer() {
    downloader --check
    need_cmd mktemp
    need_cmd chmod
    need_cmd mkdir
    need_cmd rm
    need_cmd rmdir
    need_cmd unzip

    DOWNLOAD_VERSION=$PACKAGE_VERSION

    local _tardir="librarian-$DOWNLOAD_VERSION"
    local _url="$BINARY_DOWNLOAD_PREFIX/v$DOWNLOAD_VERSION/${_tardir}.zip"
    local _tmpdir="$(mktemp -d 2>/dev/null || ensure mktemp -d -t librarian)"
    local _installdir="$HOME/.librarian"
    local _file="$_tmpdir/input.zip"

    say "downloading librarian from $_url" 1>&2

    ensure mkdir -p "$_tmpdir"
    downloader "$_url" "$_file"
    if [ $? != 0 ]; then
      say "failed to download $_url"
      say "this may be a standard network error, but it may also indicate"
      say "that the release process is not working. When in doubt"
      say "please feel free to open an issue!"
      say "https://github.com/gradleup/librarian/issues/new/choose"
      exit 1
    fi

    ensure unzip "$_file" -d "$_tmpdir"
    ensure rm -rf "$_installdir"
    ensure mv "$_tmpdir/librarian-$PACKAGE_VERSION" "$_installdir"

    ignore rm -rf "$_tmpdir"

    return "$?"
}

say() {
    local green=`tput setaf 2 2>/dev/null || echo ''`
    local reset=`tput sgr0 2>/dev/null || echo ''`
    echo "$1"
}

err() {
    local red=`tput setaf 1 2>/dev/null || echo ''`
    local reset=`tput sgr0 2>/dev/null || echo ''`
    say "${red}ERROR${reset}: $1" >&2
    exit 1
}

need_cmd() {
    if ! check_cmd "$1"
    then err "need '$1' (command not found)"
    fi
}

check_cmd() {
    command -v "$1" > /dev/null 2>&1
    return $?
}

need_ok() {
    if [ $? != 0 ]; then err "$1"; fi
}

assert_nz() {
    if [ -z "$1" ]; then err "assert_nz $2"; fi
}

# Run a command that should never fail. If the command fails execution
# will immediately terminate with an error showing the failing
# command.
ensure() {
    "$@"
    need_ok "command failed: $*"
}

# This is just for indicating that commands' results are being
# intentionally ignored. Usually, because it's being executed
# as part of error handling.
ignore() {
    "$@"
}

# This wraps curl or wget. Try curl first, if not installed,
# use wget instead.
downloader() {
    if check_cmd curl
    then _dld=curl
    elif check_cmd wget
    then _dld=wget
    else _dld='curl or wget' # to be used in error message of need_cmd
    fi

    if [ "$1" = --check ]
    then need_cmd "$_dld"
    elif [ "$_dld" = curl ]
    then curl -sSfL "$1" -o "$2"
    elif [ "$_dld" = wget ]
    then wget "$1" -O "$2"
    else err "Unknown downloader"   # should not reach here
    fi
}

download_binary_and_run_installer "$@" || exit 1