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
/*
 * MetaData.cpp
 *
 *  Created on: Jun 29, 2009
 *      Author: aclarke
 */

#include "MetaData.h"

namespace com
{

namespace xuggle
{

namespace xuggler
{

MetaData :: MetaData()
{
  mLocalMeta = 0;
  mMetaData = &mLocalMeta;
}

MetaData :: ~MetaData()
{
  if (mMetaData && *mMetaData)
    av_dict_free(mMetaData);
}

int32_t
MetaData :: getNumKeys()
{
  if (!mMetaData || !*mMetaData)
    return 0;
  
  AVDictionaryEntry* tag=0;
  int32_t retval=0;
  do
  {
    tag = av_dict_get(*mMetaData, "", tag, AV_DICT_IGNORE_SUFFIX);
    if (tag)
      retval++;
  } while(tag);
  return retval;
}

const char*
MetaData :: getKey(int32_t index)
{
  if (!mMetaData || !*mMetaData || index < 0)
    return 0;

  AVDictionaryEntry* tag=0;
  int32_t position=-1;
  do
  {
    tag = av_dict_get(*mMetaData, "", tag, AV_DICT_IGNORE_SUFFIX);
    if (tag) {
      position++;
      if (position == index)
        return tag->key;
    }
  } while(tag);
  return 0;
}
const char*
MetaData :: getValue(const char *key, Flags flag)
{
   if (!mMetaData || !*mMetaData || !key || !*key)
     return 0;
   AVDictionaryEntry* tag = av_dict_get(*mMetaData, key, 0, (int)flag);
   if (tag)
     return tag->value;
   else
     return 0;
}

int32_t
MetaData :: setValue(const char* key, const char* value)
{
  return setValue(key, value, METADATA_NONE);
}

int32_t
MetaData :: setValue(const char* key, const char* value, Flags flag)
{
  if (!key || !*key || !mMetaData)
    return -1;
  return (int32_t)av_dict_set(mMetaData, key, value, (int)flag);
}

MetaData*
MetaData :: make(AVDictionary** metaToUse)
{
  if (!metaToUse)
    return 0;
  
  MetaData* retval = make();
  
  if (retval)
    retval->mMetaData = metaToUse;

  return retval;
}

MetaData*
MetaData :: make(AVDictionary* metaDataToCopy)
{
  MetaData* retval = make();
  if (retval && metaDataToCopy)
  {
    AVDictionaryEntry* tag = 0;
    do {
      tag = av_dict_get(metaDataToCopy, "", tag,
          AV_DICT_IGNORE_SUFFIX);
      if (tag)
        if (av_dict_set(retval->mMetaData, tag->key, tag->value,0) < 0)
        {
          VS_REF_RELEASE(retval);
          break;
        }
    } while(tag);
  }
  return retval;
}

int32_t
MetaData :: copy(AVDictionary *data)
{
  if (!data)
    return -1;

  if (mMetaData) {
    if (data == *mMetaData)
      // copy the current data; just return
      return 0;
    av_dict_free(mMetaData);
    *mMetaData = 0;
  }
  av_dict_copy(mMetaData, data, 0);
  return 0;
}

int32_t
MetaData :: copy(IMetaData* dataToCopy)
{
  MetaData* data = dynamic_cast<MetaData*>(dataToCopy);
  if (!data)
    return -1;
  
  return copy(data->getDictionary());
}
}}}
