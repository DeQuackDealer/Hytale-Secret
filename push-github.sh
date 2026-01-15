#!/bin/bash

REPO_URL="https://${GITHUB_PAT}@github.com/DeQuackDealer/Hytale-Secret.git"

if [ -z "$GITHUB_PAT" ]; then
    echo "Error: GITHUB_PAT secret not found"
    exit 1
fi

git config user.email "rubidium@hytale.dev"
git config user.name "Rubidium Build"

if ! git remote | grep -q "github"; then
    git remote add github "$REPO_URL"
else
    git remote set-url github "$REPO_URL"
fi

git add -A
git commit -m "Rubidium Framework - $(date '+%Y-%m-%d %H:%M:%S')" || echo "No changes to commit"

git push github HEAD:main --force

echo "Successfully pushed to GitHub!"
