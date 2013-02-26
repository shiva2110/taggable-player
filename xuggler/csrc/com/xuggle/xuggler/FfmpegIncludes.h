/*******************************************************************************
 * Copyright (c) 2008, 2010 Xuggle Inc.  All rights reserved.
 *  
 * This file is part of Xuggle-Xuggler-Main.
 *
 * Xuggle-Xuggler-Main is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Xuggle-Xuggler-Main is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Xuggle-Xuggler-Main.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

#ifndef FFMPEGINCLUDES_H_
#define FFMPEGINCLUDES_H_

extern "C"
{
// Hack here to get rid of deprecation compilation warnings
//#define attribute_deprecated

// WARNING: This is GCC specific and is to fix a build issue
// in FFmpeg where UINT64_C is not always defined.  The
// __WORDSIZE value is a GCC constant
#define __STDC_CONSTANT_MACROS 1
#include <stdint.h>
#ifndef UINT64_C
# if __WORDSIZE == 64
#  define UINT64_C(c)	c ## UL
# else
#  define UINT64_C(c)	c ## ULL
# endif
#endif


#include <libavutil/avutil.h>
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libavdevice/avdevice.h>
#include <libavfilter/avfilter.h>
#include <libavutil/opt.h>
#include <libavutil/parseutils.h>
// FFmpeg changes these form enums to #defines.  change them back.
#undef CODEC_TYPE_UNKNOWN
#undef CODEC_TYPE_VIDEO
#undef CODEC_TYPE_AUDIO
#undef CODEC_TYPE_SUBTITLE
#undef CODEC_TYPE_ATTACHMENT
#undef CODEC_TYPE_NB

}
#endif /*FFMPEGINCLUDES_H_*/
