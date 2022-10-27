package org.apache.solr.highlight;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
//import java.io.FileWriter;
//import java.io.BufferedWriter;
//import java.util.ArrayList;
//import java.util.Collections;
import java.util.HashSet;
//import java.util.Iterator;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

//import org.apache.lucene.index.StoredDocument;
import org.apache.lucene.search.Query;
//import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
//import org.apache.lucene.search.vectorhighlight.FieldPhraseList;
//import org.apache.lucene.search.vectorhighlight.FieldQuery;
//import org.apache.lucene.search.vectorhighlight.FieldTermStack;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.PluginInfo;
//import org.apache.solr.core.SolrConfig;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Fields;
import org.apache.lucene.analysis.TokenStream;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.plugin.PluginInfoInitialized;
import org.apache.lucene.index.IndexReader;
//import org.apache.lucene.index.Terms;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.Highlighter;
//import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.TokenGroup;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
//import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;


class PosHTMLFormatter implements Formatter {
  private static final String DEFAULT_PRE_TAG = "<B>";
  private static final String DEFAULT_POST_TAG = "</B>";

  private String preTag;
  private String postTag;

  public Map<Integer, Integer> map;
  Map<Integer, String> positions;

  public PosHTMLFormatter(String preTag, String postTag) {
      this.preTag = preTag;
      this.postTag = postTag;
  }

  /** Default constructor uses HTML: &lt;B&gt; tags to markup terms. */
  public PosHTMLFormatter() {
      this(DEFAULT_PRE_TAG, DEFAULT_POST_TAG);
  }

  /* (non-Javadoc)
   * @see org.apache.lucene.search.highlight.Formatter#highlightTerm(java.lang.String, org.apache.lucene.search.highlight.TokenGroup)
   */
  public String highlightTerm(String originalText, TokenGroup tokenGroup) {
      // Se positions != null devo inviare solamente le posizioni e non tutto il testo
      if (positions != null) {
          if (tokenGroup.getTotalScore() <= 0) {
              return null;
          }
          if (map != null && map.containsKey(tokenGroup.getStartOffset())) {
              Integer pos = map.get(tokenGroup.getStartOffset());
              StringBuffer txt = new StringBuffer(originalText.length());
              txt.append(originalText);
              positions.put(pos, txt.toString());
          }
          return null;
      }
      // altrimenti invia il testo
      if (tokenGroup.getTotalScore() <= 0) {
          return originalText;
      }
      //log.debug(new ObjectMapper().writeValueAsString(tokenGroup))
      StringBuffer myPreTag;
      if (map != null && map.containsKey(tokenGroup.getStartOffset())) {
          Integer pos = map.get(tokenGroup.getStartOffset());
          Integer myPreTagLen = preTag.length() + " pos=''".length() + pos.toString().length();
          myPreTag = new StringBuffer(myPreTagLen);
          myPreTag.append(preTag.substring(0, preTag.length()-1));
          myPreTag.append(" pos=\"");
          myPreTag.append(pos.toString());
          myPreTag.append("\"");
          myPreTag.append(preTag.substring(preTag.length()-1));
      } else {
          myPreTag = new StringBuffer(preTag.length());
          myPreTag.append(preTag);
      }

      // Allocate StringBuilder with the right number of characters from the
      // beginning, to avoid char[] allocations in the middle of appends.
      StringBuilder returnBuffer =
              new StringBuilder(myPreTag.length() + originalText.length() + postTag.length());
      returnBuffer.append(myPreTag);
      returnBuffer.append(originalText);
      returnBuffer.append(postTag);
      return returnBuffer.toString();
  }
}

/**
 * <p>
 * Example configuration:
 *
 * <pre class="prettyprint">
 *   &lt;searchComponent class="solr.HighlightComponent" name="highlight"&gt;
 *     &lt;highlighting class="org.apache.solr.highlight.PositionsSolrHighlighter"/&gt;
 *   &lt;/searchComponent&gt;
 * </pre>
 * <p>
 * Notes:
 * <ul>
 * <li>fields to highlight must be configured with termVectors="true"
 * termPositions="true" termOffsets="true"
 * <li>hl.q (string) can specify the query
 * <li>hl.fl (string) specifies the field list.
 * </ul>
 */
public class PositionsSolrHighlighter extends SolrHighlighter implements
    PluginInfoInitialized {

  @Override
  public void init(PluginInfo info) {
    // TODO Auto-generated method stub

  }

  /**
   * Generates a list of Highlighted query term position(s) for each item in a
   * list of documents, or returns null if highlighting is disabled.
   *
   * @param docs query results
   * @param query the query
   * @param req the current request
   * @param defaultFields default list of fields to summarize
   *
   * @return NamedList containing a NamedList for each document, which in
   *         turns contains sets (field, positions) pairs.
   */
  @Override
  public NamedList<Object> doHighlighting(DocList docs, Query query,
      SolrQueryRequest req, String[] defaultFields) throws IOException {
    SolrParams params = req.getParams();
    if (! isHighlightingEnabled(params)) {
      return null;
    }
    // if true return only words that match with position
    boolean hlpos = params.getBool("hl.pos", false) ? true : false;
    SolrIndexSearcher searcher = req.getSearcher();
    // our own HTML formatter
    PosHTMLFormatter formatter = new PosHTMLFormatter();
    QueryScorer scorer = new QueryScorer(query);
    String[] fieldNames = getHighlightFields(query, req, defaultFields);
    Highlighter highlighter = new Highlighter(formatter, scorer);

    int[] docIDs = toDocIDs(docs);
    Set<String> fset = new HashSet<String>();

    {
      // pre-fetch documents using the Searcher's doc cache
      for (String f : fieldNames) {
        fset.add(f);
      }
      // fetch unique key if one exists.
      SchemaField keyField = searcher.getSchema().getUniqueKeyField();
      if (null != keyField) {
        fset.add(keyField.getName());
      }
    }
    // The object to be returned
    NamedList<Object> info = new SimpleOrderedMap<Object>();
    IndexSchema schema = searcher.getSchema();
    IndexReader reader = searcher.getIndexReader();
    int fragSize = params.getInt("hl.fragsize", 100);

    for (int docID : docIDs) { // cycle documents
      Document clip = searcher.doc(docID, fset);
      String printId = schema.printableUniqueKey(clip);
      NamedList<Object> fieldInfo = new SimpleOrderedMap<Object>();
      for (String field : fieldNames) { // cycle fields to be highlighted
        String body = clip.get(field);
        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, fragSize<= 0 ? body.length() : fragSize);
        highlighter.setTextFragmenter(fragmenter);
        Fields vectors = reader.getTermVectors(docID);
        if (vectors == null) {
          // if missing term vectors fall back to default UnifiedSolrHighlighter
          UnifiedSolrHighlighter hl = new UnifiedSolrHighlighter();
          return hl.doHighlighting(docs, query, req, defaultFields);
        }
        TokenStream ts = TokenSources.getTermVectorTokenStreamOrNull(field, vectors, highlighter.getMaxDocCharsToAnalyze()-1);
        if (ts == null) {
          continue;
        }
        OffsetAttribute offsets = ts.addAttribute(OffsetAttribute.class);
        if (offsets == null) {
          continue;
        }
        PositionIncrementAttribute positions = ts.addAttribute(PositionIncrementAttribute.class);
        if (positions == null) {
          continue;
        }
        ts.reset();
        // map to store offsets with positions
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        int pos = 0;
        while (ts.incrementToken()) {
          int increment = positions.getPositionIncrement();
          int start = offsets.startOffset();
          map.put(start, pos += increment);
        }
        formatter.map = map;
        if (hlpos) { // if we are to return only mathing token with positions
          // initialize positions hashmap
          formatter.positions = new HashMap<Integer, String>();
        }
        ts.reset();
        int numFragments = params.getFieldInt(field, HighlightParams.SNIPPETS, 1);
        try {
          String[] frags = highlighter.getBestFragments(ts, body, numFragments); //1000);
          if (frags.length > 0) {
            if (hlpos) {
              if (formatter.positions.size() > 0) {
                // return only tokens with positions
                fieldInfo.add(field, formatter.positions);
              }
            } else {
              if (frags.length > 0) {
                // return fragments
                fieldInfo.add(field, frags);
              }
            }
          }
          formatter.map = null;
        }
        catch (Exception e) {
          //e.printStackTrace();
          return ( null );
        }
      }
      if (fieldInfo.size() > 0) {
        info.add(printId == null ? null : printId, fieldInfo);
      }
    }

    return info;
  }

  /** Converts solr's DocList to the int[] docIDs */
  protected int[] toDocIDs(DocList docs) {
    int[] docIDs = new int[docs.size()];
    DocIterator iterator = docs.iterator();
    for (int i = 0; i < docIDs.length; i++) {
      if (!iterator.hasNext()) {
        throw new AssertionError();
      }
      int doc = iterator.nextDoc();
      docIDs[i] = doc;
    }
    if (iterator.hasNext()) {
      throw new AssertionError();
    }
    return docIDs;
  }

}