#!/bin/sh

# Use this file to clean up an install before FFMPEG's actual
# uninstall runs
prefix="/usr/local"
exec_prefix="${prefix}"
HOST_TYPE=Mac
HOST_OS=x86_64-apple-darwin11.4.2

FFMPEG_LIB_DIR="${DESTDIR}${exec_prefix}/lib"
FFMPEG_LIBRARIES="avcodec avformat avutil avdevice swscale" 

echo "Running . pre-uninstall from ${DESTDIR}${prefix}"
case $HOST_TYPE in
  Windows)
  # Our install script replaced FFMPEG's .dll.a files from the
  # MSVC generated .lib files; we need to clean that up
  rm -f "${FFMPEG_LIB_DIR}/libav*.dll.a"
  rm -f "${FFMPEG_LIB_DIR}/libswscale*.dll.a"
  ;;
  *)
  ;;
esac

