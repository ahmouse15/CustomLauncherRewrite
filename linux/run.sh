#!/bin/bash
#
# This file is part of CustomLauncherRewrite.
#
# CustomLauncherRewrite is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# CustomLauncherRewrite is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with CustomLauncherRewrite.  If not, see <https://www.gnu.org/licenses/>.
#

# Check if Java is installed and the version is at least 17
if (( $(java -version 2>&1 | grep -Po '(?<=")[0-9]{2}') >= 17 )); then
    latest=$(find . -name "CustomLauncherRewrite-*.jar" -type f -printf "%T@ %p\n" | sort -nr | head -n1 | cut -d' ' -f2-)
    java -jar "$latest"
else
    if command -v notify-send &> /dev/null; then
        notify-send -i stop "CustomLauncherRewrite" "Please update Java JRE to version 17 or higher!"
    elif command -v zenity &> /dev/null; then
        zenity --warning --text="Please update Java JRE to version 17 or higher!" --title="CustomLauncherRewrite"
    else
        echo "Please update Java JRE to version 17 or higher!"
    fi
fi
