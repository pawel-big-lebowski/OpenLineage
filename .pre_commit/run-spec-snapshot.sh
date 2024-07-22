#!/bin/bash

# Copy changed spec JSON files to target location
git diff --name-only HEAD -- 'spec/*.json' 'spec/OpenLineage.yml' | while read LINE; do
  # ignore registry files
  if [[ $LINE =~ "registry.json" ]]; then
    continue
  fi

  # extract target file name from $id field in spec files
  URL=$(cat $LINE | jq -r '.["$id"]')

  # extract target location in website repo
  LOC="website/static/${URL#*//*/}"
  LOC_DIR="${LOC%/*}"

  # create dir if necessary, and copy files
  echo "change detected in $LINE"
  mkdir -p $LOC_DIR
  cp $LINE $LOC
done