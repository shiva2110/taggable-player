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
#include <cstring>

#define attribute_deprecated

#include <com/xuggle/ferry/Logger.h>
#include <com/xuggle/ferry/RefPointer.h>

#include <com/xuggle/xuggler/Global.h>
#include <com/xuggle/xuggler/StreamCoder.h>
#include <com/xuggle/xuggler/Codec.h>
#include <com/xuggle/xuggler/Rational.h>
#include <com/xuggle/xuggler/AudioSamples.h>
#include <com/xuggle/xuggler/VideoPicture.h>
#include <com/xuggle/xuggler/Packet.h>
#include <com/xuggle/xuggler/Property.h>
#include <com/xuggle/xuggler/MetaData.h>

extern "C" {
#include <libavutil/dict.h>
}
VS_LOG_SETUP(VS_CPP_PACKAGE);

namespace com {
namespace xuggle {
namespace xuggler {
using namespace com::xuggle::ferry;

StreamCoder::StreamCoder() :
  mCodec(0)
{
  mCodecContext = 0;
  // default to DECODING.
  mDirection = DECODING;
  mOpened = false;
  mStream = 0;
  mAudioFrameBuffer = 0;
  mBytesInFrameBuffer = 0;
  mFakePtsTimeBase = IRational::make(1, AV_TIME_BASE);
  mFakeNextPts = Global::NO_PTS;
  mFakeCurrPts = Global::NO_PTS;
  mLastPtsEncoded = Global::NO_PTS;
  mSamplesCoded = 0;
  mLastExternallySetTimeStamp = Global::NO_PTS;
  mDefaultAudioFrameSize = 576;
  mNumDroppedFrames = 0;
  mAutomaticallyStampPacketsForStream = true;
  for(uint32_t i = 0; i < sizeof(mPtsBuffer)/sizeof(mPtsBuffer[0]); i++)
  {
    mPtsBuffer[i] = Global::NO_PTS;
  }
}

StreamCoder::~StreamCoder()
{
  reset();
}

void
StreamCoder::resetOptions(AVCodecContext* ctx)
{
  if (!ctx)
    return;
  if (ctx->codec && ctx->codec->priv_class && ctx->priv_data)
    av_opt_free(ctx->priv_data);
  av_opt_free(ctx);
  av_freep(&ctx->priv_data);
}

void
StreamCoder::reset()
{
  // Auto-close if the caller forgot to close this
  // stream-coder
  if (mOpened)
  {
    VS_LOG_DEBUG("Closing dangling StreamCoder");
    (void) this->close();
  }

  mAutomaticallyStampPacketsForStream = true;
  mOpened = false;
  // Don't free if we're attached to a Stream.
  // The Container will do that.
  if (mCodecContext) {
    // for reasons that I'm sure they will fix some day, avformat_free_context
    // does not free private options.  it should.
    resetOptions(mCodecContext);
    if(!mStream)
    {
      av_freep(&mCodecContext->extradata);
      av_freep(&mCodecContext->subtitle_header);
      av_freep(&mCodecContext);
    }
  }
  mCodecContext = 0;
  // We do not refcount the stream
  mStream = 0;
}

int32_t
StreamCoder :: readyAVContexts(
    Direction aDirection,
    StreamCoder *aCoder,
    Stream* aStream,
    Codec *aCodec,
    AVCodecContext *avContext,
    AVCodec *avCodec)
{
  int retval = -1;
  if (!avContext)
    return retval;

  if (avContext->sample_fmt == AV_SAMPLE_FMT_NONE)
    avContext->sample_fmt = AV_SAMPLE_FMT_S16;

  if (!aCodec) {
    if (avCodec) {
      aCoder->mCodec = Codec::make(avCodec);
    } else if (aDirection == ENCODING) {
      aCoder->mCodec = dynamic_cast<Codec*>(ICodec::findEncodingCodecByIntID(avContext->codec_id));
    } else {
      aCoder->mCodec = dynamic_cast<Codec*>(ICodec::findDecodingCodecByIntID(avContext->codec_id));
    }
  } else
    aCoder->mCodec.reset(aCodec, true);

  aCodec = aCoder->mCodec.value(); // re-use reference
  avCodec = aCodec ? aCodec->getAVCodec() : 0;
  if (aCoder->mCodecContext) {
    // was previously set; we should do something about that.
//    VS_LOG_ERROR("Warning... mojo rising");
    resetOptions(aCoder->mCodecContext);
  }
  aCoder->mCodecContext = avContext;
  aCoder->mStream = aStream;
  aCoder->mDirection = aDirection;

  avContext->opaque = aCodec;

  // these settings are necessary to have property setting work correctly
  // and avContext->codec will need to be temporarily disabled BEFORE
  // #open is called.
  avContext->codec_id = avCodec ? avCodec->id : CODEC_ID_NONE;
  avContext->codec_type = avCodec ? avCodec->type : AVMEDIA_TYPE_UNKNOWN;
  avContext->codec = avCodec;

  switch (avContext->codec_type)
  {
    case AVMEDIA_TYPE_AUDIO:
      if (avContext->block_align == 1 && avContext->codec_id == CODEC_ID_MP3)
        avContext->block_align = 0;
      if (avContext->codec_id == CODEC_ID_AC3)
        avContext->block_align = 0;
      break;
    default:
      break;
  }

  VS_LOG_TRACE("StreamCoder %p codec set to: %s [%s]",
      aCoder,
      avCodec ? avCodec->name : "unknown",
      avCodec ? avCodec->long_name : "unknown; caller must call IStreamCoder.setCodec(ICodec)");
  if (!avCodec && (avContext->codec_type == AVMEDIA_TYPE_AUDIO || avContext->codec_type == AVMEDIA_TYPE_VIDEO)) {
    VS_LOG_WARN("DEPRECATED; StreamCoder %p created without Codec.  Caller must call"
        " IStreamCoder.setCodec(ICodec) before any other methods", aCoder);
  }

  retval = 0;
  return retval;
}

void
StreamCoder::setCodec(ICodec * aCodec)
{
  Codec* codec = dynamic_cast<Codec*>(aCodec);
  AVCodec* avCodec = 0;

  if (!codec) {
    VS_LOG_INFO("Cannot set codec to null codec");
    return;
  }
  avCodec = codec->getAVCodec();

  if (!mCodecContext) {
    VS_LOG_ERROR("No codec context");
    return;
  }

  if (mCodecContext->codec_id != CODEC_ID_NONE || mCodecContext->codec) {
    VS_LOG_INFO("Codec already set to codec: %d. Ignoring setCodec call",
        mCodecContext->codec_id);
    return;
  }

  if (mCodec.value() == aCodec) {
    // setting to the same value; don't reset anything
    return;
  }

  if (mCodecContext) {
    // avcodec_get_context_defaults3 does not clear these out if already allocated
    // so we do.
    resetOptions(mCodecContext);
    av_freep(&mCodecContext->extradata);
    av_freep(&mCodecContext->subtitle_header);

    avcodec_get_context_defaults3(mCodecContext, avCodec);
  } else {
    mCodecContext = avcodec_alloc_context3(avCodec);
  }

  if (mCodecContext)
    readyAVContexts(mDirection,
        this,
        mStream,
        codec,
        mCodecContext,
        0);
}

void
StreamCoder::setCodec(ICodec::ID id)
{
  setCodec((int32_t) id);
}

void
StreamCoder::setCodec(int32_t id)
{
  RefPointer<Codec> codec = 0;
  if (ENCODING == mDirection)
  {
    codec = Codec::findEncodingCodecByIntID(id);
  }
  else
  {
    codec = Codec::findDecodingCodecByIntID(id);
  }
  if (codec)
    setCodec(codec.value());
}

StreamCoder*
StreamCoder::make(Direction direction, ICodec::ID id)
{
  RefPointer<Codec> codec = 0;
  StreamCoder* retval = 0;
  if (ENCODING == direction)
  {
    codec = Codec::findEncodingCodec(id);
  }
  else
  {
    codec = Codec::findDecodingCodec(id);
  }
  if (codec)
    retval = StreamCoder::make(direction, codec.value());
  return retval;
}

StreamCoder *
StreamCoder :: make (Direction direction, Codec* codec)
{
  StreamCoder* retval= 0;

  try
  {
    AVCodecContext* codecCtx=0;
    AVCodec* avCodec = 0;

    if (codec)
      avCodec = codec->getAVCodec();

    retval = StreamCoder::make();
    if (!retval)
      throw std::bad_alloc();

    codecCtx = avcodec_alloc_context3(avCodec);

    if (readyAVContexts(
        direction,
        retval,
        0,
        codec,
        codecCtx,
        avCodec) < 0)
      throw std::runtime_error("could not initialize codec");
  }
  catch (std::exception & e)
  {
    VS_REF_RELEASE(retval);
  }
  return retval;
}

StreamCoder *
StreamCoder::make(Direction direction, IStreamCoder* aCoder)
{
  StreamCoder *retval = 0;
  StreamCoder *coder = dynamic_cast<StreamCoder*> (aCoder);

  try
  {
    if (!coder)
      throw std::runtime_error("cannot make stream coder from null coder");

    RefPointer<Codec> codecToUse;

    codecToUse = coder->getCodec();

    retval = make(direction, codecToUse.value());
    if (!retval)
      throw std::bad_alloc();

    AVCodecContext* codec = retval->mCodecContext;
    AVCodecContext* icodec = coder->mCodecContext;

    // no really; I hate FFmpeg memory management; a lot of sensible
    // defaults got set up that we'll need to clear now.
    resetOptions(codec);
    av_freep(&codec->extradata);
    av_freep(&codec->subtitle_header);

    // temporarily set this back to zero
    codec->codec = 0;
    avcodec_copy_context(codec, icodec);
    // and copy it back by hand to ensure setProperty methods
    // work again
    codec->codec = icodec->codec;

    RefPointer<IStream> stream = coder->getStream();
    RefPointer<IRational> streamBase = stream ? stream->getTimeBase() : 0;
    double base = streamBase ? streamBase->getDouble() : 0;

    if (base && av_q2d(icodec->time_base) * icodec->ticks_per_frame > base
        && base < 1.0 / 1000)
    {
      codec->time_base.num *= icodec->ticks_per_frame;
    }
    if (!codec->time_base.num || !codec->time_base.den)
    {
      RefPointer<IRational> iStreamBase = coder->getTimeBase();
      if (iStreamBase)
      {
        codec->time_base.num = iStreamBase->getNumerator();
        codec->time_base.den = iStreamBase->getDenominator();
      }
    }

    if (readyAVContexts(
        direction,
        retval,
        0,
        codecToUse.value(),
        codec,
        icodec->codec) < 0)
      throw std::runtime_error("could not initialize AVContext");

  }
  catch (std::exception &e)
  {
    VS_LOG_WARN("Error: %s", e.what());
    VS_REF_RELEASE(retval);
  }
  return retval;
}

StreamCoder *
StreamCoder::make(Direction direction, AVCodecContext * codecCtx,
    AVCodec* avCodec, Stream* stream)
{
  StreamCoder *retval = 0;
  RefPointer<Codec> codec;

  if (codecCtx)
  {
    try
    {
       retval = StreamCoder::make();
       if (readyAVContexts(
           direction,
           retval,
           stream,
           0,
           codecCtx,
           avCodec) < 0)
         throw std::runtime_error("could not initialize codec");
    }
    catch (std::exception &e)
    {
      VS_REF_RELEASE(retval);
      throw e;
    }
  }
  return retval;
}

IStream*
StreamCoder::getStream()
{
  // Acquire for the caller.
  VS_REF_ACQUIRE(mStream);
  return mStream;
}

Codec *
StreamCoder::getCodec()
{
  return mCodec ? mCodec.get() : 0;
}

ICodec::Type
StreamCoder::getCodecType()
{
  ICodec::Type retval = ICodec::CODEC_TYPE_UNKNOWN;
  if (mCodecContext)
  {
    retval = (ICodec::Type) mCodecContext->codec_type;
  }
  else
  {
    VS_LOG_WARN("Attempt to get CodecType from uninitialized StreamCoder");
  }
  return retval;
}

ICodec::ID
StreamCoder::getCodecID()
{
  ICodec::ID retval = ICodec::CODEC_ID_NONE;
  if (mCodecContext)
  {
    retval = (ICodec::ID) mCodecContext->codec_id;
  }
  else
  {
    VS_LOG_WARN("Attempt to get CodecID from uninitialized StreamCoder");
  }
  return retval;
}

int32_t
StreamCoder::getBitRate()
{
  return (mCodecContext ? mCodecContext->bit_rate : -1);
}
void
StreamCoder::setBitRate(int32_t val)
{
  if (mCodecContext && !mOpened)
    mCodecContext->bit_rate = val;
}
int32_t
StreamCoder::getBitRateTolerance()
{
  return (mCodecContext ? mCodecContext->bit_rate_tolerance : -1);
}
void
StreamCoder::setBitRateTolerance(int32_t val)
{
  if (mCodecContext && !mOpened)
    mCodecContext->bit_rate_tolerance = val;
}
int32_t
StreamCoder::getHeight()
{
  return (mCodecContext ? mCodecContext->height : -1);
}

void
StreamCoder::setHeight(int32_t val)
{
  if (mCodecContext && !mOpened)
    mCodecContext->height = val;
}

int32_t
StreamCoder::getWidth()
{
  return (mCodecContext ? mCodecContext->width : -1);
}

void
StreamCoder::setWidth(int32_t val)
{
  if (mCodecContext && !mOpened)
    mCodecContext->width = val;
}

IRational*
StreamCoder::getTimeBase()
{
  // we make a new value and return it; caller must
  // release.
  IRational *retval = 0;

  // annoyingly, some codec contexts will NOT have
  // a timebase... so we take it from stream then.
  if (mCodecContext && mCodecContext->time_base.den
      && mCodecContext->time_base.num)
  {
    retval = Rational::make(&mCodecContext->time_base);
  }
  else if (mAutomaticallyStampPacketsForStream)
  {
    retval = mStream ? mStream->getTimeBase() : 0;
  }

  return retval;
}

void
StreamCoder::setTimeBase(IRational* src)
{
  if (mCodecContext && src && !mOpened)
  {
    mCodecContext->time_base.num = src->getNumerator();
    mCodecContext->time_base.den = src->getDenominator();
  }
  else
  {
    VS_LOG_INFO("Failed to setTimeBase on StreamCoder");
  }
}

int64_t
StreamCoder::getNextPredictedPts()
{
  return mFakeNextPts;
}

IRational*
StreamCoder::getFrameRate()
{
  return (mStream ? mStream->getFrameRate() : 0);
}

void
StreamCoder::setFrameRate(IRational* src)
{
  if (mStream && !mOpened)
    mStream->setFrameRate(src);
}

int32_t
StreamCoder::getNumPicturesInGroupOfPictures()
{
  return (mCodecContext ? mCodecContext->gop_size : -1);
}

void
StreamCoder::setNumPicturesInGroupOfPictures(int32_t val)
{
  if (mCodecContext && !mOpened)
    mCodecContext->gop_size = val;
}

IPixelFormat::Type
StreamCoder::getPixelType()
{
  IPixelFormat::Type retval = IPixelFormat::NONE;
  int32_t type = 0;
  if (mCodecContext)
  {
    retval = (IPixelFormat::Type) mCodecContext->pix_fmt;

    // little check here to see if we have an undefined int32_t.
    type = (int32_t) retval;
    if (type != mCodecContext->pix_fmt) {
      VS_LOG_ERROR("Undefined pixel format type: %d", mCodecContext->pix_fmt);
      retval = IPixelFormat::NONE;
    }
  }
  return retval;
}

void
StreamCoder::setPixelType(IPixelFormat::Type type)
{
  if (mCodecContext && !mOpened)
  {
    mCodecContext->pix_fmt = (enum PixelFormat) type;
  }
}

int32_t
StreamCoder::getSampleRate()
{
  return (mCodecContext ? mCodecContext->sample_rate : -1);
}

void
StreamCoder::setSampleRate(int32_t val)
{
  if (mCodecContext && !mOpened && val > 0)
    mCodecContext->sample_rate = val;
}

IAudioSamples::Format
StreamCoder::getSampleFormat()
{
  return (IAudioSamples::Format) (mCodecContext ? mCodecContext->sample_fmt
      : -1);
}

void
StreamCoder::setSampleFormat(IAudioSamples::Format val)
{
  if (mCodecContext && !mOpened && val > 0)
    mCodecContext->sample_fmt = (enum AVSampleFormat) val;
}

int32_t
StreamCoder::getChannels()
{
  return (mCodecContext ? mCodecContext->channels : -1);
}

void
StreamCoder::setChannels(int32_t val)
{
  if (mCodecContext && !mOpened && val > 0)
    mCodecContext->channels = val;
}

int32_t
StreamCoder::getGlobalQuality()
{
  return (mCodecContext ? mCodecContext->global_quality : FF_LAMBDA_MAX);
}

void
StreamCoder::setGlobalQuality(int32_t newQuality)
{
  if (newQuality < 0 || newQuality > FF_LAMBDA_MAX)
    newQuality = FF_LAMBDA_MAX;
  if (mCodecContext)
    mCodecContext->global_quality = newQuality;
}

int32_t
StreamCoder::getFlags()
{
  return (mCodecContext ? mCodecContext->flags : 0);
}

void
StreamCoder::setFlags(int32_t newFlags)
{
  if (mCodecContext)
    mCodecContext->flags = newFlags;
}

bool
StreamCoder::getFlag(IStreamCoder::Flags flag)
{
  bool result = false;
  if (mCodecContext)
    result = mCodecContext->flags & flag;
  return result;
}

void
StreamCoder::setFlag(IStreamCoder::Flags flag, bool value)
{
  if (mCodecContext)
  {
    if (value)
    {
      mCodecContext->flags |= flag;
    }
    else
    {
      mCodecContext->flags &= (~flag);
    }
  }

}

int32_t
StreamCoder::open()
{
  return open(0, 0);
}
int32_t
StreamCoder::open(IMetaData* aOptions, IMetaData* aUnsetOptions)
{
  int32_t retval = -1;
  AVDictionary* tmp=0;
  try
  {
    if (!mCodecContext)
      throw std::runtime_error("no codec context");

    if (!mCodec)
    {
      RefPointer<ICodec> codec = this->getCodec();
      // This should set mCodec and then release
      // the local reference.
    }

    if (!mCodec)
      throw std::runtime_error("no codec set for coder");

    // Time to set options
    if (aOptions) {
        MetaData* options = dynamic_cast<MetaData*>(aOptions);
        if (!options)
          throw new std::runtime_error("wow... who's passing us crap?");

        // make a copy of the data returned.
        av_dict_copy(&tmp, options->getDictionary(), 0);
    }

    // don't allow us to open a coder without a time base
    if (mDirection == ENCODING && mCodecContext->time_base.num == 0)
    {
      if (this->getCodecType() == ICodec::CODEC_TYPE_AUDIO)
      {
        if (mCodecContext->sample_rate > 0)
        {
          mCodecContext->time_base.num = 1;
          mCodecContext->time_base.den = mCodecContext->sample_rate;
        }
        else
        {
          throw std::runtime_error("no sample rate set on coder");
        }
      }
      else
        throw std::runtime_error("no timebase set on coder");
    }

    // Fix for issue #14: http://code.google.com/p/xuggle/issues/detail?id=14
    if (mStream)
    {
      RefPointer<IContainer> container = mStream->getContainer();
      if (container)
      {
        RefPointer<IContainerFormat> format = container->getContainerFormat();
        if (format && mDirection == ENCODING && format->getOutputFlag(
            IContainerFormat::FLAG_GLOBALHEADER))
        {
          this->setFlag(FLAG_GLOBAL_HEADER, true);
        }
      }
    }

    {
      /*
       * This is a very annoying bug.  FFmpeg will NOT find sub-codec options
       * if AVCodecContext->codec is not set to a non-null value, but
       * avcodec_open2 will fail if it is set null.  To allow users to set
       * options easily prior to opening the AVCodecContext, we stash the
       * value we had been using, and we check after the open call and log
       * an error if it changed for some reason.
       */
      AVCodec* cachedCodec = mCodecContext->codec;
      mCodecContext->codec = 0;
      retval = avcodec_open2(mCodecContext, mCodec->getAVCodec(), &tmp);

      if (retval >= 0 && cachedCodec != 0 && cachedCodec != mCodecContext->codec)
      {
        VS_LOG_ERROR("When opening StreamCoder the codec was changed by FFmpeg.  This is not good");
      }
      if (retval < 0)
      {
        mCodecContext->codec = cachedCodec;
        throw std::runtime_error("could not open codec");
      }
    }
    mOpened = true;

    mNumDroppedFrames = 0;
    mSamplesCoded = mSamplesForEncoding = mLastExternallySetTimeStamp = 0;
    mFakeCurrPts = mFakeNextPts = mLastPtsEncoded = Global::NO_PTS;
    for(uint32_t i = 0; i < sizeof(mPtsBuffer)/sizeof(mPtsBuffer[0]); i++)
    {
      mPtsBuffer[i] = Global::NO_PTS;
    }

    // Do any post open initialization here.
    if (this->getCodecType() == ICodec::CODEC_TYPE_AUDIO)
    {
      int32_t frame_bytes = getAudioFrameSize() * getChannels()
          * IAudioSamples::findSampleBitDepth(
              (IAudioSamples::Format) mCodecContext->sample_fmt) / 8;
      if (frame_bytes <= 0)
        frame_bytes = AVCODEC_MAX_AUDIO_FRAME_SIZE;

      if (!mAudioFrameBuffer || mAudioFrameBuffer->getBufferSize()
          < frame_bytes)
        // Re-create it.
        mAudioFrameBuffer = IBuffer::make(this, frame_bytes);
      mBytesInFrameBuffer = 0;
    }
    if (aUnsetOptions)
    {
      MetaData* unsetOptions = dynamic_cast<MetaData*>(aUnsetOptions);
      if (!unsetOptions)
        throw std::runtime_error("really... seriously?");
      unsetOptions->copy(tmp);
    }
  }
  catch (std::bad_alloc & e)
  {
    throw e;
  }
  catch (std::exception & e)
  {
    VS_LOG_WARN("Error: %s", e.what());
    retval = -1;
  }
  if (tmp)
    av_dict_free(&tmp);
  return retval;
}

int32_t
StreamCoder::close()
{
  int32_t retval = -1;
  if (mCodecContext && mOpened)
  {
    retval = avcodec_close(mCodecContext);
    mOpened = false;
  }
  mBytesInFrameBuffer = 0;
  return retval;
}

int32_t
StreamCoder::decodeAudio(IAudioSamples *pOutSamples, IPacket *pPacket,
    int32_t startingByte)
{
  int32_t retval = -1;
  AudioSamples *samples = dynamic_cast<AudioSamples*> (pOutSamples);
  Packet* packet = dynamic_cast<Packet*> (pPacket);

  if (samples)
    // reset the samples
    samples->setComplete(false, 0, getSampleRate(), getChannels(),
        (IAudioSamples::Format) mCodecContext->sample_fmt, Global::NO_PTS);
  if (!samples) {
    VS_LOG_WARN("Attempting to decode when not ready; no samples");
    return retval;
  }
  if (!packet) {
    VS_LOG_WARN("Attempting to decode when not ready; no packet");
    return retval;
  }
  if (!mOpened) {
    VS_LOG_WARN("Attempting to decode when not ready; codec not opened");
    return retval;
  }
  if (!mCodecContext) {
    VS_LOG_WARN("Attempting to decode when not ready; internal context not allocated");
    return retval;
  }
  if (mDirection != DECODING) {
    VS_LOG_WARN("Attempting to decode when not ready; StreamCoder is set to encode, not decode");
    return retval;
  }
  if (!mCodec || !mCodec->canDecode()) {
    VS_LOG_WARN("Attempting to decode when not ready; codec set cannot decode");
    return retval;
  }
  if (getCodecType() != ICodec::CODEC_TYPE_AUDIO) {
    VS_LOG_WARN("Attempting to decode when not ready; codec set is not an audio codec");
    return retval;
  }


  int outBufSize = 0;
  int32_t inBufSize = 0;

  // When decoding with FFMPEG, ffmpeg needs the sample buffer
  // to be at least this long.
  samples->ensureCapacity(AVCODEC_MAX_AUDIO_FRAME_SIZE);
  outBufSize = samples->getMaxBufferSize();
  inBufSize = packet->getSize() - startingByte;

  if (inBufSize > 0 && outBufSize > 0)
  {
    RefPointer<IBuffer> buffer = packet->getData();
    uint8_t * inBuf = 0;
    int16_t * outBuf = 0;

    VS_ASSERT(buffer, "no buffer in packet!");
    if (buffer)
      inBuf = (uint8_t*) buffer->getBytes(startingByte, inBufSize);

    outBuf = samples->getRawSamples(0);

    VS_ASSERT(inBuf, "no in buffer");
    VS_ASSERT(outBuf, "no out buffer");
    if (outBuf && inBuf)
    {
      VS_LOG_TRACE("Attempting decodeAudio(%p, %p, %d, %p, %d);",
          mCodecContext,
          outBuf,
          outBufSize,
          inBuf,
          inBufSize);
      AVPacket pkt;
      av_init_packet(&pkt);
      if (packet && packet->getAVPacket())
        pkt = *packet->getAVPacket();
      // copy in our buffer
      pkt.data = inBuf;
      pkt.size = inBufSize;

      mCodecContext->reordered_opaque = packet->getPts();

      {
        AVFrame frame;
        int got_frame = 0;

        avcodec_get_frame_defaults(&frame);

        retval = avcodec_decode_audio4(mCodecContext, &frame, &got_frame, &pkt);
        // the API for decoding audio changed ot support planar audio and we
        // need to back-port
        if (retval >= 0 && got_frame) {
          int ch, plane_size;
          int planar = av_sample_fmt_is_planar(mCodecContext->sample_fmt);
          int data_size = av_samples_get_buffer_size(&plane_size,
            mCodecContext->channels,
            frame.nb_samples,
            mCodecContext->sample_fmt,
            1);
          if (outBufSize < data_size) {
            VS_LOG_ERROR("Output buffer is not large enough; no audio actually returned");
            outBufSize = 0;
          } else {
            memcpy(outBuf, frame.extended_data[0], plane_size);
            if (planar && mCodecContext->channels > 1) {
              uint8_t *out = ((uint8_t*)outBuf)+plane_size;
              for(ch = 1; ch < mCodecContext->channels; ch++) {
                memcpy(out, frame.extended_data[ch], plane_size);
                out += plane_size;
              }
            }
            outBufSize = data_size;
          }
        }
      }
      VS_LOG_TRACE("Finished %d decodeAudio(%p, %p, %d, %p, %d);",
          retval,
          mCodecContext,
          outBuf,
          outBufSize,
          inBuf,
          inBufSize);
    }
    if (retval >= 0)
    {
      // outBufSize is an In-Out parameter
      if (outBufSize < 0)
        // this can happen for some MPEG decoders
        outBufSize = 0;

      IAudioSamples::Format format =
          (IAudioSamples::Format) mCodecContext->sample_fmt;
      int32_t bytesPerSample = (IAudioSamples::findSampleBitDepth(format) / 8
          * getChannels());
      int32_t numSamples = outBufSize / bytesPerSample;

      // The audio decoder doesn't set a PTS, so we need to manufacture one.
      RefPointer<IRational> timeBase =
          this->mStream ? this->mStream->getTimeBase() : 0;
      if (!timeBase)
        timeBase = this->getTimeBase();

      int64_t packetTs = packet->getPts();
      if (packetTs == Global::NO_PTS)
        packetTs = packet->getDts();

      if (packetTs == Global::NO_PTS && mFakeNextPts == Global::NO_PTS)
      {
        // the container doesn't have time stamps; assume we start
        // at zero
        VS_LOG_TRACE("Setting fake pts to 0");
        mFakeNextPts = 0;
      }
      if (packetTs != Global::NO_PTS)
      {
        // The packet had a valid stream, and a valid time base
        if (timeBase->getNumerator() != 0 && timeBase->getDenominator() != 0)
        {
          int64_t tsDelta = Global::NO_PTS;
          if (mFakeNextPts != Global::NO_PTS)
          {
            int64_t fakeTsInStreamTimeBase = Global::NO_PTS;
            // rescale our fake into the time base of stream
            fakeTsInStreamTimeBase = timeBase->rescale(mFakeNextPts,
                mFakePtsTimeBase.value());
            tsDelta = fakeTsInStreamTimeBase - packetTs;
          }

          // now, compare it to our internal value;  if our internally calculated value
          // is within 1 tick of the packet's time stamp (in the packet's time base),
          // then we're probably right;
          // otherwise, we should reset the stream's fake time stamp based on this
          // packet
          if (mFakeNextPts != Global::NO_PTS && (tsDelta >= -1 && tsDelta
              <= 1))
          {
            // we're the right value; keep our fake next pts
            VS_LOG_TRACE("Keeping mFakeNextPts: %lld", mFakeNextPts);
          }
          else
          {
            // rescale to our internal timebase
            int64_t packetTsInMicroseconds = mFakePtsTimeBase->rescale(
                packetTs, timeBase.value());
            VS_LOG_TRACE("%p Gap in audio (%lld); Resetting calculated ts from %lld to %lld",
                this,
                tsDelta,
                mFakeNextPts,
                packetTsInMicroseconds);
            mLastExternallySetTimeStamp = packetTsInMicroseconds;
            mSamplesCoded = 0;
            mFakeNextPts = mLastExternallySetTimeStamp;
          }
        }
      }
      // Use the last value of the next pts
      mFakeCurrPts = mFakeNextPts;
      // adjust our next Pts pointer
      if (numSamples > 0)
      {
        mSamplesCoded += numSamples;
        mFakeNextPts = mLastExternallySetTimeStamp
            + IAudioSamples::samplesToDefaultPts(mSamplesCoded,
                getSampleRate());
      }

      // copy the packet PTS
      samples->setComplete(numSamples > 0, numSamples, getSampleRate(),
          getChannels(), format, mFakeCurrPts);
    }
  }

  return retval;
}

int32_t
StreamCoder::decodeVideo(IVideoPicture *pOutFrame, IPacket *pPacket,
    int32_t byteOffset)
{
  int32_t retval = -1;
  VideoPicture* frame = dynamic_cast<VideoPicture*> (pOutFrame);
  Packet* packet = dynamic_cast<Packet*> (pPacket);
  if (frame)
    // reset the frame
    frame->setComplete(false, IPixelFormat::NONE, -1, -1, mFakeCurrPts);

  if (!frame) {
    VS_LOG_WARN("Attempting to decode when not ready; no frame");
    return retval;
  }
  if (!packet) {
    VS_LOG_WARN("Attempting to decode when not ready; no packet");
    return retval;
  }
  if (!mOpened) {
    VS_LOG_WARN("Attempting to decode when not ready; codec not opened");
    return retval;
  }
  if (!mCodecContext) {
    VS_LOG_WARN("Attempting to decode when not ready; internal context not allocated");
    return retval;
  }
  if (mDirection != DECODING) {
    VS_LOG_WARN("Attempting to decode when not ready; StreamCoder is set to encode, not decode");
    return retval;
  }
  if (!mCodec || !mCodec->canDecode()) {
    VS_LOG_WARN("Attempting to decode when not ready; codec set cannot decode");
    return retval;
  }
  if (getCodecType() != ICodec::CODEC_TYPE_VIDEO) {
    VS_LOG_WARN("Attempting to decode when not ready; codec set is not a video codec");
    return retval;
  }

  AVFrame *avFrame = avcodec_alloc_frame();
  if (avFrame)
  {
    RefPointer<IBuffer> buffer = packet->getData();
    int frameFinished = 0;
    int32_t inBufSize = 0;
    uint8_t * inBuf = 0;

    inBufSize = packet->getSize() - byteOffset;

    VS_ASSERT(buffer, "no buffer in packet?");
    if (buffer)
      inBuf = (uint8_t*) buffer->getBytes(byteOffset, inBufSize);

    VS_ASSERT(inBuf, "incorrect size or no data in packet");

    if (inBufSize > 0 && inBuf)
    {
      VS_LOG_TRACE("Attempting decodeVideo(%p, %p, %d, %p, %d);",
          mCodecContext,
          avFrame,
          frameFinished,
          inBuf,
          inBufSize);
      AVPacket pkt;
      av_init_packet(&pkt);
      if (packet && packet->getAVPacket())
        pkt = *packet->getAVPacket();
      // copy in our buffer
      pkt.data = inBuf;
      pkt.size = inBufSize;

      mCodecContext->reordered_opaque = packet->getPts();
      retval = avcodec_decode_video2(mCodecContext, avFrame, &frameFinished,
          &pkt);
      VS_LOG_TRACE("Finished %d decodeVideo(%p, %p, %d, %p, %d);",
          retval,
          mCodecContext,
          avFrame,
          frameFinished,
          inBuf,
          inBufSize);
      if (frameFinished)
      {
        // copy FFMPEG's buffer into our buffer; don't try to get efficient
        // and reuse the buffer FFMPEG is using; in order to allow our
        // buffers to be thread safe, we must do a copy here.
        frame->copyAVFrame(avFrame, getPixelType(), getWidth(), getHeight());
        RefPointer<IRational> timeBase = 0;
        timeBase = this->mStream ? this->mStream->getTimeBase() : 0;
        if (!timeBase)
          timeBase = this->getTimeBase();

        int64_t packetTs = avFrame->reordered_opaque;
        // if none, assume this packet's decode time, since
        // it's presentation time should have been in reordered_opaque
        if (packetTs == Global::NO_PTS)
          packetTs = packet->getDts();

        if (packetTs != Global::NO_PTS)
        {
          if (timeBase->getNumerator() != 0)
          {
            // The decoder set a PTS, so we let it override us
            int64_t nextPts = mFakePtsTimeBase->rescale(packetTs,
                timeBase.value());
            // some youtube videos incorrectly return a packet
            // with the wrong re-ordered opaque setting.  this
            // detects that and uses the PTS from the packet instead.
            // See: http://code.google.com/p/xuggle/issues/detail?id=165
            // in this way we enforce that timestamps are always
            // increasing
            if (nextPts < mFakeNextPts && packet->getPts() != Global::NO_PTS)
              nextPts = mFakePtsTimeBase->rescale(packet->getPts(),
                  timeBase.value());
            mFakeNextPts = nextPts;
          }
        }

        // Use the last value of the next pts
        mFakeCurrPts = mFakeNextPts;
        double frameDelay = av_rescale(timeBase->getNumerator(),
            AV_TIME_BASE, timeBase->getDenominator());
        frameDelay += avFrame->repeat_pict * (frameDelay * 0.5);

        // adjust our next Pts pointer
        mFakeNextPts += (int64_t) frameDelay;
        //          VS_LOG_DEBUG("frame complete: %s; pts: %lld; packet ts: %lld; opaque ts: %lld; tb: %ld/%ld",
        //              (frameFinished ? "yes" : "no"),
        //              mFakeCurrPts,
        //              (packet ? packet->getDts() : 0),
        //              packetTs,
        //              timeBase->getNumerator(),
        //              timeBase->getDenominator()
        //          );
      }
      frame->setComplete(frameFinished, this->getPixelType(),
          this->getWidth(), this->getHeight(), mFakeCurrPts);

    }
    av_free(avFrame);
  }

  return retval;
}

int32_t
StreamCoder::encodeVideo(IPacket *pOutPacket, IVideoPicture *pFrame,
    int32_t suggestedBufferSize)
{
  int32_t retval = -1;
  VideoPicture *frame = dynamic_cast<VideoPicture*> (pFrame);
  Packet *packet = dynamic_cast<Packet*> (pOutPacket);
  RefPointer<IBuffer> encodingBuffer;

  try
  {
    if (packet)
      packet->reset();

    if (getCodecType() != ICodec::CODEC_TYPE_VIDEO)
      throw std::runtime_error(
          "Attempting to encode video with non video coder");

    if (frame && frame->getPixelType() != this->getPixelType())
      throw std::runtime_error(
          "picture is not of the same PixelType as this Coder expected");
    if (frame && frame->getWidth() != this->getWidth())
      throw std::runtime_error("picture is not of the same width as this Coder");
    if (frame && frame->getHeight() != this->getHeight())
      throw std::runtime_error(
          "picture is not of the same height as this Coder");

    if (mDirection != ENCODING)
      throw std::runtime_error("Decoding StreamCoder not valid for encoding");
    if (!mCodec)
      throw std::runtime_error("Codec not set");
    if (!mCodec->canEncode())
      throw std::runtime_error("Codec cannot be used to encode");
    if (mCodecContext && mOpened && mDirection == ENCODING && packet)
    {
      uint8_t* buf = 0;
      uint32_t bufLen = 0;

      // First, get the right buffer size.
      if (suggestedBufferSize <= 0)
      {
        if (frame)
        {
          suggestedBufferSize = frame->getSize();
        }
        else
        {
          suggestedBufferSize = avpicture_get_size(
              (PixelFormat) getPixelType(), getWidth(), getHeight());
        }
      }
      VS_ASSERT(suggestedBufferSize> 0, "no buffer size in input frame");
      suggestedBufferSize = FFMAX(suggestedBufferSize, FF_MIN_BUFFER_SIZE);

      retval = packet->allocateNewPayload(suggestedBufferSize);
      if (retval >= 0)
      {
        encodingBuffer = packet->getData();
      }
      if (encodingBuffer)
      {
        buf = (uint8_t*) encodingBuffer->getBytes(0, suggestedBufferSize);
        bufLen = encodingBuffer->getBufferSize();
      }

      if (buf && bufLen)
      {
        // Change the PTS in our frame to the timebase of the encoded stream
        RefPointer<IRational> thisTimeBase = getTimeBase();

        /*
         * We make a copy of the AVFrame object and explicitly copy
         * over the values that we know encoding cares about.
         *
         * This is because often some programs decode into an VideoPicture
         * and just want to pass that to encodeVideo.  If we just
         * leave the values that Ffmpeg had in the AVFrame during
         * decoding, strange errors can result.
         *
         * Plus, this forces us to KNOW what we're passing into the encoder.
         */
        AVFrame* avFrame = 0;
        bool dropFrame = false;
        if (frame)
        {
          avFrame = avcodec_alloc_frame();
          if (!avFrame)
            throw std::bad_alloc();
          frame->fillAVFrame(avFrame);

          // convert into the time base that this coder wants
          // to output in
          int64_t codecTimeBasePts = thisTimeBase->rescale(frame->getPts(),
              mFakePtsTimeBase.value(), IRational::ROUND_DOWN);
          if (mLastPtsEncoded != Global::NO_PTS)
          {
            // adjust for rounding;
            // fixes http://code.google.com/p/xuggle/issues/detail?id=180
            if (codecTimeBasePts < mLastPtsEncoded)
            {
              VS_LOG_TRACE(
                  "Dropping frame with timestamp %lld (if coder supports higher time-base use that instead)",
                  frame->getPts());
              dropFrame = true;
            }
            else if (codecTimeBasePts == mLastPtsEncoded)
            {
              // else we're close enough; increment by 1
              ++codecTimeBasePts;
            }
          }
          VS_LOG_TRACE("Rescaling ts: %lld to %lld (last: %lld) (from base %d/%d to %d/%d)",
              frame->getPts(),
              codecTimeBasePts,
              mLastPtsEncoded,
              mFakePtsTimeBase->getNumerator(),
              mFakePtsTimeBase->getDenominator(),
              thisTimeBase->getNumerator(),
              thisTimeBase->getDenominator());
          avFrame->pts = codecTimeBasePts;
          if (!dropFrame)
            mLastPtsEncoded = avFrame->pts;
        }

        if (!dropFrame)
        {
          VS_LOG_TRACE("Attempting encodeVideo(%p, %p, %d, %p)",
              mCodecContext,
              buf,
              bufLen,
              avFrame);
          retval = avcodec_encode_video(mCodecContext, buf, bufLen, avFrame);
          VS_LOG_TRACE("Finished %d encodeVideo(%p, %p, %d, %p)",
              retval,
              mCodecContext,
              buf,
              bufLen,
              avFrame);
        }
        else
        {
          ++mNumDroppedFrames;
          retval = 0;
        }
        if (retval >= 0)
        {
          int64_t dts = (avFrame ? mLastPtsEncoded : mLastPtsEncoded + 1);
          int64_t duration = 1;
          if (retval > 0) {
            int32_t num = thisTimeBase->getNumerator();
            int32_t den = thisTimeBase->getDenominator();
            if (num*1000LL > den)
            {
              if (mCodecContext->coded_frame
                  && mCodecContext->coded_frame->repeat_pict)
              {
                num = num * (1+mCodecContext->coded_frame->repeat_pict);
              }
            }
            duration = av_rescale(1,
                num * (int64_t)den * mCodecContext->ticks_per_frame,
                den * (int64_t) thisTimeBase->getNumerator());

            // This will be zero if the Codec does not use b-frames;
            // although we provide space for delaying up to the
            // max H264 delay, in reality we only need delay by 1 tick.
            int32_t delay = FFMAX(mCodecContext->has_b_frames,
                !!mCodecContext->max_b_frames);
            if (mCodecContext->coded_frame
                && mCodecContext->coded_frame->pts != Global::NO_PTS)
            {
              int64_t pts = mCodecContext->coded_frame->pts;
              mPtsBuffer[0] = pts;
              int32_t i;
              // If first time through set others to some 'sensible' defaults.
              // If the first PTS is zero, this leads to starting negative
              // dts values, but FFmpeg allows that so we do too.
              for(i = 1; i < delay + 1 && mPtsBuffer[i] == Global::NO_PTS; i++)
                mPtsBuffer[i] = pts + (i-delay-1)*duration;
              // Order the PTS values in increasing order and the lowest
              // one will magically be the DTS we should use for this packet.
              for(i = 0; i < delay && mPtsBuffer[i] > mPtsBuffer[i+1]; i++)
                FFSWAP(int64_t, mPtsBuffer[i], mPtsBuffer[i+1]);
              dts = mPtsBuffer[0];
//              VS_LOG_ERROR("type: %d; output: dts: %lld; pts: %lld",
//                  mCodecContext->coded_frame->pict_type,
//                  dts,
//                  pts);
            }
          }
          setPacketParameters(
              packet,
              retval,
              // if the last packet, increment the pts encoded
              // by one
              dts,
              thisTimeBase.value(),
              (mCodecContext->coded_frame ? mCodecContext->coded_frame->key_frame
                  : 0), duration);
        }
        if (avFrame)
          av_free(avFrame);
      }
    }
    else
    {
      VS_LOG_WARN("Attempting to encode when not ready");
    }
  }
  catch (std::bad_alloc & e)
  {
    retval = -1;
    throw e;
  }
  catch (std::exception & e)
  {
    VS_LOG_WARN("Got error: %s", e.what());
    retval = -1;
  }

  return retval;
}

int32_t
StreamCoder::encodeAudio(IPacket * pOutPacket, IAudioSamples* pSamples,
    uint32_t startingSample)
{
  int32_t retval = -1;
  AudioSamples *samples = dynamic_cast<AudioSamples*> (pSamples);
  Packet *packet = dynamic_cast<Packet*> (pOutPacket);
  RefPointer<IBuffer> encodingBuffer;
  bool usingInternalFrameBuffer = false;

  try
  {
    if (mDirection != ENCODING)
      throw std::runtime_error("Decoding StreamCoder not valid for encoding");
    if (!mCodec)
      throw std::runtime_error("Codec not set");
    if (!mCodec->canEncode()) {
      std::string msg = "Codec cannot be used to encode: ";
      msg += mCodec->getName();
      throw std::runtime_error(msg);
    }
    if (!packet)
      throw std::invalid_argument("Invalid packet to encode to");
    // Zero out our packet
    packet->reset();

    if (!mCodecContext)
      throw std::runtime_error("StreamCoder not initialized properly");
    if (!mOpened)
      throw std::runtime_error("StreamCoder not open");
    if (getCodecType() != ICodec::CODEC_TYPE_AUDIO)
      throw std::runtime_error(
          "Attempting to encode audio with non audio coder");
    if (!mAudioFrameBuffer)
      throw std::runtime_error("Audio Frame Buffer not initialized");

    // First, how many bytes do we need to encode a packet?
    int32_t frameSize = 0;
    int32_t frameBytes = 0;
    int32_t availableSamples = (samples ? samples->getNumSamples()
        - startingSample : 0);
    int32_t samplesConsumed = 0;
    int16_t *avSamples = (samples ? samples->getRawSamples(startingSample) : 0);

    if (samples)
    {
      if (samples->getChannels() != getChannels())
        throw std::invalid_argument(
            "channels in sample do not match StreamCoder");
      if (samples->getSampleRate() != getSampleRate())
        throw std::invalid_argument(
            "sample rate in sample does not match StreamCoder");

      if (!samples->isComplete())
        throw std::invalid_argument("input samples are not complete");

      if (mFakeNextPts == Global::NO_PTS && samples->getTimeStamp()
          != Global::NO_PTS)
        mFakeNextPts = samples->getTimeStamp()
            + IAudioSamples::samplesToDefaultPts(startingSample,
                getSampleRate());

    }

    if (mFakeNextPts == Global::NO_PTS)
      mFakeNextPts = 0;

    if (availableSamples < 0 || (avSamples && availableSamples == 0))
      throw std::invalid_argument(
          "no bytes in buffer at specified starting sample");

    int32_t bytesPerSample = (samples ? samples->getSampleSize()
        : IAudioSamples::findSampleBitDepth(
            (IAudioSamples::Format) mCodecContext->sample_fmt) / 8
            * getChannels());

    /*
     * This gets tricky; There may be more audio samples passed to
     * us than can fit in an audio frame, or there may be less.
     *
     * If less, we need to cache for a future call.
     *
     * If more, we need to just use what's needed, and let the caller
     * know the number of samples we used.
     *
     * What happens when we exhaust all audio, but we still don't
     * have enough to decode a frame?  answer: the caller passes us
     * NULL as the pInSamples, and we just silently drop the incomplete
     * frame.
     *
     * To simplify coding here (and hence open for optimization if
     * this ends up being a bottleneck), I always copy audio samples
     * into a frame buffer, and then only encode from the frame buffer
     * in this class.
     */
    frameSize = getAudioFrameSize();
    frameBytes = frameSize * bytesPerSample;

    // More error checking
    VS_ASSERT(frameBytes <= mAudioFrameBuffer->getBufferSize(),
        "did frameSize change from open?");
    if (frameBytes > mAudioFrameBuffer->getBufferSize())
      throw std::runtime_error("not enough memory in internal frame buffer");

    VS_ASSERT(mBytesInFrameBuffer <= frameBytes,
        "did frameSize change from open?");
    if (frameBytes < mBytesInFrameBuffer)
      throw std::runtime_error(
          "too many bytes left over in internal frame buffer");

    unsigned char * frameBuffer = (unsigned char*) mAudioFrameBuffer->getBytes(
        0, frameBytes);
    if (!frameBuffer)
      throw std::runtime_error("could not get internal frame buffer");

    int32_t bytesToCopyToFrameBuffer = frameBytes - mBytesInFrameBuffer;
    bytesToCopyToFrameBuffer = FFMIN(bytesToCopyToFrameBuffer,
        availableSamples*bytesPerSample);

    if (avSamples)
    {
      if (availableSamples >= frameSize && mBytesInFrameBuffer == 0)
      {
        VS_LOG_TRACE("audioEncode: Using passed in buffer: %d, %d, %d",
            availableSamples, frameSize, mBytesInFrameBuffer);
        frameBuffer = (unsigned char*) avSamples;
        samplesConsumed = frameSize;
        usingInternalFrameBuffer = false;
      }
      else
      {
        VS_LOG_TRACE("audioEncode: Using internal buffer: %d, %d, %d",
            availableSamples, frameSize, mBytesInFrameBuffer);
        memcpy(frameBuffer + mBytesInFrameBuffer, avSamples,
            bytesToCopyToFrameBuffer);
        mBytesInFrameBuffer += bytesToCopyToFrameBuffer;
        samplesConsumed = bytesToCopyToFrameBuffer / bytesPerSample;
        retval = samplesConsumed;
        usingInternalFrameBuffer = true;
      }
    }
    else
    {
      // drop everything in the frame buffer, and instead
      // just pass a null buffer to the encoder.

      // this should happen when the caller passes NULL for the
      // input samples
      frameBuffer = 0;
    }
    mSamplesForEncoding += samplesConsumed;
    VS_LOG_TRACE("Consumed %ld for total of %lld",
        samplesConsumed, mSamplesForEncoding);

    if (!frameBuffer || !usingInternalFrameBuffer || mBytesInFrameBuffer
        >= frameBytes)
    {
      // First, get the right buffer size.
      int32_t bufferSize = frameBytes;
      if (mCodecContext->codec->id == CODEC_ID_FLAC
              || mCodecContext->codec->id == CODEC_ID_VORBIS)
      {
        // FLAC & VORBIS audio for some reason gives an error if your output buffer isn't
        // over double the frame size, so we fake it here.  This could be further optimized
        // to only require an exact number, but this math is simpler and will always
        // be large enough.
        bufferSize = (64 + getAudioFrameSize() * (bytesPerSample + 1)) * 2;
      }
      VS_ASSERT(bufferSize> 0, "no buffer size in samples");
      retval = packet->allocateNewPayload(bufferSize);
      if (retval >= 0)
      {
        encodingBuffer = packet->getData();
      }

      uint8_t* buf = 0;

      if (encodingBuffer)
      {
        buf = (uint8_t*) encodingBuffer->getBytes(0, bufferSize);
      }
      if (buf && bufferSize)
      {
        VS_LOG_TRACE("Attempting encodeAudio(%p, %p, %d, %p)",
            mCodecContext,
            buf,
            bufferSize,
            frameBuffer);

        // This hack works around the fact that PCM's codecs
        // calculate samples from buffer length, and sets
        // the wrong frame size, but in
        // reality we're always passing in 2 byte samples.
        double pcmCorrection = 1.0;
        switch (mCodecContext->codec->id)
        {
        case CODEC_ID_PCM_S32LE:
        case CODEC_ID_PCM_S32BE:
        case CODEC_ID_PCM_U32LE:
        case CODEC_ID_PCM_U32BE:
          pcmCorrection = 2.0;
          break;
        case CODEC_ID_PCM_S24LE:
        case CODEC_ID_PCM_S24BE:
        case CODEC_ID_PCM_U24LE:
        case CODEC_ID_PCM_U24BE:
        case CODEC_ID_PCM_S24DAUD:
          pcmCorrection = 1.5;
          break;
        case CODEC_ID_PCM_S16LE:
        case CODEC_ID_PCM_S16BE:
        case CODEC_ID_PCM_U16LE:
        case CODEC_ID_PCM_U16BE:
          pcmCorrection = 1.0;
          break;
        case CODEC_ID_PCM_ALAW:
        case CODEC_ID_PCM_MULAW:
        case CODEC_ID_PCM_S8:
        case CODEC_ID_PCM_U8:
        case CODEC_ID_PCM_ZORK:
          pcmCorrection = 0.5;
          break;
        default:
          pcmCorrection = 1.0;
        }
        retval = avcodec_encode_audio(mCodecContext, buf, (int32_t) (bufferSize
            * pcmCorrection), (int16_t*) frameBuffer);
        VS_LOG_TRACE("Finished %d encodeAudio(%p, %p, %d, %p)",
            retval,
            mCodecContext,
            buf,
            bufferSize,
            frameBuffer);
        // regardless of what happened, nuke any data in our frame
        // buffer.
        mBytesInFrameBuffer = 0;
        if (retval >= 0)
        {
          // and only do this if a packet is returned
          if (retval > 0)
          {
            mSamplesCoded += frameSize;

            // let's check to see if the time stamp of passed in
            // samples (if any) are within tolerance of our expected
            // time stamp.  if not, this is most likely happening
            // because the IStreamCoder's data source lost a packet.
            // We will adjust our starting time stamps then for this new
            // packet
            mFakeCurrPts = mFakeNextPts;
            if (samples && samples->getTimeStamp() != Global::NO_PTS)
            {
              int64_t samplesTs = samples->getTimeStamp()
                  + IAudioSamples::samplesToDefaultPts(startingSample
                      + samplesConsumed, getSampleRate());
              int64_t samplesCached = mSamplesForEncoding - mSamplesCoded;
              int64_t tsDelta = IAudioSamples::samplesToDefaultPts(
                  samplesCached, getSampleRate());
              int64_t gap = samplesTs - (mFakeNextPts + tsDelta);

              // ignore negative gaps; some containers like WMV
              // don't actually set the right time stamps, and we
              // can assume a negative gap can't happen
              if ((gap > 1))
              {
                VS_LOG_TRACE("reset;"
                    "samplesTs:%lld;"
                    "samplesConsumed:%ld;"
                    "startingSample:%lu;"
                    "samplesForEncoding:%lld;"
                    "samplesCoded:%lld;"
                    "samplesCached:%lld;"
                    "tsDelta:%lld;"
                    "fakeNextPts:%lld;"
                    "gap:%lld;"
                    "lastExternallySetTs:%lld;"
                    "new fakeNextPts:%lld;",
                    samplesTs,
                    samplesConsumed,
                    startingSample,
                    mSamplesForEncoding,
                    mSamplesCoded,
                    samplesCached,
                    tsDelta,
                    mFakeNextPts,
                    gap,
                    mLastExternallySetTimeStamp,
                    samplesTs - tsDelta);
                mLastExternallySetTimeStamp = samplesTs - tsDelta;
                mSamplesCoded = 0;
                mSamplesForEncoding = samplesCached;
              }
            }
            mFakeNextPts = mLastExternallySetTimeStamp
                + IAudioSamples::samplesToDefaultPts(mSamplesCoded,
                    getSampleRate());

          }
          RefPointer<IRational> thisTimeBase = getTimeBase();
          int64_t ts;
          int64_t duration = IAudioSamples::samplesToDefaultPts(frameSize,
              getSampleRate());
          if (!thisTimeBase)
          {
            thisTimeBase.reset(mFakePtsTimeBase.value(), true);
            ts = mFakeCurrPts;
          }
          else
          {
            ts = thisTimeBase->rescale(mFakeCurrPts, mFakePtsTimeBase.value());
            duration
                = thisTimeBase->rescale(duration, mFakePtsTimeBase.value());
          }
          setPacketParameters(packet, retval, ts, thisTimeBase.value(), true,
              duration);

          retval = samplesConsumed;
        }
        else
        {
          throw std::runtime_error("avcodec_encode_audio failed");
        }
      }
    }
  }
  catch (std::bad_alloc & e)
  {
    throw e;
  }
  catch (std::exception& e)
  {
    VS_LOG_WARN("error: %s", e.what());
    retval = -1;
  }

  return retval;
}

void
StreamCoder::setPacketParameters(Packet * packet, int32_t size, int64_t dts,
    IRational *timebase, bool keyframe, int64_t duration)
{
  packet->setDuration(duration);

  //  VS_LOG_DEBUG("input:  dts: %lld; pts: %lld",
  //      dts,
  //      mCodecContext->coded_frame ? mCodecContext->coded_frame->pts : Global::NO_PTS);


  int64_t pts = dts;

  if (mCodecContext->coded_frame && mCodecContext->coded_frame->pts
      != Global::NO_PTS)
  {
    RefPointer<IRational> coderBase = getTimeBase();
    pts = timebase->rescale(mCodecContext->coded_frame->pts, coderBase.value());
  }
  if (pts == Global::NO_PTS)
    pts = dts;

  if (pts != Global::NO_PTS && (dts == Global::NO_PTS || dts > pts))
    // if our pts is earlier than what we think our decode time stamp should
    // be, well, then adjust the decode timestamp.  We should ALWAYS
    // honor PTS if set
    dts = pts;

  packet->setKeyPacket(keyframe);
  packet->setPts(pts);
  packet->setDts(dts);
  packet->setStreamIndex(-1);
  packet->setTimeBase(timebase);
  // We will sometimes encode some data, but have zero data to send.
  // in that case, mark the packet as incomplete so people don't
  // output it.
  packet->setComplete(size > 0, size);

  if (mStream)
  {
    packet->setStreamIndex(mStream->getIndex());
    //    VS_LOG_DEBUG("use AutomaticallyStampPacketsForStream: %d", mAutomaticallyStampPacketsForStream);

    if (mAutomaticallyStampPacketsForStream)
      mStream->stampOutputPacket(packet);
  }

  VS_LOG_TRACE("Encoded packet; size: %d; pts: %lld", size, pts);
}

int32_t
StreamCoder::getAudioFrameSize()
{
  int32_t retval = 0;
  if (mCodec && mCodec->getType() == ICodec::CODEC_TYPE_AUDIO)
  {
    if (mCodecContext->frame_size <= 1)
    {
      // Rats; some PCM encoders give a frame size of 1, which is too
      //small.  We pick a more sensible value.
      retval = getDefaultAudioFrameSize();
    }
    else
    {
      retval = mCodecContext->frame_size;
    }
  }
  return retval;
}

int32_t
StreamCoder::streamClosed(Stream*stream)
{
  int32_t retval = 0;
  if (stream == mStream)
  {
    reset();
  }
  return retval;
}

// For ref-count debugging purposes.
int32_t
StreamCoder::acquire()
{
  int32_t retval = 0;
  retval = RefCounted::acquire();
  VS_LOG_TRACE("Acquired %p: %d", this, retval);
  return retval;
}

int32_t
StreamCoder::release()
{
  int32_t retval = 0;
  retval = RefCounted::release();
  VS_LOG_TRACE("Released %p: %d", this, retval);
  return retval;
}

int32_t
StreamCoder::getCodecTag()
{
  return (mCodecContext ? mCodecContext->codec_tag : 0);
}

void
StreamCoder::setCodecTag(int32_t tag)
{
  if (mCodecContext)
    mCodecContext->codec_tag = tag;
}

int32_t
StreamCoder::getNumProperties()
{
  return Property::getNumProperties(mCodecContext);
}

IProperty*
StreamCoder::getPropertyMetaData(int32_t propertyNo)
{
  return Property::getPropertyMetaData(mCodecContext, propertyNo);
}

IProperty*
StreamCoder::getPropertyMetaData(const char *name)
{
  return Property::getPropertyMetaData(mCodecContext, name);
}

int32_t
StreamCoder :: setProperty(IMetaData* valuesToSet, IMetaData* valuesNotFound)
{
  return Property::setProperty(mCodecContext, valuesToSet, valuesNotFound);
}


int32_t
StreamCoder::setProperty(const char* aName, const char *aValue)
{
  return Property::setProperty(mCodecContext, aName, aValue);
}

int32_t
StreamCoder::setProperty(const char* aName, double aValue)
{
  return Property::setProperty(mCodecContext, aName, aValue);
}

int32_t
StreamCoder::setProperty(const char* aName, int64_t aValue)
{
  return Property::setProperty(mCodecContext, aName, aValue);
}

int32_t
StreamCoder::setProperty(const char* aName, bool aValue)
{
  return Property::setProperty(mCodecContext, aName, aValue);
}

int32_t
StreamCoder::setProperty(const char* aName, IRational *aValue)
{
  return Property::setProperty(mCodecContext, aName, aValue);
}

char*
StreamCoder::getPropertyAsString(const char *aName)
{
  return Property::getPropertyAsString(mCodecContext, aName);
}

double
StreamCoder::getPropertyAsDouble(const char *aName)
{
  return Property::getPropertyAsDouble(mCodecContext, aName);
}

int64_t
StreamCoder::getPropertyAsLong(const char *aName)
{
  return Property::getPropertyAsLong(mCodecContext, aName);
}

IRational*
StreamCoder::getPropertyAsRational(const char *aName)
{
  return Property::getPropertyAsRational(mCodecContext, aName);
}

bool
StreamCoder::getPropertyAsBoolean(const char *aName)
{
  return Property::getPropertyAsBoolean(mCodecContext, aName);
}

bool
StreamCoder::isOpen()
{
  return mOpened;
}

int32_t
StreamCoder::getDefaultAudioFrameSize()
{
  return mDefaultAudioFrameSize;
}

void
StreamCoder::setDefaultAudioFrameSize(int32_t aNewSize)
{
  if (aNewSize > 0)
    mDefaultAudioFrameSize = aNewSize;
}

int32_t
StreamCoder::setStream(Stream* stream, bool assumeOnlyStream)
{
  int32_t retval = -1;
  if (assumeOnlyStream)
    mStream = stream;
  AVStream *avStream = stream ? stream->getAVStream() : 0;
  if (avStream)
  {
    // This handles the case where FFMPEG actually alloced a stream
    // codeccontext and thinks it'll free it later when the input
    // file closes.  in this case, we free the old value because
    // we're about to overwrite it.
    if (avStream->codec) {
      resetOptions(avStream->codec);
      av_freep(&avStream->codec->extradata);
      av_freep(&avStream->codec->subtitle_header);
      av_free(avStream->codec);
    }
    avStream->codec = mCodecContext;
  }
  retval = 0;
  return retval;
}

int64_t
StreamCoder::getNumDroppedFrames()
{
  return mNumDroppedFrames;
}

void
StreamCoder::setAutomaticallyStampPacketsForStream(bool value)
{
  //  VS_LOG_DEBUG("setAutomaticallyStampPacketsForStream: %d", value);
  mAutomaticallyStampPacketsForStream = value;
}

bool
StreamCoder::getAutomaticallyStampPacketsForStream()
{
  return mAutomaticallyStampPacketsForStream;
}

int32_t
StreamCoder::setExtraData(com::xuggle::ferry::IBuffer* src, int32_t offset,
    int32_t numBytes, bool allocNew)
{
  if (!mCodecContext || !src)
    return -1;

  void* bytes = src->getBytes(offset, numBytes);
  if (!bytes)
    return -1;

  if (mCodecContext->extradata_size < numBytes || !mCodecContext->extradata)
  {
    if (allocNew)
    {
      av_free(mCodecContext->extradata);
      mCodecContext->extradata_size = 0;
      mCodecContext->extradata = (uint8_t*) av_malloc(numBytes
          + FF_INPUT_BUFFER_PADDING_SIZE);
      if (!mCodecContext->extradata)
      {
        return -1;
      }
      mCodecContext->extradata_size = numBytes;
    }
    else
      return -1;
  }
  memcpy(mCodecContext->extradata, bytes, numBytes);
  return numBytes;
}
int32_t
StreamCoder::getExtraData(com::xuggle::ferry::IBuffer *dest, int32_t offset,
    int32_t maxBytesToCopy)
{
  if (!mCodecContext || !mCodecContext->extradata
      || mCodecContext->extradata_size <= 0 || !dest || offset < 0
      || maxBytesToCopy < 0 || dest->getSize() < offset + maxBytesToCopy)
    return 0;

  int32_t bytesToCopy = FFMIN(maxBytesToCopy, mCodecContext->extradata_size);
  if (bytesToCopy <= 0)
    return 0;
  void* bytes = dest->getBytes(offset, bytesToCopy);
  if (!bytes)
    return 0;
  memcpy(bytes, mCodecContext->extradata, bytesToCopy);
  return bytesToCopy;
}

int32_t
StreamCoder::getExtraDataSize()
{
  if (mCodecContext)
    return mCodecContext->extradata_size;
  return 0;
}

IStreamCoder::CodecStandardsCompliance
StreamCoder :: getStandardsCompliance()
{
  if (mCodecContext)
    return (CodecStandardsCompliance) mCodecContext->strict_std_compliance;
  return (CodecStandardsCompliance)0;
}

int32_t
StreamCoder :: setStandardsCompliance(CodecStandardsCompliance compliance)
{
  if (!mCodecContext)
    return -1;
  mCodecContext->strict_std_compliance = compliance;
  return 0;
}
}
}
}
