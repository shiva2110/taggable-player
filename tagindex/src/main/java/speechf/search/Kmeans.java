package speechf.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import speechf.main.TranscriptWord;

public abstract class Kmeans {

	/**
	 * Initializes clusters and cluster seeds
	 * @param resultDocs
	 * @return
	 */
	public abstract List<Cluster> initializeSeeds(List<TranscriptWord> resultDocs);
	
	/**
	 * Re-adjusts mean of the cluster based on the recent state change
	 * @param clusters
	 * @param resultDocs
	 */
	public abstract void reAdjustMean(List<Cluster> clusters, List<TranscriptWord> resultDocs, Map<Integer, Integer> resultClusterMap);
	
	/**
	 * Calculates distance between cluster mean and the result
	 * @param cluster
	 * @param resultDoc
	 * @return
	 */
	public abstract Float getDist(Cluster cluster, TranscriptWord resultDoc);
	
	
	/**
	 * Performs K-means clustering
	 * @param resultDocs
	 * @return
	 */
	public List<Cluster> cluster(List<TranscriptWord> resultDocs) {
		List<Cluster> clusters = initializeSeeds(resultDocs);
		Map<Integer, Integer> resultClusterMap = new HashMap<Integer, Integer>();
		
		while(true){

			boolean stateChange = false;
			for(int i=0; i<resultDocs.size(); i++){
				int closestCluster = -1;
				float minDist = Float.MAX_VALUE;

				for(int j=0; j<clusters.size(); j++){
					
					float dist = getDist(clusters.get(j), resultDocs.get(i));
					if(dist<minDist) {
						minDist = dist;
						closestCluster = j;
					}
				}

				if(!resultClusterMap.containsKey(i) || (resultClusterMap.containsKey(i) && resultClusterMap.get(i)!=closestCluster)){
					resultClusterMap.put(i, closestCluster);
					stateChange = true;
				}				
			}

			if(!stateChange){
				break;
			}
			
			/////// re-adjust cluster mean
			reAdjustMean(clusters, resultDocs, resultClusterMap);
		}
		
		for(Integer resultId: resultClusterMap.keySet()) {
			Integer clusterId = resultClusterMap.get(resultId);
			clusters.get(clusterId).addMember(resultDocs.get(resultId));
		}
		
		return clusters;
	}
}
