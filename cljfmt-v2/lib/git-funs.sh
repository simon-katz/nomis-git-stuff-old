#!/bin/bash

function git_stash_if_dirty_include_untracked {
    local message=$1
    git stash push \
        --quiet \
        --include-untracked \
        --message "${message}"
}

function replace_previous_n_commits {
    local n=$1
    local commit_sha=$2
    echo "Committing: Replacing last ${n} commits with a single commit."
    git reset --quiet --soft HEAD~${n}
    git add . # TODO Is this needed?
    git commit --quiet --no-verify -C ${commit_sha}
}
