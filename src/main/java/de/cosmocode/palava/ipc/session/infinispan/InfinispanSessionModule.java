/**
 * Copyright 2010 CosmoCode GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.cosmocode.palava.ipc.session.infinispan;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;
import de.cosmocode.palava.ipc.IpcSessionProvider;
import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tobias Sarnowski
 */
public class InfinispanSessionModule implements Module {
    private static final Logger LOG = LoggerFactory.getLogger(InfinispanSessionModule.class);

    private String cacheName;


    public InfinispanSessionModule(String cacheName) {
        this.cacheName = cacheName;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(IpcSessionProvider.class).to(SessionProvider.class).asEagerSingleton();
        binder.bind(Cache.class).annotatedWith(SessionCache.class).to(Key.get(Cache.class, Names.named(cacheName)));
    }
}