/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.iget.org.elasticsearch.searchPlugin;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedSetDocValues;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.mapper.IdFieldMapper;
import org.elasticsearch.index.mapper.Uid;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.script.ScoreScript;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.ScriptEngine;
import org.elasticsearch.script.ScriptFactory;
import org.elasticsearch.search.lookup.SearchLookup;
import org.iget.org.elasticsearch.searchPlugin.Factory.DefaultParserFactory;
import org.iget.org.elasticsearch.searchPlugin.Factory.GzipBase64ParserFactory;
import org.iget.org.elasticsearch.searchPlugin.Factory.IEncodeParser;
import org.iget.org.elasticsearch.searchPlugin.Factory.IEncodeParserFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class FunctionFilterPlugin extends Plugin implements ScriptPlugin {

    private static Map<String, IEncodeParserFactory> factoryMap = new HashMap();
    static{
        factoryMap.put("None",new DefaultParserFactory());
        factoryMap.put("GzipBase64",new GzipBase64ParserFactory());
    }
    @Override
    public ScriptEngine getScriptEngine(
        Settings settings,
        Collection<ScriptContext<?>> contexts
    ) {
        return new FilterScriptEngine();
    }

    private static class FilterScriptEngine implements ScriptEngine {
        @Override
        public String getType() {
            return "filter_scripts";
        }

        @Override
        public <T> T compile(
            String scriptName,
            String scriptSource,
            ScriptContext<T> context,
            Map<String, String> params
        ) {
            if (context.equals(ScoreScript.CONTEXT) == false) {
                throw new IllegalArgumentException(getType() +
                    " scripts cannot be used for context ["
                    + context.name + "]");
            }
            // we use the script "source" as the script identifier
            if ("filter".equals(scriptSource)) {
                ScoreScript.Factory factory = new FilterScriptEngine.FilterFactory();
                return context.factoryClazz.cast(factory);
            }
            throw new IllegalArgumentException("Unknown script name "
                + scriptSource);
        }

        @Override
        public void close() {
            // optionally close resources
        }

        @Override
        public Set<ScriptContext<?>> getSupportedContexts() {
            return Collections.singleton(ScoreScript.CONTEXT);
        }

        private static class FilterFactory implements ScoreScript.Factory,
            ScriptFactory {
            @Override
            public ScoreScript.LeafFactory newFactory(
                Map<String, Object> params,
                SearchLookup lookup
            ) {
                return new FilterLeafFactory(params, lookup);
            }
        }

        private static class FilterLeafFactory implements ScoreScript.LeafFactory {
            private final Map<String, Object> params;
            private final SearchLookup lookup;
            private final String match_type;
            private final String encode;
            private final String match_filed;
            private final Object value;


            private FilterLeafFactory(
                Map<String, Object> params, SearchLookup lookup
            ) {
                if (params.containsKey("value") == false) {
                    throw new IllegalArgumentException(
                        "Missing parameter [value]"
                    );
                } if (params.containsKey("match_type") == false) {
                    throw new IllegalArgumentException(
                        "Missing parameter [match_type]"
                    );
                }
                if (params.containsKey("match_filed") == false) {
                    throw new IllegalArgumentException(
                        "Missing parameter [match_filed]"
                    );
                }
                this.encode = (String) params.getOrDefault("encode","None");
                this.params = params;
                this.lookup = lookup;
                this.match_filed =  params.get("match_filed").toString();
                this.match_type = params.get("match_type").toString();
                this.value = params.get("value");
            }

            @Override
            public boolean needs_score() {
                return false;
            }


            @Override
            public ScoreScript newInstance(LeafReaderContext context)
                throws IOException {

                SortedSetDocValues docValues =context.reader().getSortedSetDocValues(match_filed);
                return new ScoreScript(params, lookup, context) {
                    @Override
                    public double execute(ExplanationHolder explanation) throws IOException {
                        IEncodeParserFactory factory =  factoryMap.get(encode);
                        IEncodeParser parser = factory.createParser();
                        Set<String> valSet = parser.parse(value);
                        Object value;
                        //如果_id 则从上下文中取，否则从source中取得
                        if(match_filed.equals(IdFieldMapper.NAME)) {
                             value = Uid.decodeId(context.reader().document(_getDocId()).getField(IdFieldMapper.NAME).binaryValue().bytes);
                        }else{
                            if(match_filed.contains(".")){
                                int index = 0;
                                String[] topSource = match_filed.split("\\.");
                                int length = match_filed.split("\\.").length;
                                Map<String,Object> map = (Map<String, Object>) lookup.source().getOrDefault(topSource[0],Collections.emptyMap());
                                while(index<length-2){
                                    index++;
                                    map = (Map<String, Object>) map.getOrDefault(topSource[index],Collections.emptyMap());
                                }
                                value = map.get(topSource[length-1]);


                            }
                            else {
                                value = lookup.source().getOrDefault(match_filed,"") + "";
                            }

                        }
                        //lookup.sourceLookup.reader.document(_getDocId()).getField(IdFieldMapper.NAME)
                        double result = 0;
                        int factor = 0;
                        if(match_type.equals("must")){
                            factor = 1;
                        }else if(match_type.equals("must_not")){
                            factor = -1;
                        }else{
                            throw new IOException("no such match_type");
                        }
                        if (value instanceof List){
                            boolean contains = false;
                            List<String> stringList = (List<String>) ((List) value).stream().map(en->en+"").collect(Collectors.toList());
                            for (String v:stringList) {
                                if (valSet.contains(v)) {
                                    result = 1 * factor;
                                    contains = true;
                                    break;
                                }
                            }
                            if (!contains){
                                result = -1 * factor;
                            }
                        }else{
                            value = null==value?"":value+"";
                            if(valSet.contains(value)){
                                result = 1*factor;
                            }else {
                                result = -1 * factor;
                            }
                        }


                        return result==-1?0:1;
                    }
                };

            }
        }
    }
}
