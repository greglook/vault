#!/usr/bin/zsh

# Initialize a tmux workspace session for this project.
#
# Author: Greg Look

PROJECT_ROOT=$(dirname $(dirname $(readlink -f $0)))
SESSION_NAME=$(basename $PROJECT_ROOT)

# Don't run inside tmux!
if [[ -n "$TMUX" ]]; then
    echo "Workspace must be set up from outside tmux!"
    exit 1
fi

# Check if session already exists.
if tmux has-session -t $SESSION_NAME 2> /dev/null; then
    echo "tmux server already contains session: $SESSION_NAME"
    exit 1
fi

new_window() {
    WINDOW_INDEX=$1; shift
    WINDOW_NAME=$1; shift
    tmux new-window -t $SESSION_NAME:$WINDOW_INDEX -n $WINDOW_NAME -c $PROJECT_ROOT "$@"
}

run_in_window() {
    WINDOW_INDEX=$1; shift
    tmux send-keys -t $SESSION_NAME:$WINDOW_INDEX "$@" C-m
}

# Initialize workspace
tmux -2 new-session -d -s $SESSION_NAME
new_window 0 "misc" -k -c $HOME
new_window 1 "vault"
new_window 2 "repl" && run_in_window 2 "lein trampoline repl"
new_window 3 "src"  -c "$PROJECT_ROOT/src/vault"
new_window 4 "test" -c "$PROJECT_ROOT/test/vault"
new_window 5 "doc"  -c "$PROJECT_ROOT/doc"

# Attach to session
tmux select-window -t $SESSION_NAME:1
tmux attach -t $SESSION_NAME
