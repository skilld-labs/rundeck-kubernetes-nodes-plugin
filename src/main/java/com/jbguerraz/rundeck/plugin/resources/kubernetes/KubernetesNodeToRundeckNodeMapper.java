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
* NodeGenerator.java
* 
* User: Jean-Baptiste Guerraz <a href="mailto:jbguerraz@gmail.com">jbguerraz@gmail.com</a>
* Created: Oct 18, 2010 7:03:37 PM
* 
*/
package com.jbguerraz.rundeck.plugin.resources.kubernetes;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeSystemInfo;
import io.fabric8.kubernetes.api.model.NodeStatus;
import io.fabric8.kubernetes.api.model.NodeAddress;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.common.NodeSetImpl;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * InstanceToNodeMapper produces Rundeck node definitions from Kubernetes Nodes
 *
 * @author Jean-Baptiste Guerraz <a href="mailto:jbguerraz@gmail.com">jbguerraz@gmail.com</a>
 */
class KubernetesNodeToRundeckNodeMapper {
    static final Logger logger = Logger.getLogger(KubernetesNodeToRundeckNodeMapper.class);
    static private String username;
    static private String sshPort;
    private String labelSelectors;
    private Config clientConfiguration;

    KubernetesNodeToRundeckNodeMapper(final Config clientConfiguration) {
	this.clientConfiguration = clientConfiguration;
    }

    public INodeSet performQuery() {
        final NodeSetImpl nodeSet = new NodeSetImpl();
	final NodeList nodeList;
	try (final KubernetesClient client = new DefaultKubernetesClient(clientConfiguration)) {
		if (null != labelSelectors) {
	        	final HashMap<String, String> labelSelectorsMap = (HashMap<String, String>) Arrays.asList(labelSelectors.split(",")).stream().map(s -> s.split("=")).collect(Collectors.toMap(e -> e[0], e -> e[1]));
			nodeList = client.nodes().withLabels(labelSelectorsMap).list();
		}
		else {
			nodeList = client.nodes().list();
		}
	        mapInstances(nodeSet, nodeList);
	} catch (KubernetesClientException e) {
		logger.error(e.getMessage(), e);
	}
	return nodeSet;
    }

    private void mapInstances(final NodeSetImpl nodeSet, final NodeList nodeList) {
        final List<Node> nodes = nodeList.getItems();
        for (final Node node : nodes) {
            final INodeEntry iNodeEntry;
            try {
                iNodeEntry = KubernetesNodeToRundeckNodeMapper.kubernetesNodeToRundeckNode(node);
                if (null != iNodeEntry) {
                    nodeSet.putNode(iNodeEntry);
                }
            } catch (GeneratorException e) {
                logger.error(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    static INodeEntry kubernetesNodeToRundeckNode(final Node k8sNode) throws GeneratorException {
        final NodeEntryImpl node = new NodeEntryImpl();
	final ObjectMeta metadata = k8sNode.getMetadata();
	final NodeStatus nodeStatus = k8sNode.getStatus();
	final NodeSystemInfo nodeInfo = nodeStatus.getNodeInfo();
	final Map<String,Object> additionalProperties = nodeInfo.getAdditionalProperties();
	node.setNodename(metadata.getName());
	node.setUsername(username);
	node.setOsFamily(additionalProperties.get("operatingSystem").toString());
	node.setOsArch(additionalProperties.get("architecture").toString());
	node.setOsName(nodeInfo.getOsImage());
	node.setOsVersion(nodeInfo.getKernelVersion());
	node.setAttribute("KubeletVersion", nodeInfo.getKubeletVersion());
	node.setAttribute("KubeProxyVersion", nodeInfo.getKubeProxyVersion());
        node.setAttribute("ContainerRuntimeVersion", nodeInfo.getContainerRuntimeVersion());
	for (final NodeAddress address : nodeStatus.getAddresses()) {
		if ("InternalIP".equals(address.getType())) {
			node.setHostname(address.getAddress() + ":" + sshPort);
			break;
		}
	}
	final HashSet<String> tags = new HashSet<String>();
	for (Map.Entry<String,String> entry : metadata.getLabels().entrySet()) {
		tags.add(entry.getKey() + ":" + entry.getValue());
	}
	node.setTags(tags);
        return node;
    }

    public String getLabelSelectors() {
        return labelSelectors;
    }

    public void setLabelSelectors(final String labelSelectors) {
        this.labelSelectors = labelSelectors;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSshPort() {
        return sshPort;
    }

    public void setSshPort(String sshPort) {
        this.sshPort = sshPort;
    }

    public static class GeneratorException extends Exception {
        public GeneratorException() {
        }

        public GeneratorException(final String message) {
            super(message);
        }

        public GeneratorException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public GeneratorException(final Throwable cause) {
            super(cause);
        }
    }
}
