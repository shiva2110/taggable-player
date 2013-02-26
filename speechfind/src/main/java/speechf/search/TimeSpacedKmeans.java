package speechf.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import speechf.main.TranscriptWord;
import speechf.main.TranscriptWordProp;

public class TimeSpacedKmeans extends Kmeans{

	private static Float defaultSeedingSpace =  new Float(1.00) ; // in sec
	
	@Override
	public List<Cluster> initializeSeeds(List<TranscriptWord> resultDocs) {
		List<Cluster> seedList = new ArrayList<Cluster>();
		Collections.sort(resultDocs);

		Float previousSeedTime = new Float(0 - defaultSeedingSpace);
		for(TranscriptWord doc : resultDocs){
			Float time = new Float(doc.getValue(TranscriptWordProp.START_TIME));
			if(time>=(previousSeedTime+defaultSeedingSpace)){
				Cluster cluster = new Cluster();
				cluster.mean = time;				
				seedList.add(cluster);
				previousSeedTime = time;
			}
		}

		return seedList;
	}

	@Override
	public void reAdjustMean(List<Cluster> clusters, List<TranscriptWord> resultDocs, Map<Integer, Integer> resultClusterMap) {
		
		for(Cluster cluster: clusters){
			cluster.mean = new Float(0);
			cluster.totalMembers = 0;
		}

		for(Integer resultId: resultClusterMap.keySet()) {
			Cluster cluster = clusters.get(resultClusterMap.get(resultId));				
			cluster.mean = cluster.mean + new Float(resultDocs.get(resultId).getValue(TranscriptWordProp.START_TIME));
			cluster.totalMembers ++ ;
		}

		for(Cluster cluster: clusters){
			cluster.mean = cluster.mean/cluster.totalMembers;
		}
		
	}

	@Override
	public Float getDist(Cluster cluster, TranscriptWord resultDoc) {
		Float clusterMean  = cluster.mean;
		Float resultStartTime = new Float(resultDoc.getValue(TranscriptWordProp.START_TIME));
		return Math.abs(clusterMean - resultStartTime);
	}

}
