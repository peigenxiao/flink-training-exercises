#!/bin/bash

# Copyright 2015 data Artisans GmbH
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# usage: just pipe raw data into script

tr -d '\r' | awk -F "," '{print NR ",START," $6 ",1970-01-01 00:00:00," $11 "," $12 "," $13 "," $14 "," $8 "," $1 "," $2}{print NR ",END," $7 "," $6 "," $11 "," $12 "," $13 "," $14 "," $8 "," $1 "," $2}' | sort -t ',' -k 3 -S "4G" < /dev/stdin
