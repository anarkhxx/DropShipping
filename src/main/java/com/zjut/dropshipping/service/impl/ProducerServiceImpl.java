package com.zjut.dropshipping.service.impl;

import com.zjut.dropshipping.common.Const;
import com.zjut.dropshipping.common.ResponseCode;
import com.zjut.dropshipping.common.ServerResponse;
import com.zjut.dropshipping.dataobject.Agent;
import com.zjut.dropshipping.dataobject.Agreement;
import com.zjut.dropshipping.dataobject.Producer;
import com.zjut.dropshipping.dataobject.Goods;
import com.zjut.dropshipping.dto.*;
import com.zjut.dropshipping.repository.*;

import com.zjut.dropshipping.service.ProducerService;
import com.zjut.dropshipping.utils.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ljx
 */
@Service("ProducerService")
public class ProducerServiceImpl implements ProducerService {
    private final ProducerRepository producerRepository;
    private final AgentRepository agentRepository;
    private final OrderRepository orderRepository;
    private final EvaluationRepository evaluationRepository;
    private final AgreementRepository agreementRepository;
    private final CategoryRepository categoryRepository;
    private final GoodsRepository goodsRepository;
    @Autowired
    public ProducerServiceImpl(ProducerRepository producerRepository,
                               AgentRepository agentRepository,
                               OrderRepository orderRepository,
                               EvaluationRepository evaluationRepository,
                               AgreementRepository agreementRepository,
                               CategoryRepository categoryRepository,
                               GoodsRepository goodsRepository) {
        this.producerRepository = producerRepository;
        this.agentRepository = agentRepository;
        this.orderRepository = orderRepository;
        this.evaluationRepository = evaluationRepository;
        this.agreementRepository = agreementRepository;
        this.categoryRepository = categoryRepository;
        this.goodsRepository = goodsRepository;
    }

    @Override
    public ServerResponse<String> register(Producer producer) {

        // 检验厂商名
        ServerResponse<String> validResponse = this.checkValid(producer.getName(), Const.PRODUCER_NAME);
        if (validResponse.isError()) {
            return validResponse;
        }

        // 校验电话号码
        validResponse = this.checkValid(producer.getContactPhone(), Const.PHONE);
        if (validResponse.isError()) {
            return validResponse;
        }

        // 校验身份证号
        validResponse = this.checkValid(producer.getContactIdentityNumber(), Const.IDENTITY_NUMBER);
        if (validResponse.isError()) {
            return validResponse;
        }

        // 检验营业执照
        validResponse = this.checkValid(producer.getLicenseNumber(), Const.LICENSE_NUMBER);
        if (validResponse.isError()) {
            return validResponse;
        }

        // 设置状态为未审核
        producer.setState(Const.AccountState.UNREVIEWED);

        // MD5加密
        producer.setPassword(MD5Util.MD5EncodeUtf8(producer.getPassword()));

        producerRepository.save(producer);

        return ServerResponse.createBySuccessMessage("注册成功");
    }

    @Override
    public ServerResponse<Producer> login(String contactPhone, String password) {
        int resultCount = producerRepository.countByContactPhone(contactPhone);
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("该手机号码未被注册");
        }

        String md5Password = MD5Util.MD5EncodeUtf8(password);
        Producer producer = producerRepository.findByContactPhoneAndPassword(contactPhone, md5Password);
        if (producer == null) {
            return ServerResponse.createByErrorMessage("用户名或密码错误");
        }

        producer.setPassword(null);
        return ServerResponse.createBySuccess("登陆成功", producer);
    }

    private ServerResponse<String> checkValid(String str, String type) {
        if (StringUtils.isNotBlank(type)) {
            // 开始校验
            if (Const.PRODUCER_NAME.equals(type)) {
                int resultCount = producerRepository.countByName(str);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("该厂商名已被注册");
                }
            }
            if (Const.PHONE.equals(type)) {
                int resultCount = producerRepository.countByContactPhone(str);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("该手机已被注册");
                }
            }
            if (Const.IDENTITY_NUMBER.equals(type)) {
                int resultCount = producerRepository.countByContactIdentityNumber(str);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("该身份证已被注册");
                }
            }
            if (Const.LICENSE_NUMBER.endsWith(type)) {
                int resultCount = producerRepository.countByLicenseNumber(str);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("该营业执照已被注册");
                }
            }
        } else {
            return ServerResponse.createByErrorMessage("参数错误");
        }
        return ServerResponse.createBySuccessMessage("校验成功");
    }

    @Override
    public ServerResponse<String> producerRequestAgreement(Integer agentId, Integer producerId) {
        Agreement agreement = agreementRepository.findByProducerIdAndAgentId(producerId, agentId);
        Agreement agreement1 = new Agreement();
        agreement1.setProducerId(producerId);
        agreement1.setAgentId(agentId);
        if (agreement == null) {
            agreement1.setState("厂商发送请求");
            agreementRepository.save(agreement1);
            return ServerResponse.createBySuccess("请求发送成功");
        } else if (agreement.getState().equals(Const.AgreementState.NORMAL)) {
            return ServerResponse.createByErrorMessage("已达成协议");
        } else if (agreement.getState().equals(Const.AgreementState.PRODUCER_REQUEST)) {
            return ServerResponse.createByErrorMessage("请求已发送");
        } else {
            agreement1.setState(Const.AgreementState.NORMAL);
            agreementRepository.save(agreement1);
            return ServerResponse.createBySuccess("达成协议");
        }
    }

    @Override
    public ServerResponse getAgentAgreementRequest(Integer producerId) {
        List<Agreement> agreementList = agreementRepository.findByProducerIdAndState(producerId, Const.AgreementState.AGENT_REQUEST);
        if (agreementList.size() == 0) {
            return ServerResponse.createByErrorMessage("还没有厂商请求协议");
        }
        return ServerResponse.createBySuccess(this.getAgentAgreementRequestList(agreementList,producerId));
    }

    @Override
    public ServerResponse responseAgentAgreementRequest(Integer producerId, Integer agentId, String response) {
        Agreement agreement = new Agreement();
        agreement.setAgentId(agentId);
        agreement.setProducerId(producerId);

        if (Const.AgreementResponse.ACCEPT.equals(response)) {
            agreement.setState(Const.AgreementState.NORMAL);
            agreementRepository.save(agreement);
            return ServerResponse.createBySuccess("达成协议");
        } else if (Const.AgreementResponse.REFUSE.equals(response)) {
            agreementRepository.delete(agreement);
            return ServerResponse.createBySuccess("已拒绝请求");
        } else {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
    }

    private List<AgentAgreementRequestDTO> getAgentAgreementRequestList(List<Agreement> agreementList,Integer producerId) {
        List<AgentAgreementRequestDTO> agentAgreementRequestDTOList = new ArrayList<>();

        for (Agreement agreement :
                agreementList) {
            Agent agent = agentRepository.findOneById(agreement.getAgentId());
            AgentAgreementRequestDTO agentAgreementRequestDTO = new AgentAgreementRequestDTO();
            agentAgreementRequestDTO.setId(agent.getId());
            agentAgreementRequestDTO.setName(agent.getName());
            agentAgreementRequestDTO.setPhone(agent.getPhone());
            agentAgreementRequestDTO.setRegion(agent.getRegion());
            agentAgreementRequestDTO.setJoinTime(agent.getJoinTime());
            agentAgreementRequestDTO.setRequestTime(agreementRepository.findTimeByProducerIdAndAgentId(producerId ,agent.getId()));
            agentAgreementRequestDTO.setMonthlysale(orderRepository.findAmountByAgentId(agent.getId()));
            agentAgreementRequestDTO.setLevel(evaluationRepository.findLevelByAgentId(agent.getId()));

            agentAgreementRequestDTOList.add(agentAgreementRequestDTO);
        }
        return agentAgreementRequestDTOList;
    }
    @Override
    public ServerResponse  getRecommendAgent(Integer pageNumber, Integer numberOfElements){

        PageRequest pageRequest = PageRequest.of(pageNumber - 1, numberOfElements);
        Page<Agent> agentPage = agentRepository.findAllByState(Const.AccountState.NORMAL, pageRequest);
        return ServerResponse.createBySuccess(getRecommendPageChunk(agentPage));
    }

    private PageChunk<RecommendAgentDTO> getRecommendPageChunk(Page<Agent> agentPage) {
        PageChunk<RecommendAgentDTO> pageChunk = new PageChunk<>();
        pageChunk.setContent(getRecommendAgentDTO(agentPage.getContent()));
        pageChunk.setTotalPages(agentPage.getTotalPages());
        pageChunk.setTotalElements(agentPage.getTotalElements());
        pageChunk.setPageNumber(agentPage.getPageable().getPageNumber() + 1);
        pageChunk.setNumberOfElements(agentPage.getNumberOfElements());
        return pageChunk;
    }
    private List<RecommendAgentDTO> getRecommendAgentDTO(List<Agent> agentList) {
        List<RecommendAgentDTO> recommendAgentDTOList = new ArrayList<>();
        for (Agent agent :
                agentList) {
            RecommendAgentDTO recommendAgentDTO = new RecommendAgentDTO();
            recommendAgentDTO.setId(agent.getId());
            recommendAgentDTO.setName(agent.getName());
            recommendAgentDTO.setMonthlysale(orderRepository.findAmountByAgentId(agent.getId()));
            recommendAgentDTO.setRegion(agent.getRegion());
            recommendAgentDTO.setLevel(evaluationRepository.findLevelByAgentId(agent.getId()));

            recommendAgentDTOList.add(recommendAgentDTO);
        }
        return recommendAgentDTOList;
    }



    @Override
    public ServerResponse getAcceptedAgent(Integer producerId,Integer pageNumber, Integer numberOfElements){

        PageRequest pageRequest = PageRequest.of(pageNumber - 1, numberOfElements);
        Page<Agent> agentPage = agentRepository.findAcceptedAgentByProducerId(producerId, pageRequest);
        return ServerResponse.createBySuccess(getAcceptedPageChunk(agentPage));
    }



    private PageChunk<AcceptedAgentDTO> getAcceptedPageChunk(Page<Agent> agentPage) {
        PageChunk<AcceptedAgentDTO> pageChunk = new PageChunk<>();
        pageChunk.setContent(getAcceptedAgentDTO(agentPage.getContent()));
        pageChunk.setTotalPages(agentPage.getTotalPages());
        pageChunk.setTotalElements(agentPage.getTotalElements());
        pageChunk.setPageNumber(agentPage.getPageable().getPageNumber() + 1);
        pageChunk.setNumberOfElements(agentPage.getNumberOfElements());
        return pageChunk;
    }

    private List<AcceptedAgentDTO> getAcceptedAgentDTO(List<Agent> agentList) {
        List<AcceptedAgentDTO> AcceptedAgentDTOList = new ArrayList<>();
        for (Agent agent :
                agentList) {
            AcceptedAgentDTO acceptedAgentDTO = new AcceptedAgentDTO();
            acceptedAgentDTO.setId(agent.getId());
            acceptedAgentDTO.setName(agent.getName());
            acceptedAgentDTO.setMonthlysale(orderRepository.findAmountByAgentId(agent.getId()));
            acceptedAgentDTO.setRegion(agent.getRegion());
            acceptedAgentDTO.setLevel(evaluationRepository.findLevelByAgentId(agent.getId()));

            AcceptedAgentDTOList.add(acceptedAgentDTO);
        }
        return AcceptedAgentDTOList;
    }

    @Override
    public ServerResponse getDetailAgent(Integer agentId){
        Agent agent = agentRepository.findOneById(agentId);
        return ServerResponse.createBySuccess(getAgentDetailDTO(agent));
    }


    private AgentDetailDTO getAgentDetailDTO(Agent agent) {

            AgentDetailDTO agentDetailDTO = new AgentDetailDTO();
            agentDetailDTO.setPhone(agent.getPhone());
            agentDetailDTO.setExternalShop(agent.getExternalShop());
            agentDetailDTO.setJoinTime(agent.getJoinTime());
            agentDetailDTO.setLevel(evaluationRepository.findLevelByAgentId(agent.getId()));
            agentDetailDTO.setProductSaleList(categoryRepository.findProductSaleListByAgentId(agent.getId()));

        return agentDetailDTO;
    }

    @Override
    public ServerResponse addGoods(Integer producerId,
                                   String goodsName,
                                   Integer categoryId,
                                   Double price,
                                   Integer stock,
                                   String content) {

                Goods goods = new Goods();
                goods.setProducerId(producerId);
                goods.setCategoryId(categoryId);
                goods.setName(goodsName);
                goods.setPrice(price);
                goods.setStock(stock);
                goods.setState("正常");
                goods.setContent(content);
                goodsRepository.save(goods);

                return ServerResponse.createBySuccess("商品添加成功");

    }
}
