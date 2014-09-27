#!/bin/bash

# Regenerates the project's documentation.

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOC_TARGET="target/doc"

cd "$PROJECT_DIR"

echo "Cleaning doc target"
rm -rf $DOC_TARGET
mkdir -p $DOC_TARGET

echo "Building codox, hiera, and clique"
lein do doc, hiera, clique

echo "Generating clique graph"
dot -Tsvg < deps.dot > $DOC_TARGET/fn-clique.svg
rm deps.dot
