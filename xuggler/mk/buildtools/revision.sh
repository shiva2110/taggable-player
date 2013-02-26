#!/bin/sh

# Copyright (c) 2009 by Xuggle Inc.
#
# Permission is hereby granted, free of charge, to any person obtaining
# a copy of this software and associated documentation files (the
# "Software"), to deal in the Software without restriction, including
# without limitation the rights to use, copy, modify, merge, publish,
# distribute, sublicense, and/or sell copies of the Software, and to
# permit persons to whom the Software is furnished to do so, subject to
# the following conditions:
#
# The above copyright notice and this permission notice shall be
# included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
# LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
# OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
# WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
# It is REQUESTED BUT NOT REQUIRED if you use this library, that you let 
# us know by sending e-mail to info\@xuggle.com telling us briefly how you're
# using the library and what you like or don't like about it.

# Usage; $0 "directory_to_check" "file_to_read_last_revision" ["optional file to write last to" ["optional ant properties file to write"]

# look with GIT
# prints the commiter time as a UNIX timestamp
# revision=`git log -1 --pretty=format:"%ct" --date=short 2>/dev/null`
# Unfortunately with GIT there is no longer small incremental version
# numbers, and LD on different platforms DO NOT do well with large
# revision numbers.  And so we're just going to hardcode revision to zero
#
revision=0

if test -f "$2" ; then
  test $revision || revision=`cat "$2"`
fi
# no version number found
test $revision || revision=0

if test ! "x$3" = "x"; then
 echo $revision > "$3"
fi
# And spit out to the command line so the tool that called this
# can do it's magic
if test -z "$4"; then
  # just spit out the revision to the command line
  echo $revision
else
  echo "library.revision: $revision" > "$4"
fi
