    /*******************************************************************************
 * Copyright (c) 2012 Xuggle Inc.  All rights reserved.
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

#include <com/xuggle/xuggler/io/URLProtocolHandler.h>
#include <com/xuggle/xuggler/io/URLProtocolManager.h>

namespace com { namespace xuggle{ namespace xuggler { namespace io
{

URLProtocolHandler :: URLProtocolHandler(
    URLProtocolManager* mgr)
{
  mManager = mgr;
}

URLProtocolHandler :: ~URLProtocolHandler()
{

}

const char*
URLProtocolHandler :: getProtocolName()
{
  const char* retval = 0;
  if (mManager)
    retval = mManager->getProtocolName();
  return retval;
}

}}}}
