/*
 * Copyright (c) 2009-2016, United States Government, as represented by the Secretary of Health and Human Services.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above
 *       copyright notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 *     * Neither the name of the United States Government nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE UNITED STATES GOVERNMENT BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package gov.hhs.fha.nhinc.admingui.managed;

import gov.hhs.fha.nhinc.admingui.constant.NavigationConstant;
import gov.hhs.fha.nhinc.admingui.event.model.Audit;
import gov.hhs.fha.nhinc.admingui.services.AuditService;
import gov.hhs.fha.nhinc.admingui.services.impl.AuditServiceImpl;
import gov.hhs.fha.nhinc.admingui.util.ConnectionHelper;
import gov.hhs.fha.nhinc.admingui.util.XSLTransformHelper;
import gov.hhs.fha.nhinc.nhinclib.NhincConstants;
import gov.hhs.fha.nhinc.nhinclib.NullChecker;
import gov.hhs.fha.nhinc.util.HomeCommunityMap;
import gov.hhs.fha.nhinc.util.StreamUtils;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achidamb
 */
@ManagedBean(name = "auditSearchBean")
@SessionScoped
public class AuditSearchBean {

    private static final Logger LOG = LoggerFactory.getLogger(AuditSearchBean.class);

    private String userId;
    private Map<String, String> remoteHcidMap;
    private Date eventStartDate;
    private Date eventEndDate;
    private List<String> eventTypeList;
    private int activeIndex = 0;
    private List<String> selectedRemoteHcidList;
    private List<String> selectedEventTypeList;
    private List<Audit> auditRecordList;
    private String auditMessage;
    private boolean auditFound;
    private String messageId;
    private String relatesTo;
    private long auditId;
    private Map<String, String> remoteHcidOrgNameMap;
    private String auditBlobMsg;
    private final String AUDIT_RECORDS_FOUND = "Audit Records Found";
    private final String AUDIT_RECORDS_NOT_FOUND = "Audit Records not Found";
    public static final String DEFAULT_AUDIT_XSL_FILE = "/WEB-INF/audit.xsl";
    public String blobContent;

    private final XSLTransformHelper transformHelper = new XSLTransformHelper();
    private final AuditService service;

    public AuditSearchBean() {
        service = new AuditServiceImpl();
        setEventTypeList(populateEventTypeList());
        //This map stores key=Orgname and value =remoteHcid. Need this map for display in the dropdownbox
        //on AuditSearch screen.
        setRemoteHcidMap(populateRemoteOrgHcid());

        //This map stores key=remoteHcid and value=Orgname. Need this map for datatable display on AuditSearch screen.
        setRemoteHcidOrgNameMap(populateRemoteHcidAndOrgName());
    }

    /**
     * This method searches Audit database based on eventoutcome, serviceTypes, userId, remoteHcid, eventstartDate and
     * eventEndDate
     */
    public void searchAudit() {
        if (NullChecker.isNullish(this.messageId) && NullChecker.isNullish(this.relatesTo)) {
            this.auditRecordList = service.searchAuditRecord(
                this.selectedEventTypeList, NullChecker.isNotNullishIgnoreSpace(userId) ? userId.trim() : null,
                getRemoteHCIDFromSelectedOrgs(), eventStartDate, eventEndDate, getRemoteHcidOrgNameMap());
        } else {
            this.auditRecordList = service.searchAuditRecordBasedOnMsgIdAndRelatesTo(
                NullChecker.isNotNullishIgnoreSpace(messageId) ? messageId.trim() : null,
                NullChecker.isNotNullishIgnoreSpace(relatesTo) ? relatesTo.trim() : null, getRemoteHcidOrgNameMap());
        }
        if (NullChecker.isNullish(this.auditRecordList)) {
            this.auditMessage = AUDIT_RECORDS_NOT_FOUND;
            auditFound = false;
        } else {
            this.auditMessage = AUDIT_RECORDS_FOUND;
            auditFound = true;
        }
    }

    public String clearAuditTab() {
        this.eventEndDate = null;
        this.eventStartDate = null;
        if (this.selectedEventTypeList != null) {
            this.selectedEventTypeList.clear();
        }
        if (this.selectedRemoteHcidList != null) {
            this.selectedRemoteHcidList.clear();
        }

        this.userId = null;
        if (auditRecordList != null) {
            this.auditRecordList.clear();
        }
        this.auditMessage = "";
        this.auditFound = false;
        this.auditBlobMsg = "";
        return NavigationConstant.AUDIT_SEARCH_PAGE;
    }

    public String clearAuditTabMessageId() {
        this.relatesTo = null;
        this.messageId = null;
        if (auditRecordList != null) {
            this.auditRecordList.clear();
        }
        this.auditMessage = "";
        this.auditFound = false;
        return NavigationConstant.AUDIT_SEARCH_PAGE;
    }

    public void fetchAuditBlob() {
        this.blobContent = "";
        this.auditBlobMsg = service.fetchAuditBlob(this.auditId);
        this.blobContent = new String(getAuditHtml());
    }

    private byte[] getAuditHtml() {
        byte[] convertXmlToHtml = null;
        InputStream xsl = FacesContext.getCurrentInstance().getExternalContext().
            getResourceAsStream(DEFAULT_AUDIT_XSL_FILE);
        InputStream xml = new ByteArrayInputStream(this.auditBlobMsg.getBytes());
        if (xsl != null) {
            convertXmlToHtml = transformHelper.convertXMLToHTML(xml, xsl);
            closeStream(xsl);
            closeStream(xml);
        }
        return convertXmlToHtml;
    }

    private void closeStream(InputStream stream) {
        StreamUtils.closeStreamSilently(stream);

    }

    private Map<String, String> populateRemoteOrgHcid() {
        return new ConnectionHelper().getOrgNameRemoteHcidExternalEntities();
    }

    private List<String> populateEventTypeList() {
        List<String> serviceNames = new ArrayList<>();
        for (String serviceName : NhincConstants.NHIN_SERVICE_NAMES.getEnumServiceNamesList()) {
            serviceNames.add(NhincConstants.NHIN_SERVICE_NAMES.valueOf(serviceName).getUDDIServiceName());
        }
        return serviceNames;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getRelatesTo() {
        return relatesTo;
    }

    public void setRelatesTo(String relatesTo) {
        this.relatesTo = relatesTo;
    }

    public boolean isAuditFound() {
        return auditFound;
    }

    public String getAuditMessage() {
        return auditMessage;
    }

    public List<Audit> getAuditRecordList() {
        return this.auditRecordList;
    }

    public void getAuditRecordList(List<Audit> auditRecordList) {
        this.auditRecordList = auditRecordList;
    }

    public List<String> getSelectedEventTypeList() {
        return this.selectedEventTypeList;
    }

    public void setSelectedEventTypeList(List<String> selectedEventTypeList) {
        this.selectedEventTypeList = selectedEventTypeList;
    }

    public List<String> getSelectedRemoteHcidList() {
        return this.selectedRemoteHcidList;
    }

    public void setSelectedRemoteHcidList(List<String> remoteHcid) {
        this.selectedRemoteHcidList = remoteHcid;
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setActiveIndex(int activeIndex) {
        this.activeIndex = activeIndex;
    }

    public Map<String, String> getRemoteHcidMap() {
        return this.remoteHcidMap;
    }

    private void setRemoteHcidMap(Map<String, String> remoteHcidMap) {
        this.remoteHcidMap = remoteHcidMap;
    }

    public Date getEventStartDate() {
        return eventStartDate;
    }

    public void setEventStartDate(Date eventStartDate) {
        this.eventStartDate = eventStartDate;
    }

    public Date getEventEndDate() {
        return eventEndDate;
    }

    public void setEventEndDate(Date eventEndDate) {
        this.eventEndDate = eventEndDate;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getEventTypeList() {
        return this.eventTypeList;
    }

    private void setEventTypeList(List<String> eventTypeList) {
        this.eventTypeList = eventTypeList;
    }

    private Map<String, String> getRemoteHcidOrgNameMap() {
        return remoteHcidOrgNameMap;
    }

    private void setRemoteHcidOrgNameMap(Map<String, String> remoteHcidOrgNameMap) {
        this.remoteHcidOrgNameMap = remoteHcidOrgNameMap;
    }

    public String getAuditBlobMsg() {
        return auditBlobMsg;
    }

    public void setAuditBlobMsg(String auditBlobMsg) {
        this.auditBlobMsg = auditBlobMsg;
    }

    /**
     * @return the auditId
     */
    public long getAuditId() {
        return auditId;
    }

    /**
     * @param auditId
     */
    public void setAuditId(long auditId) {
        this.auditId = auditId;
    }

    public String getBlobContent() {
        return blobContent;
    }

    public void setBlobContent(String blobContent) {
        this.blobContent = blobContent;
    }

    private List<String> getRemoteHCIDFromSelectedOrgs() {
        if (NullChecker.isNotNullish(selectedRemoteHcidList)) {
            List<String> selectedRemoteHcid = new ArrayList<>();
            for (String orgName : selectedRemoteHcidList) {
                selectedRemoteHcid.add(HomeCommunityMap.getHomeCommunityWithoutPrefix(orgName));
            }
            return selectedRemoteHcid;
        }
        return null;
    }

    private Map<String, String> populateRemoteHcidAndOrgName() {
        return new ConnectionHelper().getRemoteHcidOrgNameMap();
    }

}
