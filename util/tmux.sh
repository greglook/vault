#!/usr/bin/zsh

# Initialize a tmux workspace session for this project.
#
# Author: Greg Look

PROJECT_ROOT=$(dirname $(dirname $(readlink -f $0)))
SESSION=$(basename $PROJECT_ROOT)

# Don't run inside tmux!
if [[ -n "$TMUX" ]]; then
    echo "Workspace must be set up from outside tmux!"
    exit 1
fi

# Check if session already exists.
if tmux has-session -t $SESSION 2> /dev/null; then
    echo "tmux server already contains session: $SESSION"
    exit 1
fi

# Initialize workspace
tmux -2 new-session -d -s $SESSION

tmux new-window -t "$SESSION:0" -n 'misc' -c "$HOME" -k

tmux new-window -t "$SESSION:1" -n 'vault' -c "$PROJECT_ROOT"
tmux send-keys -t "$SESSION:1" "alias vault='$PROJECT_ROOT/bin/vault'" C-m

tmux new-window -t "$SESSION:2" -n 'src' -c "$PROJECT_ROOT/src/vault"
tmux split-window -t "$SESSION:2" -h -l 60 -c "$PROJECT_ROOT"
tmux send-keys -t "$SESSION:2.1" "lein trampoline repl" C-m

tmux new-window -t "$SESSION:3" -n 'test' -c "$PROJECT_ROOT/test/vault"
tmux split-window -t "$SESSION:3" -h -c "$PROJECT_ROOT"
tmux send-keys -t "$SESSION:3.1" "lein test-refresh" # C-m

tmux new-window -t "$SESSION:4" -n 'doc'  -c "$PROJECT_ROOT/doc"

# Attach to session
tmux select-window -t "$SESSION:1"
tmux attach -t "$SESSION"
