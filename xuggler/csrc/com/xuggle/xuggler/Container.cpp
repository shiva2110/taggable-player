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

#include <stdexcept>

// for strncpy
#include <cstring>

//#define attribute_deprecated

#include <com/xuggle/ferry/JNIHelper.h>
#include <com/xuggle/ferry/Logger.h>
#include <com/xuggle/xuggler/Container.h>
#include <com/xuggle/xuggler/ContainerFormat.h>
#include <com/xuggle/xuggler/Stream.h>
#include <com/xuggle/xuggler/Packet.h>
#include <com/xuggle/xuggler/Global.h>
#include <com/xuggle/xuggler/Property.h>
#include <com/xuggle/xuggler/MetaData.h>
#include <com/xuggle/ferry/IBuffer.h>
#include <com/xuggle/xuggler/io/URLProtocolManager.h>
VS_LOG_SETUP(VS_CPP_PACKAGE);

#define XUGGLER_CHECK_INTERRUPT(retval) do { \
    if ((retval) < 0) { \
       JNIHelper* helper = JNIHelper::getHelper(); \
      if (helper && helper->isInterrupted()) \
        (retval) = AVERROR(EINTR); \
        } \
} while(0)

using namespace com::xuggle::ferry;
using namespace com::xuggle::xuggler::io;

/** Some static functions used by custom IO
 */
int
Container_url_read(void*h, unsigned char* buf, int size)
{
  int retval = -1;
  try {
    URLProtocolHandler* handler = (URLProtocolHandler*)h;
    if (handler)
      retval = handler->url_read(buf,size);
  } catch (...)
  {
    retval = -1;
  }
  VS_LOG_TRACE("URLProtocolHandler[%p]->url_read(%p, %d) ==> %d", h, buf, size, retval);
  return retval;
}
int
Container_url_write(void*h, unsigned char* buf, int size)
{
  int retval = -1;
  try {
    URLProtocolHandler* handler = (URLProtocolHandler*)h;
    if (handler)
      retval = handler->url_write(buf,size);
  } catch (...)
  {
    retval = -1;
  }
  VS_LOG_TRACE("URLProtocolHandler[%p]->url_write(%p, %d) ==> %d", h, buf, size, retval);
  return retval;
}
int64_t
Container_url_seek(void*h, int64_t position, int whence)
{
  int64_t retval = -1;
  try {
    URLProtocolHandler* handler = (URLProtocolHandler*)h;
    if (handler)
      retval = handler->url_seek(position,whence);
  } catch (...)
  {
    retval = -1;
  }
  VS_LOG_TRACE("URLProtocolHandler[%p]->url_seek(%p, %lld) ==> %d", h, position, whence, retval);
  return retval;
}

namespace com { namespace xuggle { namespace xuggler
{

  Container :: Container()
  {
    VS_LOG_TRACE("Making container: %p", this);
    mFormatContext = avformat_alloc_context();
    if (!mFormatContext)
      throw std::bad_alloc();
    // Set up thread interrupt handling.
    mFormatContext->interrupt_callback.callback = Global::avioInterruptCB;
    mFormatContext->interrupt_callback.opaque = this;
    mIsOpened = false;
    mIsMetaDataQueried=false;
    mNeedTrailerWrite = false;
    mNumStreams = 0;
    mInputBufferLength = 0;
    mReadRetryCount = 1;
    mCustomIOHandler = 0;
  }

  Container :: ~Container()
  {
    reset();
    resetContext();
    VS_LOG_TRACE("Destroyed container: %p", this);
  }

  void
  Container :: resetContext()
  {
    if (mFormatContext) {
      if (mCustomIOHandler) {
        if (mFormatContext->pb)
          av_freep(&mFormatContext->pb->buffer);
        av_freep(&mFormatContext->pb);
      }
      avformat_free_context(mFormatContext);
      mFormatContext = 0;
    }
  }
  void
  Container :: reset()
  {
    mMetaData.reset();
    if (mIsOpened)
    {
      VS_LOG_DEBUG("Closing dangling Container");
      (void) this->close(true);
    }
    if (mCustomIOHandler) {
      if (mFormatContext) {
        if(mFormatContext->pb)
          av_freep(&mFormatContext->pb->buffer);
        av_freep(&mFormatContext->pb);
      }
      delete mCustomIOHandler;
    }
    mCustomIOHandler = 0;
  }

  AVFormatContext *
  Container :: getFormatContext()
  {
    return mFormatContext;
  }

  int32_t
  Container :: setInputBufferLength(uint32_t size)
  {
    int32_t retval = -1;
    if (mIsOpened)
    {
      VS_LOG_WARN("Attempting to set input buffer length while file is opened; ignoring");
    }
    else
    {
      mInputBufferLength = size;
      retval = size;
    }
    return retval;
  }

  uint32_t
  Container :: getInputBufferLength()
  {
    return mInputBufferLength;
  }

  bool
  Container :: isOpened()
  {
    return mIsOpened;
  }

  bool
  Container :: isHeaderWritten()
  {
    return (mIsOpened && mNeedTrailerWrite);
  }

  int32_t
  Container :: open(const char *url,Type type,
      IContainerFormat *pContainerFormat)
  {
    return open(url, type, pContainerFormat, false, true);
  }

  int32_t
  Container :: open(const char *url,Type type,
      IContainerFormat *pContainerFormat,
      bool aStreamsCanBeAddedDynamically,
      bool aLookForAllStreams)
  {
    return open(url, type, pContainerFormat,
      aStreamsCanBeAddedDynamically, aLookForAllStreams,
      0, 0);
  }
  int32_t
  Container :: open(const char *url,Type type,
      IContainerFormat *pContainerFormat,
      bool aStreamsCanBeAddedDynamically,
      bool aLookForAllStreams,
      IMetaData* aOptions,
      IMetaData* aUnsetOptions)
  {
    AVDictionary *tmp=0;

    int32_t retval = -1;

    // reset if an open is called before a close.
    reset();
    if (!mFormatContext)
    {
      // always reset to a new one
      mFormatContext = avformat_alloc_context();
      if (!mFormatContext)
        throw std::bad_alloc();
    }

    if (pContainerFormat)
      setFormat(pContainerFormat);

    // Let's check for custom IO
    mCustomIOHandler = URLProtocolManager::findHandler(
        url,
        type == WRITE ? URLProtocolHandler::URL_WRONLY_MODE : URLProtocolHandler::URL_RDONLY_MODE,
            0);
    if (mCustomIOHandler) {
      if (mInputBufferLength <= 0)
        // default to 2k
        mInputBufferLength = 2048;
      // free and realloc the input buffer length
      uint8_t* buffer = (uint8_t*)av_malloc(mInputBufferLength);
      if (!buffer)
        throw std::bad_alloc();

      // we will allocate ourselves an io context; ownership of buffer passes here.
      mFormatContext->pb = avio_alloc_context(
          buffer,
          mInputBufferLength,
          type == WRITE ? 1 : 0,
          mCustomIOHandler,
          Container_url_read,
          Container_url_write,
          Container_url_seek);
      if (!mFormatContext->pb)
        av_free(buffer);
    }
    // set up our options
    if (aOptions) {
      MetaData* options = dynamic_cast<MetaData*>(aOptions);
      if (!options)
        throw std::runtime_error("um, this shouldn't ever happen");
      // make a copy of the data returned
      av_dict_copy(&tmp, options->getDictionary(), 0);
    }
    if (url && *url)
    {
      if (type == WRITE)
      {
        retval = openOutputURL(url,
            aStreamsCanBeAddedDynamically, &tmp);
      } else if (type == READ)
      {
        retval = openInputURL(url,
            aStreamsCanBeAddedDynamically, aLookForAllStreams, &tmp);
      }
      else
      {
        VS_ASSERT(false, "Invalid type for open");
        retval = -1;
      }
    }
    XUGGLER_CHECK_INTERRUPT(retval);
    if (aUnsetOptions) {
      MetaData* unsetOptions = dynamic_cast<MetaData*>(aUnsetOptions);
      if (!unsetOptions)
        throw std::runtime_error("a little part of me just died inside");
      unsetOptions->copy(tmp);
    }
    if (tmp)
      av_dict_free(&tmp);
    return retval;
  }

  IContainerFormat*
  Container :: getContainerFormat()
  {
    ContainerFormat *retval = ContainerFormat::make();
    if (retval)
    {
      if (mFormatContext)
      {
        if (mFormatContext->iformat)
          retval->setInputFormat(mFormatContext->iformat);
        if (mFormatContext->oformat)
          retval->setOutputFormat(mFormatContext->oformat);
      }
    }
    return retval;
  }

  int32_t
  Container:: setupAllInputStreams()
  {
    // do nothing if we're already all set up.
    if (mNumStreams == mFormatContext->nb_streams)
      return 0;

    int32_t retval = -1;
    // loop through and find the first non-zero time base
    AVRational *goodTimebase = 0;
    for(uint32_t i = 0;i < mFormatContext->nb_streams;i++){
      AVStream *avStream = mFormatContext->streams[i];
      if(avStream && avStream->time_base.num && avStream->time_base.den){
        goodTimebase = &avStream->time_base;
        break;
      }
    }

    // Only look for new streams
    for (uint32_t i =mNumStreams ; i < mFormatContext->nb_streams; i++)
    {
      AVStream *avStream = mFormatContext->streams[i];
      if (avStream)
      {
        if (goodTimebase && (!avStream->time_base.num || !avStream->time_base.den))
        {
          avStream->time_base = *goodTimebase;
        }

        RefPointer<Stream>* stream = new RefPointer<Stream>(
            Stream::make(this, avStream,
                (this->getType() == READ ?
                    IStream::INBOUND : IStream::OUTBOUND
                ), 0)
        );

        if (stream)
        {
          if (stream->value())
          {
            mStreams.push_back(stream);
            mNumStreams++;
          } else {
            VS_LOG_ERROR("Couldn't make a stream %d", i);
            delete stream;
          }
          stream = 0;
        }
        else
        {
          VS_LOG_ERROR("Could not make Stream %d", i);
          retval = -1;
        }
      } else {
        VS_LOG_ERROR("no FFMPEG allocated stream: %d", i);
        retval = -1;
      }
    }
    return retval;
  }

  int32_t
  Container :: openInputURL(const char *url,
      bool aStreamsCanBeAddedDynamically,
      bool aLookForAllStreams,
      AVDictionary** options)
  {
    int32_t retval = -1;
    AVInputFormat *inputFormat = 0;

    try {
      // We have prealloced the format
      if (mFormat)
      {
        inputFormat = mFormat->getInputFormat();
      }

      if (mCustomIOHandler) {
        retval = mCustomIOHandler->url_open(url, URLProtocolHandler::URL_RDONLY_MODE);
      } else {
        // rely on the avformat_open_input call to do the right thing
        retval = 0;
      }

      if (retval >= 0)
        retval = avformat_open_input(
            &mFormatContext,
            url,
            inputFormat,
            options
        );

      if (retval >= 0) {
        if (!mFormatContext && mFormatContext->iformat) {
          // we didn't know the format before, but we sure as hell know it now
          // so we set it back.
          RefPointer<ContainerFormat> format = ContainerFormat::make();
          if (format)
          {
            format->setInputFormat(mFormatContext->iformat);
            setFormat(format.value());
          }
        }
        mIsOpened = true;
        if (aStreamsCanBeAddedDynamically)
        {
          mFormatContext->ctx_flags |= AVFMTCTX_NOHEADER;
        }

        if (aLookForAllStreams)
        {
          retval = queryStreamMetaData();
        }
      } else {
        VS_LOG_DEBUG("Could not open output url: %s", url);
      }
    } catch (std::exception &e) {
      if (retval >= 0)
        retval = -1;
    }
    return retval;
  }

  int32_t
  Container :: openOutputURL(const char* url,
      bool aStreamsCanBeAddedDynamically,
      AVDictionary** options)
  {
    int32_t retval = -1;
    AVOutputFormat *outputFormat = 0;

    try {
      if (mFormat)
        // ask for it from the container
        outputFormat = mFormat->getOutputFormat();

      if (!outputFormat) {
        // guess it.
        outputFormat = av_guess_format(0, url, 0);
        RefPointer<ContainerFormat> format = ContainerFormat::make();
        if (!format)
          throw std::bad_alloc();

        format->setOutputFormat(outputFormat);
        setFormat(format.value());
      }

      if (!outputFormat)
        throw std::runtime_error("could not find output format");

      if (aStreamsCanBeAddedDynamically)
      {
        mFormatContext->ctx_flags |= AVFMTCTX_NOHEADER;
      }
      mFormatContext->oformat = outputFormat;
      // now because we can guess at the last moment, we need to set up
      // private options
      {
        AVFormatContext *s = mFormatContext;
        if (!s->priv_data && s->oformat->priv_data_size > 0) {
          s->priv_data = av_mallocz(s->oformat->priv_data_size);
          if (!s->priv_data)
            throw std::bad_alloc();
          if (s->oformat->priv_class) {
            *(const AVClass**)s->priv_data= s->oformat->priv_class;
            av_opt_set_defaults(s->priv_data);
          }
        }
      }

      // set options
      retval = av_opt_set_dict(mFormatContext, options);
      if (retval < 0)
        throw std::runtime_error("could not set options");

      if (mCustomIOHandler)
        retval = mCustomIOHandler->url_open(url, URLProtocolHandler::URL_WRONLY_MODE);
      else
        retval = avio_open2(&mFormatContext->pb,
            url, AVIO_FLAG_WRITE,
            &mFormatContext->interrupt_callback,
            0
        );
      if (retval < 0)
        throw std::runtime_error("could not open file");

      mIsOpened = true;
      strncpy(mFormatContext->filename, url, sizeof(mFormatContext->filename)-1);
      // force a null termination
      mFormatContext->filename[sizeof(mFormatContext->filename)-1] = 0;
    } catch (std::exception & e) {
      VS_LOG_ERROR("URL: %s; Error: %s", url, e.what());
      if (retval >= 0)
        retval = -1;
    }
    return retval;
  }

  IContainer::Type
  Container :: getType()
  {
    return (!mFormatContext ? READ :
        (mFormatContext->oformat ? WRITE: READ));
  }

  int32_t
  Container :: getNumStreams()
  {
    int32_t retval = 0;
    if (mFormatContext)
    {
      if (mFormatContext->nb_streams != mNumStreams)
        setupAllInputStreams();
      retval = mFormatContext->nb_streams;
    }
    XUGGLER_CHECK_INTERRUPT(retval);
    return retval;
  }

  int32_t
  Container :: close()
  {
    return close(false);
  }
  
  int32_t
  Container :: close(bool dangling)
  {
    int32_t retval = -1;
    mMetaData.reset();

    if (mFormatContext && mIsOpened)
    {
      VS_ASSERT(mNumStreams == mStreams.size(),
          "unexpected number of streams");

      if (mNeedTrailerWrite)
      {
        if (dangling)
          // don't actually write the trailer when dangling; we could
          // block on that, which could occur inside a finalizer thread
          // or other unexpected thread
          VS_LOG_ERROR("Disposing of dangling container but could not write trailer");
        else {
          VS_LOG_DEBUG("Writing dangling trailer");
          (void) this->writeTrailer();
        }
        mNeedTrailerWrite = false;
      }
      mOpenCoders.clear();

      while(mStreams.size() > 0)
      {
        RefPointer<Stream> * stream=mStreams.back();

        VS_ASSERT(stream && *stream, "no stream?");
        if (stream && *stream) {
          (*stream)->containerClosed(this);
          delete stream;
        }
        mStreams.pop_back();
      }
      mNumStreams = 0;

      // we need to remember the avio context
      AVIOContext* pb = mFormatContext->pb;

      if (this->getType() == READ)
        avformat_close_input(&mFormatContext);

      if (mCustomIOHandler) {
        retval = mCustomIOHandler->url_close();
        if (!mFormatContext) {
          if (pb)
            av_freep(&pb->buffer);
          av_free(pb);
        }
      } else if (this->getType() != READ)
        retval = avio_close(pb);
      else
        retval = 0;

      resetContext();
      mIsOpened = false;
      mIsMetaDataQueried=false;
    }
    XUGGLER_CHECK_INTERRUPT(retval);
    return retval;
  }

  Stream *
  Container :: getStream(uint32_t position)
  {
    Stream *retval = 0;
    if (mFormatContext)
    {
      if (mFormatContext->nb_streams != mNumStreams)
        setupAllInputStreams();

      if (position < mNumStreams)
      {
        // will acquire for caller.
        retval = (*mStreams.at(position)).get();
      }
    }
    return retval;
  }

  int32_t
  Container :: readNextPacket(IPacket * ipkt)
  {
    int32_t retval = -1;
    Packet* pkt = dynamic_cast<Packet*>(ipkt);
    if (mFormatContext && pkt)
    {
      AVPacket tmpPacket;
      AVPacket* packet=0;

      packet = &tmpPacket;
      av_init_packet(packet);
      pkt->reset();
      int32_t numReads=0;
      do
      {
        retval = av_read_frame(mFormatContext,
            packet);
        ++numReads;
      }
      while (retval == AVERROR(EAGAIN) &&
          (mReadRetryCount < 0 || numReads <= mReadRetryCount));

      if (retval >= 0)
        pkt->wrapAVPacket(packet);
      av_free_packet(packet);

      // Get a pointer to the wrapped packet
      packet = pkt->getAVPacket();
      // and dump it's contents
      VS_LOG_TRACE("read-packet: %lld, %lld, %d, %d, %d, %lld, %lld: %p",
          pkt->getDts(),
          pkt->getPts(),
          pkt->getFlags(),
          pkt->getStreamIndex(),
          pkt->getSize(),
          pkt->getDuration(),
          pkt->getPosition(),
          packet->data);
      
      // and let's try to set the packet time base if known
      if (pkt->getStreamIndex() >= 0)
      {
        RefPointer<IStream> stream = this->getStream(pkt->getStreamIndex());
        if (stream)
        {
          RefPointer<IRational> streamBase = stream->getTimeBase();
          if (streamBase)
          {
            pkt->setTimeBase(streamBase.value());
          }
        }
      }
    }
    XUGGLER_CHECK_INTERRUPT(retval);
    return retval;
  }
  int32_t
  Container :: writePacket(IPacket *ipkt)
  {
    return writePacket(ipkt, true);
  }
  int32_t
  Container :: writePacket(IPacket *ipkt, bool forceInterleave)
  {
    int32_t retval = -1;
    Packet *pkt = dynamic_cast<Packet*>(ipkt);
    try
    {
      if (this->getType() != WRITE)
        throw std::runtime_error("cannot write packet to read only container");
      
      if (!mFormatContext)
        throw std::logic_error("no format context");

      if (!pkt)
        throw std::runtime_error("cannot write missing packet");

      if (!pkt->isComplete())
        throw std::runtime_error("cannot write incomplete packet");

      if (!pkt->getSize())
        throw std::runtime_error("cannot write empty packet");

      if (!mNeedTrailerWrite)
        throw std::runtime_error("container has not written header yet");

      int32_t pktIndex = pkt->getStreamIndex();
      
      if ((uint32_t)pktIndex >= mNumStreams)
        throw std::runtime_error("packet being written to stream that doesn't exist");

      RefPointer<Stream> *streamPtr = mStreams.at(pktIndex);
      if (!streamPtr || !*streamPtr)
        throw std::runtime_error("no stream set up for this packet");
      
      Stream* stream = streamPtr->value();

      // Create a new packet that wraps the input data; this
      // just copies meta-data
      RefPointer<Packet> outPacket = Packet::make(pkt, false);
      // Stamp it with the stream data
      if (stream->stampOutputPacket(outPacket.value()) <0)
        throw std::runtime_error("could not stamp output packet");
      
      AVPacket *packet = 0;
      packet = outPacket->getAVPacket();
      if (!packet || !packet->data)
        throw std::runtime_error("no data in packet");
      
      /*
      VS_LOG_DEBUG("write-packet: %lld, %lld, %d, %d, %d, %lld, %lld: %p",
          pkt->getDts(),
          pkt->getPts(),
          pkt->getFlags(),
          pkt->getStreamIndex(),
          pkt->getSize(),
          pkt->getDuration(),
          pkt->getPosition(),
          packet->data);
          */
      
      if (forceInterleave)
        retval =  av_interleaved_write_frame(mFormatContext, packet);
      else
        retval = av_write_frame(mFormatContext, packet);
    }
    catch (std::exception & e)
    {
      VS_LOG_ERROR("Error: %s", e.what());
      retval = -1;
    }
    XUGGLER_CHECK_INTERRUPT(retval);
    return retval;
  }

  int32_t
  Container :: writeHeader()
  {
    int32_t retval = -1;
    try {
      if (this->getType() != WRITE)
        throw std::runtime_error("cannot write packet to read only container");

      if (!mFormatContext)
        throw std::runtime_error("no format context allocated");

#ifdef VS_DEBUG
      // for shits and giggles, dump the ffmpeg output
      // dump_format(mFormatContext, 0, (mFormatContext ? mFormatContext->filename :0), 1);
#endif // VS_DEBUG

      // now we're going to walk through and record each open stream coder.
      // this is needed to catch a potential error on writeTrailer().
      mOpenCoders.clear();
      int numStreams = getNumStreams();
      if (numStreams < 0 && 
          !(mFormatContext->ctx_flags & AVFMTCTX_NOHEADER))
        throw std::runtime_error("no streams added to container");
      
      if (numStreams == 0)
      {
        RefPointer<IContainerFormat> format = getContainerFormat();
        if (format)
        {
          const char *shortName = format->getOutputFormatShortName();
          if (shortName && !strcmp(shortName, "mp3"))
            throw std::runtime_error("no streams added to mp3 container");
        }
      }
      for(int i = 0; i < numStreams; i++)
      {
        RefPointer<IStream> stream = this->getStream(i);
        if (stream)
        {
          RefPointer<IStreamCoder> coder = stream->getStreamCoder();
          if (coder)
          {
            if (coder->isOpen())
              // add to our open list
              mOpenCoders.push_back(coder);
            else
              VS_LOG_TRACE("writing Header for container, but at least one codec (%d) is not opened first", i);
          }
        }
      }
      retval = avformat_write_header(mFormatContext,0);
      if (retval < 0)
        throw std::runtime_error("could not write header for container");

      // force a flush.
      avio_flush(mFormatContext->pb);
      // and remember that a writeTrailer is needed
      mNeedTrailerWrite = true;
    }
    catch (std::exception & e)
    {
      VS_LOG_ERROR("Error: %s", e.what());
      retval = -1;
    }
    XUGGLER_CHECK_INTERRUPT(retval);
    return retval;
  }
  int32_t
  Container :: writeTrailer()
  {
    int32_t retval = -1;
    try
    {
      if (this->getType() != WRITE)
        throw std::runtime_error("cannot write packet to read only container");

      if (!mFormatContext)
        throw std::runtime_error("no format context allocated");

      if (mNeedTrailerWrite)
      {
        while(mOpenCoders.size()>0)
        {
          RefPointer<IStreamCoder> coder = mOpenCoders.front();
          mOpenCoders.pop_front();
          if (!coder->isOpen())
          {
            mOpenCoders.clear();
            throw std::runtime_error("attempt to write trailer, but at least one used codec already closed");
          }
        }
        retval = av_write_trailer(mFormatContext);
        if (retval == 0)
        {
          avio_flush(mFormatContext->pb);
        }
      } else {
        VS_LOG_WARN("writeTrailer() with no matching call to writeHeader()");
      }
    }
    catch (std::exception & e)
    {
      VS_LOG_ERROR("Error: %s", e.what());
      retval = -1;
    }
    // regardless of whether or not the write trailer succeeded, since
    // an attempt has occurred, we shouldn't call it twice.
    mNeedTrailerWrite = false;
    XUGGLER_CHECK_INTERRUPT(retval);
    return retval;
  }

  int32_t
  Container :: queryStreamMetaData()
  {
    int retval = -1;
    if (mIsOpened)
    {
      if (!mIsMetaDataQueried)
      {
        retval = avformat_find_stream_info(mFormatContext, 0);
        // for shits and giggles, dump the ffmpeg output
        // dump_format(mFormatContext, 0, (mFormatContext ? mFormatContext->filename :0), 0);
        mIsMetaDataQueried = true;
      } else {
        retval = 0;
      }

      if (retval >= 0 && mFormatContext->nb_streams > 0)
      {
        setupAllInputStreams();
      } else {
        VS_LOG_WARN("Could not find streams in input container");
      }
    }
    else
    {
      VS_LOG_WARN("Attempt to queryStreamMetaData but container is not open");
    }
    XUGGLER_CHECK_INTERRUPT(retval);
    return retval;
  }

  int32_t
  Container :: seekKeyFrame(int32_t streamIndex, int64_t timestamp, int32_t flags)
  {
    int32_t retval = -1;

    if (mIsOpened)
    {
      if (streamIndex >= (int32_t)mNumStreams)
        VS_LOG_WARN("Attempt to seek on streamIndex %d but only %d streams known about in container",
            streamIndex, mNumStreams);
      else
        retval = av_seek_frame(mFormatContext, streamIndex, timestamp, flags);
    }
    else
    {
      VS_LOG_WARN("Attempt to seekKeyFrame but container is not open");
    }
    XUGGLER_CHECK_INTERRUPT(retval);
    return retval;
  }

  int32_t
  Container :: seekKeyFrame(int32_t streamIndex, int64_t minTimeStamp,
          int64_t targetTimeStamp, int64_t maxTimeStamp, int32_t flags)
  {
    int32_t retval = -1;

    if (mIsOpened)
    {
      if (streamIndex >= (int32_t)mNumStreams)
        VS_LOG_WARN("Attempt to seek on streamIndex %d but only %d streams known about in container",
            streamIndex, mNumStreams);
      else
        retval = avformat_seek_file(mFormatContext, streamIndex,
            minTimeStamp,
            targetTimeStamp,
            maxTimeStamp,
            flags);
    }
    else
    {
      VS_LOG_WARN("Attempt to seekKeyFrame but container is not open");
    }
    XUGGLER_CHECK_INTERRUPT(retval);
    return retval;
  }

  int64_t
  Container :: getDuration()
  {
    int64_t retval = Global::NO_PTS;
    queryStreamMetaData();
    if (mFormatContext)
      retval = mFormatContext->duration;
    return retval;
  }

  int64_t
  Container :: getStartTime()
  {
    int64_t retval = Global::NO_PTS;
    queryStreamMetaData();
    if (mFormatContext)
      retval = mFormatContext->start_time;
    return retval;
  }

  int64_t
  Container :: getFileSize()
  {
    int64_t retval = -1;
    queryStreamMetaData();
    if (mFormatContext) {
      if (mFormatContext->iformat && mFormatContext->iformat->flags & AVFMT_NOFILE)
        retval = 0;
      else {
        retval = avio_size(mFormatContext->pb);
        retval = FFMAX(0, retval);
      }
    }
    return retval;
  }

  int32_t
  Container :: getBitRate()
  {
    int32_t retval = -1;
    queryStreamMetaData();
    if (mFormatContext)
      retval = mFormatContext->bit_rate;
    return retval;
  }

  int32_t
  Container :: setPreload(int32_t)
  {
    VS_LOG_WARN("Deprecated and will be removed; does nothing now.");
    return -1;
  }

  int32_t
  Container :: getPreload()
  {
    VS_LOG_WARN("Deprecated and will be removed; does nothing now.");
    return -1;
  }

  int32_t
  Container :: setMaxDelay(int32_t maxdelay)
  {
    int32_t retval = -1;
    if (mIsOpened || !mFormatContext)
    {
      VS_LOG_WARN("Attempting to set max delay while file is opened; ignoring");
    }
    else
    {
      mFormatContext->max_delay = maxdelay;
      retval = maxdelay;
    }
    return retval;
  }

  int32_t
  Container :: getMaxDelay()
  {
    int32_t retval = -1;
    if (mFormatContext)
      retval = mFormatContext->max_delay;
    return retval;
  }

  int32_t
  Container :: getNumProperties()
  {
    return Property::getNumProperties(mFormatContext);
  }

  IProperty*
  Container :: getPropertyMetaData(int32_t propertyNo)
  {
    return Property::getPropertyMetaData(mFormatContext, propertyNo);
  }

  IProperty*
  Container :: getPropertyMetaData(const char *name)
  {
    return Property::getPropertyMetaData(mFormatContext, name);
  }

  int32_t
  Container :: setProperty(IMetaData* valuesToSet, IMetaData* valuesNotFound)
  {
    return Property::setProperty(mFormatContext, valuesToSet, valuesNotFound);
  }

  int32_t
  Container :: setProperty(const char* aName, const char *aValue)
  {
    return Property::setProperty(mFormatContext, aName, aValue);
  }

  int32_t
  Container :: setProperty(const char* aName, double aValue)
  {
    return Property::setProperty(mFormatContext, aName, aValue);
  }

  int32_t
  Container :: setProperty(const char* aName, int64_t aValue)
  {
    return Property::setProperty(mFormatContext, aName, aValue);
  }

  int32_t
  Container :: setProperty(const char* aName, bool aValue)
  {
    return Property::setProperty(mFormatContext, aName, aValue);
  }


  int32_t
  Container :: setProperty(const char* aName, IRational *aValue)
  {
    return Property::setProperty(mFormatContext, aName, aValue);
  }


  char*
  Container :: getPropertyAsString(const char *aName)
  {
    return Property::getPropertyAsString(mFormatContext, aName);
  }

  double
  Container :: getPropertyAsDouble(const char *aName)
  {
    return Property::getPropertyAsDouble(mFormatContext, aName);
  }

  int64_t
  Container :: getPropertyAsLong(const char *aName)
  {
    return Property::getPropertyAsLong(mFormatContext, aName);
  }

  IRational*
  Container :: getPropertyAsRational(const char *aName)
  {
    return Property::getPropertyAsRational(mFormatContext, aName);
  }

  bool
  Container :: getPropertyAsBoolean(const char *aName)
  {
    return Property::getPropertyAsBoolean(mFormatContext, aName);
  }

  int32_t
  Container :: getFlags()
  {
    int32_t flags = (mFormatContext ? mFormatContext->flags : 0);
    // remove custom io if set
    flags &= ~(AVFMT_FLAG_CUSTOM_IO);
    return flags;
  }

  void
  Container :: setFlags(int32_t newFlags)
  {
    if (mFormatContext) {
      mFormatContext->flags = newFlags;
      // force custom io
      mFormatContext->flags |= AVFMT_FLAG_CUSTOM_IO;
    }
  }

  bool
  Container :: getFlag(IContainer::Flags flag)
  {
    bool result = false;
    if (mFormatContext)
      result = mFormatContext->flags& flag;
    return result;
  }

  void
  Container :: setFlag(IContainer::Flags flag, bool value)
  {
    if (mFormatContext)
    {
      if (value)
      {
        mFormatContext->flags |= flag;
      }
      else
      {
        mFormatContext->flags &= (~flag);
      }
    }

  }
  
  const char*
  Container :: getURL()
  {
    return mFormatContext && *mFormatContext->filename ? mFormatContext->filename : 0;
  }
  
  int32_t
  Container :: flushPackets()
  {
    int32_t retval = -1;
    try
    {
      if (this->getType() != WRITE)
        throw std::runtime_error("cannot write packet to read only container");

      if (!mFormatContext)
        throw std::runtime_error("no format context allocated");

      // Do the flush
      avio_flush(mFormatContext->pb);
      retval = 0;
    }
    catch (std::exception & e)
    {
      VS_LOG_ERROR("Error: %s", e.what());
      retval = -1;
    }
    XUGGLER_CHECK_INTERRUPT(retval);
    return retval;
  }

  int32_t
  Container :: getReadRetryCount()
  {
    return mReadRetryCount;
  }
  
  void
  Container :: setReadRetryCount(int32_t aCount)
  {
    mReadRetryCount = aCount;
  }
  
  int32_t
  Container :: setFormat(IContainerFormat* aFormat)
  {
    int32_t retval = -1;
    ContainerFormat* format = dynamic_cast<ContainerFormat*>(aFormat);
    try {
      if (!format)
        throw std::runtime_error("no format set");

      if (!mFormatContext)
        throw std::runtime_error("no underlying AVFormatContext");
      if (mFormatContext->iformat || mFormatContext->oformat)
        throw std::runtime_error("format already set on this IContainer; cannot be reset");
      if (mIsOpened)
        throw std::runtime_error("container already opened");

      AVOutputFormat* oformat = format->getOutputFormat();
      AVInputFormat* iformat = format->getInputFormat();

      if (!iformat && !oformat)
        throw std::runtime_error("no input or output format set");

      // iformat, if set, always wins
      if (iformat) {
        mFormatContext->iformat = iformat;
        mFormatContext->oformat = 0;
      } else {
        // throw away the old context and use the new to get the correct options set up
        resetContext();
        mFormatContext = 0;
        if (avformat_alloc_output_context2(&mFormatContext, oformat, 0, 0)<0)
          throw std::runtime_error("could not allocate output context");
      }
      mFormat.reset(format, true);
      retval = 0;
    } catch (std::exception &e)
    {
      retval = -1;
    }
    return retval;
  }
  bool
  Container :: canStreamsBeAddedDynamically()
  {
    if (mFormatContext)
      return mFormatContext->ctx_flags & AVFMTCTX_NOHEADER;
    return false;
  }

  IMetaData*
  Container :: getMetaData()
  {
    if (!mMetaData && mFormatContext)
    {
      if (this->getType() == WRITE)
        mMetaData = MetaData::make(&mFormatContext->metadata);
      else
        // make a read-only copy so when libav deletes the
        // input version we don't delete our copy
        mMetaData = MetaData::make(mFormatContext->metadata);
    }
    return mMetaData.get();
  }
  void
  Container :: setMetaData(IMetaData * copy)
  {
    MetaData* data = dynamic_cast<MetaData*>(getMetaData());
    if (data) {
      data->copy(copy);
      // release for the get above
      data->release();
    }
    return;
  }
  
  int32_t
  Container:: createSDPData(com::xuggle::ferry::IBuffer* buffer)
  {
    if (!mFormatContext)
      return -1;
    if (!buffer)
      return -1;
    
    int32_t bufSize = buffer->getBufferSize();
    if (bufSize <= 0)
      return -1;
    
    char* bytes = static_cast<char*>(buffer->getBytes(0, bufSize));
    if (!bytes)
      return -1;
    
    bytes[0] = 0;
    // null terminate the buffer to ensure strlen below doesn't
    // overrun
    bytes[bufSize-1]=0;
    int32_t ret = av_sdp_create(&mFormatContext, 1, bytes, bufSize-1);

    if (ret < 0)
    {
      VS_LOG_INFO("Could not create SDP file: %d", ret);
      return ret;
    }
    // Otherwise, we have to figure out the length, including the 
    // terminating null
    ret = strlen(bytes)+1;
    return ret;
  }

  int32_t
  Container :: setForcedAudioCodec(ICodec::ID id)
  {
    int32_t retval=-1;
    if (mFormatContext && id != ICodec::CODEC_ID_NONE)
    {
      RefPointer<ICodec> codec = ICodec::findDecodingCodec(id);
      if (codec && codec->getType() == ICodec::CODEC_TYPE_AUDIO)
        mFormatContext->audio_codec_id = (enum CodecID) id;
    }
    return retval;
  }

  int32_t
  Container :: setForcedVideoCodec(ICodec::ID id)
  {
    int32_t retval=-1;
    if (mFormatContext && id != ICodec::CODEC_ID_NONE)
    {
      RefPointer<ICodec> codec = ICodec::findDecodingCodec(id);
      if (codec && codec->getType() == ICodec::CODEC_TYPE_VIDEO)
        mFormatContext->video_codec_id = (enum CodecID) id;
    }
    return retval;
  }

  int32_t
  Container :: setForcedSubtitleCodec(ICodec::ID id)
  {
    int32_t retval=-1;
    if (mFormatContext && id != ICodec::CODEC_ID_NONE)
    {
      RefPointer<ICodec> codec = ICodec::findDecodingCodec(id);
      if (codec && codec->getType() == ICodec::CODEC_TYPE_SUBTITLE)
        mFormatContext->subtitle_codec_id = (enum CodecID) id;
    }
    return retval;
  }

  Stream*
  Container :: addNewStream(ICodec::ID id)
  {
    Stream* retval=0;
    RefPointer<ICodec> codec;
    try
    {
      codec = ICodec::findEncodingCodec(id);

      if (!codec) {
        VS_LOG_ERROR("Could not find encoding codec: %d", id);
        throw std::runtime_error("Could not find encoding codec");
      }
      retval = addNewStream(codec.value());
    }
    catch (std::exception & e)
    {
      VS_LOG_DEBUG("Error: %s", e.what());
      VS_REF_RELEASE(retval);
    }
    return retval;
  }

  Stream*
  Container:: addNewStream(ICodec* aCodec)
  {
    Codec* codec = dynamic_cast<Codec*>(aCodec);
    Stream *retval=0;
    try
    {
      AVCodec* avCodec = codec ? codec->getAVCodec() : 0;

      if (!mFormatContext)
        throw std::runtime_error("no format context");

      if (!isOpened())
        throw std::runtime_error("attempted to add stream to "
            " unopened container");

      if (isHeaderWritten())
        throw std::runtime_error("cannot add stream after header is written"
            );

      AVStream * stream=0;
      stream = avformat_new_stream(mFormatContext, avCodec);


      if (!stream)
        throw std::runtime_error("could not allocate stream");

      RefPointer<Stream>* p = new RefPointer<Stream>(
          Stream::make(this, stream, IStream::OUTBOUND, avCodec)
      );
      if (!p) throw std::bad_alloc();
      if (*p)
      {
        mStreams.push_back(p);
        mNumStreams++;
        retval = p->get(); // acquire for caller
      }
      else
      {
        delete p;
        throw std::bad_alloc();
      }
    } catch (std::exception & e)
    {
      VS_LOG_DEBUG("Error: %s", e.what());
      VS_REF_RELEASE(retval);
    }
    return retval;
  }

  Stream*
  Container :: addNewStream(IStreamCoder* aCoder)
  {
    Stream *retval=0;
    StreamCoder * coder = dynamic_cast<StreamCoder*>(aCoder);
    RefPointer<ICodec> codec;
    try
    {
      if (!coder)
        throw std::runtime_error("must pass non-null coder");
      codec = coder->getCodec();
      if (!codec)
        throw std::runtime_error("StreamCoder has no attached Codec");

      retval = this->addNewStream(codec.value());
      if (retval)
      {
        if (retval->setStreamCoder(coder) < 0)
          throw std::runtime_error("Could not set StreamCoder on stream");
      }

    } catch (std::exception & e)
    {
      VS_LOG_DEBUG("addNewStream Error: %s", e.what());
      VS_REF_RELEASE(retval);
    }
    return retval;
  }
  
  Stream*
  Container :: addNewStream(int32_t id)
  {
    Stream *retval=0;
    retval = addNewStream((ICodec*)0);
    if (retval)
      retval->setId(id);
    return retval;
  }

  Container*
  Container :: make(IContainerFormat* format)
  {
    Container* retval;
    retval = Container::make();
    if (retval)
      if (retval->setFormat(format) < 0)
        VS_REF_RELEASE(retval);
    return retval;
  }

}}}
