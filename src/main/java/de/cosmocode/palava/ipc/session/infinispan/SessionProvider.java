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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.cosmocode.palava.concurrent.BackgroundScheduler;
import de.cosmocode.palava.core.Registry;
import de.cosmocode.palava.core.lifecycle.Disposable;
import de.cosmocode.palava.core.lifecycle.Initializable;
import de.cosmocode.palava.core.lifecycle.LifecycleException;
import de.cosmocode.palava.ipc.IpcConnection;
import de.cosmocode.palava.ipc.IpcConnectionDestroyEvent;
import de.cosmocode.palava.ipc.IpcSession;
import de.cosmocode.palava.ipc.IpcSessionProvider;
import de.cosmocode.palava.ipc.session.infinispan.Session.SessionKey;

/**
 * 
 * 
 * @author Tobias Sarnowski
 */
@Singleton
final class SessionProvider implements IpcSessionProvider, Initializable, Runnable, 
    IpcConnectionDestroyEvent, Disposable {
    
    private static final Logger LOG = LoggerFactory.getLogger(SessionProvider.class);

    private final Cache<SessionKey, Session> cache;
    
    private final ScheduledExecutorService scheduler;
    
    private final Registry registry;

    @Inject
    @SuppressWarnings("unchecked")
    public SessionProvider(@SessionCache Cache<?, ?> cache,
        @BackgroundScheduler ScheduledExecutorService scheduler,
        Registry registry) {
        this.cache = (Cache<SessionKey, Session>) Preconditions.checkNotNull(cache, "Cache");
        this.scheduler = Preconditions.checkNotNull(scheduler, "Scheduler");
        this.registry = Preconditions.checkNotNull(registry, "Registry");
    }

    @Override
    public void initialize() throws LifecycleException {
        registry.register(IpcConnectionDestroyEvent.class, this);
        scheduler.scheduleAtFixedRate(this, 1, 15, TimeUnit.MINUTES);
    }

    @Override
    public IpcSession getSession(String sessionId, String identifier) {
        Session session = cache.get(new SessionKey(sessionId, identifier));
        if (session == null) {
            session = new Session(UUID.randomUUID().toString(), identifier);
            LOG.debug("Created new session {}", session);
        }
        return session;
    }


    @Override
    public void run() {
        for (Map.Entry<SessionKey, Session> entry : cache.entrySet()) {
            final Session session = entry.getValue();
            if (session.isExpired()) {
                LOG.debug("Expiring {}...", session);
                cache.removeAsync(entry.getKey());
            }
        }
    }

    @Override
    public void eventIpcConnectionDestroy(IpcConnection connection) {
        final IpcSession ipcSession = connection.getSession();
        if (ipcSession instanceof Session) {
            final Session session = Session.class.cast(ipcSession);
            cache.put(session.getKey(), session);
        }
    }
    
    @Override
    public void dispose() throws LifecycleException {
        registry.remove(this);
    }

    @Override
    public String toString() {
        return "SessionProvider{" + "cache=" + cache + '}';
    }
    
}
