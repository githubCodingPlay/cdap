#!/usr/bin/env bash

# Copyright © 2014 Cask Data, Inc.
# 
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
# 
# http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
  
# Build script for docs
# Builds the docs (all except javadocs and PDFs) from the .rst source files using Sphinx
# Builds the javadocs and copies them into place
# Zips everything up so it can be staged
# REST PDF is built as a separate target and checked in, as it is only used in SDK and not website
# Target for building the SDK
# Targets for both a limited and complete set of javadocs
# Targets not included in usage are intended for internal usage by script

source ../vars
source ../_common/common-build.sh

CLI_DOC_TOOL="../tools/docs-cli-commands.py"
CLI_INPUT_TXT="${PROJECT_PATH}/cdap-docs-gen/target/cdap-docs-cli.txt"
CLI_TABLE_RST="cdap-cli-table.rst"

CHECK_INCLUDES=${TRUE}

function download_includes() {
  local target_includes_dir=${1}
  echo "Copying CLI Docs: building rst file from cli-docs results..." 
  python "${CLI_DOC_TOOL}" "${CLI_INPUT_TXT}" "${target_includes_dir}/${CLI_TABLE_RST}"
  status_code=$?
  if [ "${status_code}" == "0" ]; then
    echo "CLI rst file written to ${CLI_TABLE_RST}"
  else
    local m="Error ${status_code} building CLI docs table"
    echo_red_bold "${m}"
    set_message "${m}"
  fi
  return $status_code
}

function build_extras() {
  echo_red_bold "Building extras."
  local html_path="${TARGET_PATH}/${HTML}"

  if [ "x${USING_JAVADOCS}" != "x" ]; then
    echo "Copying Javadocs."
    rm -rf ${html_path}/${JAVADOCS}
    cp -r ${API_JAVADOCS} ${html_path}/.
    mv -f ${html_path}/${APIDOCS} ${html_path}/${JAVADOCS}
  else
    echo "Not using Javadocs."
  fi

  echo "Copying license PDFs."
  cp ${SCRIPT_PATH}/${LICENSES_PDF}/*.pdf ${html_path}/${LICENSES}
}

run_command ${1}
