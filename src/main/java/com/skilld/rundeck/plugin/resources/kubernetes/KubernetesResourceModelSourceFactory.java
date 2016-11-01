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
* KubernetesResourceModelSourceFactory.java
* 
* User: Jean-Baptiste Guerraz <a href="mailto:jbguerraz@gmail.com">jbguerraz@gmail.com</a>
* Created: 9/22/2016 4:42 PM
* 
*/
package com.skilld.rundeck.plugin.resources.kubernetes;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.core.resources.ResourceModelSource;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceFactory;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.dtolabs.rundeck.plugins.util.PropertyBuilder;

import java.io.File;
import java.util.*;

/**
 * KubernetesResourceModelSourceFactory is the factory that can create a {@link ResourceModelSource} based on a configuration.
 * @author Jean-Baptiste Guerraz <a href="mailto:jbguerraz@gmail.com">jbguerraz@gmail.com</a>
 */
@Plugin(name = "kubernetes-nodes", service = "ResourceModelSource")
public class KubernetesResourceModelSourceFactory implements ResourceModelSourceFactory, Describable {
    private Framework framework;
    public static final String PROVIDER_NAME = "kubernetes-nodes";
    public static final String LABEL_SELECTORS = "labelSelectors";
    public static final String USERNAME = "username";
    public static final String SSH_PORT = "sshPort";
    public static final String REFRESH_INTERVAL = "refreshInterval";

    public KubernetesResourceModelSourceFactory(final Framework framework) {
        this.framework = framework;
    }

    public ResourceModelSource createResourceModelSource(final Properties properties) throws ConfigurationException {
        final KubernetesResourceModelSource kubernetesResourceModelSource = new KubernetesResourceModelSource(properties);
        kubernetesResourceModelSource.validate();
        return kubernetesResourceModelSource;
    }

    static Description DESC = DescriptionBuilder.builder()
            .name(PROVIDER_NAME)
            .title("Kubernetes")
            .description("Provides nodes from Kubernetes")
            .property(PropertyUtil.integer(REFRESH_INTERVAL, "Refresh Interval", "Minimum time in seconds between API requests to Kubernetes (default is 30)", false, "30"))
            .property(PropertyUtil.string(LABEL_SELECTORS, "Label selectors", "Kubernetes node label selectors", false, null))
            .property(PropertyUtil.string(USERNAME, "Username", "The username used for SSH connections", true, null))
            .property(PropertyUtil.integer(SSH_PORT, "SSH Port", "The port used for SSH connections (default is 22)", true, "22"))
            .build();

    public Description getDescription() {
        return DESC;
    }
}
