/*
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

/*
* KubernetesResourceModelSource.java
* 
* User: Jean-Baptiste Guerraz <a href="mailto:jbguerraz@gmail.com">jbguerraz@gmail.com</a>
* Created: 9/22/2016 4:42 PM
* 
*/
package com.skilld.rundeck.plugin.resources.kubernetes;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.resources.ResourceModelSource;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * KubernetesResourceModelSource produces nodes by querying the Kubernetes API to list instances.
 * <p/>
 * The RunDeck node definitions are created from the instances on a mapping system to convert properties of the kubernetes
 * instances to attributes defined on the nodes.
 * <p/>
 *
 * @author Jean-Baptiste Guerraz <a href="mailto:jbguerraz@gmail.com">jbguerraz@gmail.com</a>
 */
public class KubernetesResourceModelSource implements ResourceModelSource {
    static Logger logger = Logger.getLogger(KubernetesResourceModelSource.class);
    long refreshInterval = 30000;
    long lastRefresh = 0;
    String username;
    String sshPort;
    String labelSelectors;

    INodeSet iNodeSet;
    KubernetesNodeToRundeckNodeMapper mapper;

    public KubernetesResourceModelSource(final Properties configuration) {
        username = configuration.getProperty(KubernetesResourceModelSourceFactory.USERNAME);
        sshPort = configuration.getProperty(KubernetesResourceModelSourceFactory.SSH_PORT);
        labelSelectors = configuration.getProperty(KubernetesResourceModelSourceFactory.LABEL_SELECTORS);
        int refreshSecs = 30;
        final String refreshStr = configuration.getProperty(KubernetesResourceModelSourceFactory.REFRESH_INTERVAL);
        if (null != refreshStr && !"".equals(refreshStr)) {
            try {
                refreshSecs = Integer.parseInt(refreshStr);
            } catch (NumberFormatException e) {
                logger.warn(KubernetesResourceModelSourceFactory.REFRESH_INTERVAL + " value is not valid: " + refreshStr);
            }
        }
        refreshInterval = refreshSecs * 1000;
        initialize();
    }

    private void initialize() {
        Config config = new ConfigBuilder().build();
        mapper = new KubernetesNodeToRundeckNodeMapper(config);
        mapper.setUsername(username);
	mapper.setSshPort(sshPort);
        mapper.setLabelSelectors(labelSelectors);
    }


    public synchronized INodeSet getNodes() throws ResourceModelSourceException {
        if (!needsRefresh()) {
            if (null != iNodeSet) {
                logger.info("Returning " + iNodeSet.getNodeNames().size() + " cached nodes from Kubernetes");
            }
            return iNodeSet;
        }
	iNodeSet = mapper.performQuery();
        lastRefresh = System.currentTimeMillis();
        if (null != iNodeSet) {
            logger.info("Read " + iNodeSet.getNodeNames().size() + " nodes from Kubernetes");
        }
        return iNodeSet;
    }

    /**
     * Returns true if the last refresh time was longer ago than the refresh interval
     */
    private boolean needsRefresh() {
        return refreshInterval < 0 || (System.currentTimeMillis() - lastRefresh > refreshInterval);
    }

    public void validate() throws ConfigurationException {
        File serviceAccountToken = new File(Config.KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH);
	File serviceAccountCert = new File(Config.KUBERNETES_SERVICE_ACCOUNT_CA_CRT_PATH);
        if(!serviceAccountToken.exists() || !serviceAccountCert.exists()) { 
            throw new ConfigurationException("This plugin require rundeck to runs in-cluster. Remote authentication not supported.");
        }
    }
}
