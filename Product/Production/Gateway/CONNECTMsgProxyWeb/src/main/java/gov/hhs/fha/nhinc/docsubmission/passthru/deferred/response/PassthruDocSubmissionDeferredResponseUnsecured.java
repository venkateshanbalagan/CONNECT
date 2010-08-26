/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gov.hhs.fha.nhinc.docsubmission.passthru.deferred.response;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.BindingType;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.soap.Addressing;

/**
 *
 * @author JHOPPESC
 */
@WebService(serviceName = "ProxyXDRAsyncResponse_Service", portName = "ProxyXDRAsyncResponse_Port", endpointInterface = "gov.hhs.fha.nhinc.nhincproxyxdr.async.response.ProxyXDRAsyncResponsePortType", targetNamespace = "urn:gov:hhs:fha:nhinc:nhincproxyxdr:async:response", wsdlLocation = "WEB-INF/wsdl/PassthruDocSubmissionDeferredResponseUnsecured/NhincProxyXDRResponse.wsdl")
@BindingType(value = javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
@Addressing(enabled=true)
public class PassthruDocSubmissionDeferredResponseUnsecured {
    @Resource
    private WebServiceContext context;

    public gov.hhs.healthit.nhin.XDRAcknowledgementType provideAndRegisterDocumentSetBAsyncResponse(gov.hhs.fha.nhinc.common.nhinccommonproxy.RespondingGatewayProvideAndRegisterDocumentSetResponseRequestType provideAndRegisterAsyncRespRequest) {
        return new PassthruDocSubmissionDeferredResponseImpl().provideAndRegisterDocumentSetBResponseOrch(provideAndRegisterAsyncRespRequest, context);
    }

}