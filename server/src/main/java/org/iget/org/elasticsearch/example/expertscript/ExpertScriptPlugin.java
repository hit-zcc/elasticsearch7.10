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

package org.iget.org.elasticsearch.example.expertscript;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.index.Term;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.script.ScoreScript;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.ScriptEngine;
import org.elasticsearch.script.ScriptFactory;
import org.elasticsearch.search.lookup.SearchLookup;

/**
 * An  script plugin to limit query length.
 */
public class ExpertScriptPlugin extends Plugin implements ScriptPlugin {
    @Override
    public ScriptEngine getScriptEngine(
        Settings settings,
        Collection<ScriptContext<?>> contexts
    ) {
        return new BetaScriptEngine();
    }

    private static class BetaScriptEngine implements ScriptEngine {
        @Override
        public String getType() {
            return "beta_scripts";
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
            if ("beta_query".equals(scriptSource)) {
                ScoreScript.Factory factory = new BetaFactory();
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

        private static class BetaFactory implements ScoreScript.Factory,
            ScriptFactory {
            @Override
            public ScoreScript.LeafFactory newFactory(
                Map<String, Object> params,
                SearchLookup lookup
            ) {
                return new BetaLeafFactory(params, lookup);
            }
        }

        private static class BetaLeafFactory implements ScoreScript.LeafFactory {
            private final Map<String, Object> params;
            private final SearchLookup lookup;
            private final String multipliedItem;
            private final int factor;

            private BetaLeafFactory(
                Map<String, Object> params, SearchLookup lookup
            ) {
                if (params.containsKey("factor") == false) {
                    throw new IllegalArgumentException(
                        "Missing parameter [factor]"
                    );
                } if (params.containsKey("multipliedItem") == false) {
                    throw new IllegalArgumentException(
                        "Missing parameter [multipliedItem]"
                    );
                }
                this.params = params;
                this.lookup = lookup;
                this.factor =  Integer.parseInt(params.get("factor").toString());
                this.multipliedItem = params.get("multipliedItem").toString();
            }

            @Override
            public boolean needs_score() {
                return true;
            }


            @Override
            public ScoreScript newInstance(LeafReaderContext context)
                throws IOException {

                SortedSetDocValues docValues =context.reader().getSortedSetDocValues("id");

                return new ScoreScript(params, lookup, context) {
                    int currentDocid = -1;

                    @Override
                    public double execute(ExplanationHolder explanation) throws IOException {

                        System.out.println(docValues.lookupOrd(_getDocId()).utf8ToString());
                        return 0;
                    }
                };

        }
        }
    }
}
