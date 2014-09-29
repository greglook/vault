#!/bin/bash

# Regenerates the project's documentation.

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOC_TARGET="target/doc"

cd "$PROJECT_DIR"

if [[ -d $DOC_TARGET ]]; then
    echo "Cleaning doc target"
    rm -rf $DOC_TARGET
fi

mkdir -p $DOC_TARGET

echo "Building coverage, codox, marginalia, and ns-hierarchy"
lein do test, cloverage, hiera, doc, marg --dir $DOC_TARGET --file marginalia.html

if [[ -f deps.dot ]]; then
    echo "Generating clique graph"
    dot -Tsvg < deps.dot > $DOC_TARGET/fn-clique.svg
    rm deps.dot
fi
