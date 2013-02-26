/*
 * 
 * Copyright 1999-2004 Carnegie Mellon University.  
 * Portions Copyright 2004 Sun Microsystems, Inc.  
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.decoder;

import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Integer;
import edu.cmu.sphinx.decoder.search.SearchManager;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The primary decoder class */
public class Decoder extends AbstractDecoder {

    /** The property for the number of features to recognize at once. */
    @S4Integer(defaultValue = 100000)
    public final static String PROP_FEATURE_BLOCK_SIZE = "featureBlockSize";
    private int featureBlockSize;
    private static final Logger logger = LoggerFactory.getLogger(Decoder.class);
    public Decoder() {

    }
    
    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        featureBlockSize = ps.getInt(PROP_FEATURE_BLOCK_SIZE);
    }

    /**
     *
     * @param searchManager
     * @param fireNonFinalResults
     * @param autoAllocate
     * @param resultListeners
     * @param featureBlockSize
     */
    public Decoder( SearchManager searchManager, boolean fireNonFinalResults, boolean autoAllocate, List<ResultListener> resultListeners, int featureBlockSize) {
        super( searchManager, fireNonFinalResults, autoAllocate, resultListeners);
        this.featureBlockSize = featureBlockSize;
    }
    
    int totalFrames;
    /**
     * Decode frames until recognition is complete.
     *
     * @param referenceText the reference text (or null)
     * @return a result
     */
    @Override
    public Result decode(String referenceText) {
        searchManager.startRecognition();
        Result result;
        do {
        	
            result = searchManager.recognize(featureBlockSize);
            totalFrames = totalFrames + featureBlockSize;
            if (result != null) {
                result.setReferenceText(referenceText);
                fireResultListeners(result);
            }
            
           if(result==null) {
        	  logger.debug("Stream end has reached");
           } else if(result!=null && result.isFinal()) {
         	  logger.debug("Stream end has NOT reached, result is final");
           } else if(result!=null && !result.isFinal()) {
         	  logger.debug("Stream end has NOT reached, result is NOT final, total frames read " + totalFrames);
           }           
                       
        } while (result != null && !result.isFinal());
        searchManager.stopRecognition();
        return result;
    }
}
