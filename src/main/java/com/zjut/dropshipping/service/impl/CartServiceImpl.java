package com.zjut.dropshipping.service.impl;

import com.zjut.dropshipping.common.ServerResponse;
import com.zjut.dropshipping.dataobject.*;
import com.zjut.dropshipping.dto.ShoppingCartItemDTO;
import com.zjut.dropshipping.dto.ShoppingCartItemListDTO;
import com.zjut.dropshipping.repository.*;
import com.zjut.dropshipping.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zjxjwxk
 */
@Service("CartService")
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final GoodsSpecItemRepository goodsSpecItemRepository;
    private final GoodsRepository goodsRepository;
    private final SpecificationRepository specificationRepository;
    private final ProducerRepository producerRepository;

    @Autowired
    public CartServiceImpl(CartRepository cartRepository,
                           GoodsSpecItemRepository goodsSpecItemRepository,
                           GoodsRepository goodsRepository,
                           SpecificationRepository specificationRepository,
                           ProducerRepository producerRepository) {
        this.cartRepository = cartRepository;
        this.goodsSpecItemRepository = goodsSpecItemRepository;
        this.goodsRepository = goodsRepository;
        this.specificationRepository = specificationRepository;
        this.producerRepository = producerRepository;
    }

    @Override
    public ServerResponse getList(Integer agentId) {
        List<ShoppingCart> shoppingCartList = cartRepository.findByAgentId(agentId);
        if (shoppingCartList.size() == 0) {
            return ServerResponse.createByErrorMessage("购物车中还没有商品");
        }
        return ServerResponse.createBySuccess(this.getShoppingCartMap(shoppingCartList));
    }

    @Override
    public ServerResponse add(Integer agentId, String goodsSpecIds, Integer amount) {
        ShoppingCart shoppingCart = new ShoppingCart(agentId, goodsSpecIds, amount);
        if (cartRepository.save(shoppingCart) != null) {
            return ServerResponse.createBySuccess("加入购物车成功");
        }
        return ServerResponse.createByErrorMessage("加入购物车失败");
    }

    @Override
    public ServerResponse delete(Integer agentId, String goodsSpecIds) {
        ShoppingCart shoppingCart = cartRepository.findByGoodsSpecIds(goodsSpecIds);
        if (shoppingCart == null) {
            return ServerResponse.createByErrorMessage("购物车中没有该商品");
        }
        cartRepository.delete(shoppingCart);
        return ServerResponse.createBySuccess("删除购物车商品成功");
    }

    private Map<Integer, ShoppingCartItemListDTO> getShoppingCartMap(List<ShoppingCart> shoppingCartList) {
        Map<Integer, ShoppingCartItemListDTO> shoppingCartMap = new HashMap<>(5);
        for (ShoppingCart shoppingCart :
                shoppingCartList) {
            ShoppingCartItemDTO shoppingCartItemDTO = new ShoppingCartItemDTO();
            Goods goods = new Goods();
            String[] goodsSpecIds = shoppingCart.getGoodsSpecIds().split(";");
            List<Specification> specificationList = new ArrayList<>();
            // 遍历该商品的不同规格
            for (String goodsSpecId :
                    goodsSpecIds) {
                GoodsSpecItem goodsSpecItem = goodsSpecItemRepository.findByGoodsSpecId(Integer.parseInt(goodsSpecId));
                if (shoppingCartItemDTO.getGoodsId() == null) {
                    goods = goodsRepository.findByGoodsId(goodsSpecItem.getGoodsId());
                    shoppingCartItemDTO.setGoodsId(goods.getGoodsId());
                    shoppingCartItemDTO.setGoodsName(goods.getName());
                    // 获得基本价格
                    shoppingCartItemDTO.setPrice(goods.getPrice());
                }
                // 计算该规格的差价
                shoppingCartItemDTO.addPrice(goodsSpecItem.getPriceDifference());
                Specification specification = specificationRepository.findBySpecId(goodsSpecItem.getSpecId());
                specificationList.add(specification);
                shoppingCartItemDTO.setAmount(shoppingCart.getAmount());
            }
            shoppingCartItemDTO.setSpecificationList(specificationList);

            // 将购物车条目列表根据厂家id放入Map
            if (!shoppingCartMap.containsKey(goods.getProducerId())) {
                ShoppingCartItemListDTO shoppingCartItemListDTO = new ShoppingCartItemListDTO();
                Producer producer = producerRepository.findOneById(goods.getProducerId());
                shoppingCartItemListDTO.setProducerId(goods.getProducerId());
                shoppingCartItemListDTO.setProducerName(producer.getName());
                List<ShoppingCartItemDTO> shoppingCartItemDTOList = new ArrayList<>();
                shoppingCartItemListDTO.setShoppingCartItemDTOList(shoppingCartItemDTOList);
                shoppingCartMap.put(goods.getProducerId(), shoppingCartItemListDTO);
            }
            shoppingCartMap.get(goods.getProducerId()).getShoppingCartItemDTOList().add(shoppingCartItemDTO);
        }
        return shoppingCartMap;
    }
}
