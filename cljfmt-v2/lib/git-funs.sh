#!/bin/bash

function git_stash_include_untracked {
    local message=$1
    git stash push \
        --quiet \
        --include-untracked \
        --message "${message}"
}
