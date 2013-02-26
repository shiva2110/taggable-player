package speechf.main;

import java.lang.reflect.Field;


public class AudioInfo {
	private int samplingRate;
	private int channels;
	private int audioFrameSize;
	private int bitRate;
	private double frameRate;
	
	
	
	public double getFrameRate() {
		return frameRate;
	}
	public void setFrameRate(double frameRate) {
		this.frameRate = frameRate;
	}
	public int getAudioFrameSize() {
		return audioFrameSize;
	}
	public void setAudioFrameSize(int audioFrameSize) {
		this.audioFrameSize = audioFrameSize;
	}
	public int getBitRate() {
		return bitRate;
	}
	public void setBitRate(int bitRate) {
		this.bitRate = bitRate;
	}
	public int getSamplingRate() {
		return samplingRate;
	}
	public void setSamplingRate(int samplingRate) {
		this.samplingRate = samplingRate;
	}
	public int getChannels() {
		return channels;
	}
	public void setChannels(int channels) {
		this.channels = channels;
	}
	
	public String toString() {
		Class c = this.getClass();
		Field[] fields = c.getDeclaredFields();
		
		StringBuffer sb = new StringBuffer();
		for(Field f : fields){
			try {
				sb.append(f.getName() + "=" + f.get(this).toString() + "; ");
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
		
		return sb.toString();
		
	}
}
