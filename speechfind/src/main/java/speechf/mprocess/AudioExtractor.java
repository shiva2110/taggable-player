package speechf.mprocess;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import speechf.main.AudioInfo;

import com.xuggle.xuggler.IAudioResampler;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.ICodec.ID;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IContainer.Type;
import com.xuggle.xuggler.io.XugglerIO;

public class AudioExtractor {
	private IContainer container;
	public IStreamCoder audioCoder;
	private IPacket packet;
	private int audioStreamIndex;

	// Optional re-sampling params
	private boolean reSampling = false;
	private AudioInfo reSampleAudioInfo;
	private IAudioResampler audioResampler;

	// maintains the info about audio extracted
	private AudioInfo audioInfo; 

	public AudioExtractor(String url) throws IOException{
		init(url);
	}

	public void init(String url) throws IOException{
		container = IContainer.make();
		if(container.open(url, Type.READ, null) < 0) {
			throw new IOException("Could not open input stream for media processing");
		}

		boolean audioCodecFound = false;

		int numStreams = container.getNumStreams();
		//Get the audio coder
		for(int i = 0; i < numStreams; i++)	{
			IStream stream = container.getStream(i);
			audioCoder = stream.getStreamCoder();
			if(audioCoder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO){
				audioCodecFound = true;
				audioStreamIndex = i;
				break;
			}
		}

		if(!audioCodecFound){
			throw new IOException("No audio signal found in the media stream");
		}

		if (audioCoder.open() < 0)
			throw new RuntimeException("could not open audio decoder");

		packet =  IPacket.make();

		audioInfo = new AudioInfo();
		
		audioInfo.setChannels(audioCoder.getChannels());
		audioInfo.setSamplingRate(audioCoder.getSampleRate());		
		audioInfo.setAudioFrameSize(audioCoder.getAudioFrameSize());
		audioInfo.setBitRate(audioCoder.getBitRate());
		
		System.out.println(audioInfo.toString());
	}

	public AudioInfo getAudioInfo() {
		return audioInfo;
	}

	/**
	 * Optional audio info setting.
	 * The extra knowledge supplied with the audioInfo object will be used while extracting audio.
	 * In case of re-sampling or re-construction of audio data, this audioInfo object will be updated with appropriate changes.
	 * @param audioInfo
	 */
	public void setAudioInfo(AudioInfo audioInfo){
		this.audioInfo = audioInfo;
		this.audioInfo.setChannels(audioCoder.getChannels());
		this.audioInfo.setSamplingRate(audioCoder.getSampleRate());
	}

	/**
	 * Optional resampling setting.
	 * If set, Audio extractor will attempt to re-sample the extracted audio based on this setting.
	 * @param resamplingRate
	 */
	public void setResample(AudioInfo audioInfo){
		this.reSampling = true;
		this.reSampleAudioInfo = audioInfo;
	}

	/**
	 * Reads audio bytes of next packet.
	 * Should be called repeatedly until null is received, indicating end of audio stream.
	 * @return
	 * @throws IOException
	 */
	public List<Byte> readAudioBytes() throws IOException {
 
		while(container.readNextPacket(packet) >= 0) {
			if(packet.getStreamIndex() == audioStreamIndex) {
				List<Byte> audioBytes = new ArrayList<Byte>();
				IAudioSamples samples = IAudioSamples.make(1024, audioCoder.getChannels());
				int offset = 0;
				while(offset < packet.getSize()) {
					int bytesDecoded = audioCoder.decodeAudio(samples, packet, offset);
					if (bytesDecoded < 0)
						throw new IOException("got error decoding audio!");
					offset += bytesDecoded;
					if(samples.isComplete()){						 
						IAudioSamples outSamples = reSample(samples);
						samples = (outSamples==null)?samples:outSamples;
						byte[] rawBytes = samples.getData().getByteArray(0, samples.getSize());
						for(byte b: rawBytes){
							audioBytes.add(new Byte(b));
						}
					}
				}

				return audioBytes;
			}
		}
		return null;				
	}

	/**
	 * Re-samples audio samples based on the re-sampling setting.
	 * @param samples
	 */
	private IAudioSamples reSample(IAudioSamples samples) {
				
		if(reSampling){
			audioResampler = IAudioResampler.make(reSampleAudioInfo.getChannels(), audioInfo.getChannels(), 
					reSampleAudioInfo.getSamplingRate(), audioInfo.getSamplingRate());
			IAudioSamples outSamples = IAudioSamples.make(1024, reSampleAudioInfo.getChannels());
			audioResampler.resample(outSamples, samples, samples.getNumSamples());
			return outSamples;
		}
		return null;
	}

	public void seekToStart(){
		if(container!=null){
			container.seekKeyFrame(audioStreamIndex, 0, 0);
		}
	}

	private SourceDataLine mline ;

	public void playbackAudio(byte[] audioBytes){
		if(mline==null) {
			if(audioCoder==null) {
				throw new RuntimeException("The audio could not be played as the audio coder is not initialized," +
						" most likely there was a problem while reading the input media file stream earlier while initializing audio coder");
			}
			openAudioLine(audioCoder);
		}

		mline.write(audioBytes, 0, audioBytes.length);
		closeAudioLine();
	}

	private void openAudioLine(IStreamCoder audioCoder) {
		AudioFormat audioFormat = new AudioFormat(audioCoder.getSampleRate(),
				(int)IAudioSamples.findSampleBitDepth(audioCoder.getSampleFormat()),
				audioCoder.getChannels(),
				true, /* xuggler defaults to signed 16 bit samples */
				false);
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
		try {
			mline = (SourceDataLine) AudioSystem.getLine(info);
			mline.open(audioFormat);
			mline.start();
		} catch(LineUnavailableException e){
			throw new RuntimeException("The audio line could not be opened!");
		}	 
	}

	private void closeAudioLine() {
		if(mline!=null) {
			mline.drain();
			mline.close();
			mline = null;
		}
	}

	public void close() {
		this.container.close();
	}

	public void saveAudio(String url) throws IOException {

		// Configure output audio settings
		IPacket outPacket = IPacket.make();
		IContainer outContainer = IContainer.make();
		outContainer.open(url, Type.WRITE, null);
		IStream outStream = outContainer.addNewStream(audioStreamIndex);
		IStreamCoder outCoder = outStream.getStreamCoder();
		ICodec codec = ICodec.guessEncodingCodec(outContainer.getContainerFormat(), null, url, null, ICodec.Type.CODEC_TYPE_AUDIO);
		outCoder.setCodec(codec);
		if(this.reSampling) {
			outCoder.setSampleRate(reSampleAudioInfo.getSamplingRate());
			outCoder.setChannels(reSampleAudioInfo.getChannels());
		} else {
			outCoder.setSampleRate(audioInfo.getSamplingRate());
			outCoder.setChannels(audioInfo.getChannels());
		}
		
		outCoder.open();
		
		int ret = outContainer.writeHeader();
		if(ret<0){
			throw new RuntimeException("could not write header!");
		}

		//seeks to start of the media file.
		seekToStart();

		while(container.readNextPacket(packet) >= 0) {
			if(packet.getStreamIndex() == audioStreamIndex) {
				List<Byte> audioBytes = new ArrayList<Byte>();
				IAudioSamples samples = IAudioSamples.make(1024, audioCoder.getChannels());
				int offset = 0;
				while(offset < packet.getSize()) {
					int bytesDecoded = audioCoder.decodeAudio(samples, packet, offset);
					if (bytesDecoded < 0)
						throw new IOException("got error decoding audio!");
					offset += bytesDecoded;
					if(samples.isComplete()){						 
						IAudioSamples outSamples = reSample(samples);
						outSamples = (outSamples==null)?samples:outSamples;
						int numSamplesConsumed = 0;
						while (numSamplesConsumed < outSamples.getNumSamples()) {
							int retval = outCoder.encodeAudio(outPacket, outSamples, numSamplesConsumed);
							if (retval <= 0)
								throw new RuntimeException("Could not encode any audio: " + retval);							
							numSamplesConsumed += retval;
							if(outPacket.isComplete()) {
								retval = outContainer.writePacket(outPacket);
								if (retval < 0)
									throw new RuntimeException("Could not write packets to output container! " + retval);	
							}
						}
					}
				}

			}
		}
		
		ret = outContainer.writeTrailer();
		if(ret<0){
			throw new RuntimeException("could not write trailer!");
		}
		
		outContainer.close();
	}
}