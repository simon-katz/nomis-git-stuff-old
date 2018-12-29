#!/bin/bash

function git__return_branch-name {
    # TODO Make the grep here a bit more selective.
    echo $(git branch | grep \* | cut -d ' ' -f2)
}

function git__stash_if_dirty_include_untracked {
    local message=$1
    git stash push \
        --quiet \
        --include-untracked \
        --message "${message}"
}

function git__replace_previous_n_commits_incl_staged {
    local n=$1
    local commit_sha=$2
    echo "Committing: Replacing last ${n} commits with a single commit."
    git reset --quiet --soft HEAD~${n}
    git commit --quiet --no-verify -C ${commit_sha}
}

function git__return_top_stash_name {
    local n=$1
    echo $(git stash list | head -${n})
}

function git__return_top_commit_message {
    local n=$1
    echo $(git log --format=%s -n ${n} | tail -1)
}
