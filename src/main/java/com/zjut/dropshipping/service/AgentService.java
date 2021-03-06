package com.zjut.dropshipping.service;

import com.zjut.dropshipping.common.ServerResponse;
import com.zjut.dropshipping.dataobject.Agent;

/**
 * @author zjxjwxk
 */
public interface AgentService {

    ServerResponse<String> register(Agent agent);

    ServerResponse<Agent> login(String phone, String password);

    ServerResponse<String> requestAgreement(Integer producerId, Integer agentId);

    ServerResponse getAgreementProducer(Integer agentId, String state);

    ServerResponse responseProducerAgreementRequest(Integer agentId, Integer producerId, String response);

    ServerResponse cancelAgreement(Integer agentId, Integer producerId);

    ServerResponse getRecommendProducer(Integer pageNumber, Integer numberOfElements);
}
