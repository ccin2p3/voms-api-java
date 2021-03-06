/**
 * Copyright (c) Istituto Nazionale di Fisica Nucleare, 2006-2014.
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
package org.italiangrid.voms.request.impl;

import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.italiangrid.voms.VOMSError;
import org.italiangrid.voms.request.VOMSACRequest;
import org.italiangrid.voms.request.VOMSServerInfo;
import org.italiangrid.voms.util.VOMSFQANNamingScheme;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

/**
 * 
 * This class builds VOMS XML requests starting from {@link VOMSACRequest}
 * objects.
 * 
 * @author Andrea Ceccanti
 * 
 */
public class VOMSRequestFactory {

  private static volatile VOMSRequestFactory instance = null;

  private String orderString;
  private String targetString;
  private long lifetime = 0;

  protected DocumentBuilder docBuilder;

  public synchronized static VOMSRequestFactory instance() {

    if (instance == null)
      instance = new VOMSRequestFactory();

    return instance;

  }

  private VOMSRequestFactory() {

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setIgnoringComments(true);
    factory.setNamespaceAware(false);
    factory.setValidating(false);

    try {
      docBuilder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new VOMSError(e.getMessage());
    }

  }

  public long getLifetime() {

    return lifetime;
  }

  public void setLifetime(long lifetime) {

    this.lifetime = lifetime;
  }

  public String getOrderString() {

    return orderString;
  }

  public void setOrderString(String orderString) {

    this.orderString = orderString;
  }

  public String getTargetString() {

    return targetString;
  }

  public void setTargetString(String targetString) {

    this.targetString = targetString;
  }

  private void setOptionsForRequest(VOMSRequestFragment fragment) {

    if (orderString != null && orderString != "")
      fragment.buildOrderElement(orderString);

    if (targetString != null && targetString != "")
      fragment.buildTargetsElement(targetString);

    fragment.buildLifetime(lifetime);
  }

  private void loadOptions(VOMSACRequest options) {

    lifetime = options.getLifetime();
  }

  public Document buildRequest(VOMSACRequest acRequest, VOMSServerInfo endpoint) {

    loadOptions(acRequest);

    Document request = docBuilder.newDocument();
    VOMSRequestFragment frag = new VOMSRequestFragment(request);

    if (acRequest.getRequestedFQANs().isEmpty()) {

      frag.groupCommand("/" + endpoint.getVoName());
      setOptionsForRequest(frag);

      request.appendChild(frag.getFragment());
      return request;
    }

    Iterator<String> fqanIter = acRequest.getRequestedFQANs().iterator();
    frag.buildBase64();
    frag.buildVersion();

    while (fqanIter.hasNext()) {

      String FQAN = fqanIter.next();

      if (FQAN.equals("all")) {
        frag.allCommand();
      } else if (VOMSFQANNamingScheme.isGroup(FQAN)) {

        frag.groupCommand(FQAN);

      } else if (VOMSFQANNamingScheme.isRole(FQAN)) {

        frag.roleCommand(VOMSFQANNamingScheme.getRoleName(FQAN));

      } else if (VOMSFQANNamingScheme.isQualifiedRole(FQAN)) {

        frag.mappingCommand(VOMSFQANNamingScheme.getGroupName(FQAN),
          VOMSFQANNamingScheme.getRoleName(FQAN));
      }
    }

    setOptionsForRequest(frag);

    request.appendChild(frag.getFragment());
    return request;
  }

}

/**
 * Helper class to manage the creation of VOMS XML requests.
 * 
 * @author andreaceccanti
 * 
 */
class VOMSRequestFragment {

  private Document doc;

  DocumentFragment fragment;
  Element root;
  Element command;
  Element order;
  Element targets;
  Element lifetime;
  Element base64;
  Element version;

  public VOMSRequestFragment(Document doc) {

    this.doc = doc;

    fragment = doc.createDocumentFragment();
    buildRootElement();
  }

  protected void buildRootElement() {

    root = doc.createElement("voms");
    fragment.appendChild(root);

  }

  private void appendTextChild(Element e, String text) {

    e.appendChild(doc.createTextNode(text));
  }

  private String buildCompatibleOrderString(String s) {

    String[] FQANs = s.split(",");

    if (FQANs.length == 0)
      return "";

    for (int i = 0; i < FQANs.length; i++) {
      if (VOMSFQANNamingScheme.isQualifiedRole(FQANs[i]))
        FQANs[i] = VOMSFQANNamingScheme.toOldQualifiedRoleSyntax(FQANs[i]);
    }

    StringBuilder fqansString = new StringBuilder();

    for (int i = 0; i < FQANs.length; i++) {
      fqansString.append(FQANs);
      if (i < FQANs.length - 1)
        fqansString.append(",");
    }

    return fqansString.toString();
  }

  void buildCommandElement(String cmdString) {

    command = doc.createElement("command");
    appendTextChild(command, cmdString);
    root.appendChild(command);
  }

  void buildOrderElement(String orderString) {

    order = doc.createElement("order");

    // Temporary compatibility hack
    appendTextChild(order, buildCompatibleOrderString(orderString));

    root.appendChild(order);
  }

  void buildTargetsElement(String targetString) {

    targets = doc.createElement("targets");
    appendTextChild(targets, targetString);
    root.appendChild(targets);

  }

  void buildLifetime(long lifetime) {

    buildLifetime(Long.toString(lifetime));
  }

  void buildLifetime(String lifetimeString) {

    lifetime = doc.createElement("lifetime");
    appendTextChild(lifetime, lifetimeString);
    root.appendChild(lifetime);
  }

  void buildBase64() {

    base64 = doc.createElement("base64");
    appendTextChild(base64, "1");
    root.appendChild(base64);
  }

  void buildVersion() {

    version = doc.createElement("version");
    appendTextChild(version, "4");
    root.appendChild(version);
  }

  public DocumentFragment getFragment() {

    return fragment;
  }

  public void groupCommand(String groupName) {

    buildCommandElement("G" + groupName);
  }

  public void roleCommand(String roleName) {

    buildCommandElement("R" + roleName);

  }

  public void mappingCommand(String groupName, String roleName) {

    buildCommandElement("B" + groupName + ":" + roleName);

  }

  public void allCommand() {

    buildCommandElement("A");
  }

  public void listCommand() {

    buildCommandElement("N");
  }
}
