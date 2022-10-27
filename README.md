# positions-solr-highlighter
Solr Plugin that enables to store term position in highlighted snippets

compile with ./gradlew build

put position-highlighter-1.0.jar in a folder

Add the following to solrconfig.xml
```xml
<lib path="<path to jar>/position-highlighter-1.0.jar" />
```

modify highlight searchComponent and add class="org.apache.solr.highlight.PositionsSolrHighlighter"
```xml
      <searchComponent class="solr.HighlightComponent" name="highlight">
        <highlighting class="org.apache.solr.highlight.PositionsSolrHighlighter">
          <!-- Configure the standard fragmenter -->
          <!-- This could most likely be commented out in the "default" case -->
          <fragmenter name="gap"
                  default="true"
                  class="solr.highlight.GapFragmenter">
            <lst name="defaults">
              <int name="hl.fragsize">100</int>
            </lst>
          </fragmenter>
```
