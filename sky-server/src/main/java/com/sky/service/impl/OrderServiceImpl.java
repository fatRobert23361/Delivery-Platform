package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.service.ShoppingCartService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //处理各种业务异常
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook == null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        Long currentId = BaseContext.getCurrentId();
        List<ShoppingCart> list = shoppingCartMapper.list(ShoppingCart.builder().userId(currentId).build());
        if(list == null || list.size() == 0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(currentId);
        orders.setAddress(addressBook.getDetail());
        orders.setAddressBookId(addressBook.getId());
        orderMapper.insert(orders);
        //向订单明细表插入n条数据
        List<OrderDetail> res = new ArrayList<>();
        for (ShoppingCart shoppingCart : list) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            res.add(orderDetail);
        }
        orderDetailMapper.insertBatch(res);
        //清空当前用户端购物车数据
        shoppingCartMapper.deleteByUserId(currentId);
        //封装VO返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
        return orderSubmitVO;
    }

    /**
     * 查询历史订单
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        Page<OrderVO> page = orderMapper.page(ordersPageQueryDTO);
        for (OrderVO orderVO: page){
            List<OrderDetail> list = orderDetailMapper.getByOrderId(orderVO.getId());
            orderVO.setOrderDetailList(list);
        }
        return new PageResult(page.getTotal(), page.getResult());

    }

    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    public OrderVO queryById(Long id){
        OrderVO orderVO = orderMapper.queryById(id);
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderVO.getId());
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 取消订单
     * @param orderId
     */
    public void cancelOrder(Long orderId){
        //查询是否存在订单
        Orders orders = orderMapper.queryById(orderId);
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     * @param id
     */
    public void repeatOrder(Long id){
        Orders orders = orderMapper.queryById(id);
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        List<OrderDetail> orderList = orderDetailMapper.getByOrderId(orders.getId());
        for(OrderDetail orderDetail : orderList){
            ShoppingCartDTO shoppingCart = new ShoppingCartDTO();
            if(orderDetail.getDishId() != null){
                shoppingCart.setDishId(orderDetail.getDishId());
            }
            if (orderDetail.getSetmealId() != null){
                shoppingCart.setSetmealId(orderDetail.getSetmealId());
            }
            if(orderDetail.getDishFlavor() != null){
                shoppingCart.setDishFlavor(orderDetail.getDishFlavor());
            }
            for(int i = 0;i<orderDetail.getNumber();i++){
                shoppingCartService.addShoppingCart(shoppingCart);
            }

        }


    }

    /**
     * 管理段历史订单查询
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult pageQueryAdmin(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        //ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        Page<OrderVO> page = orderMapper.page(ordersPageQueryDTO);
        for (OrderVO orderVO: page){
            List<OrderDetail> list = orderDetailMapper.getByOrderId(orderVO.getId());
            orderVO.setOrderDetailList(list);
        }
        return new PageResult(page.getTotal(), page.getResult());

    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    public OrderStatisticsVO showStatistics(){
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setConfirmed(orderMapper.countById(Orders.CONFIRMED));
        orderStatisticsVO.setDeliveryInProgress(orderMapper.countById(Orders.DELIVERY_IN_PROGRESS));
        orderStatisticsVO.setToBeConfirmed(orderMapper.countById(Orders.TO_BE_CONFIRMED));
        return orderStatisticsVO;
    }

    public void confirmOrder(Long orderId){
        //查询是否存在订单
        Orders orders = orderMapper.queryById(orderId);
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        orders.setStatus(Orders.CONFIRMED);
        //orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    public void rejectOrder(Long orderId, String reason){
        //查询是否存在订单
        Orders orders = orderMapper.queryById(orderId);
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setRejectionReason(reason);
        orderMapper.update(orders);
    }

    public void adminCancelOrder(Long orderId, String reason){
        //查询是否存在订单
        Orders orders = orderMapper.queryById(orderId);
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelReason(reason);
        orderMapper.update(orders);
    }

    public void deliverOrder(Long orderId){
        //查询是否存在订单
        Orders orders = orderMapper.queryById(orderId);
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);
    }

    public void completeOrder(Long orderId){
        //查询是否存在订单
        Orders orders = orderMapper.queryById(orderId);
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }

//        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
//        vo.setPackageStr(jsonObject.getString("package"));
        paySuccess(ordersPaymentDTO.getOrderNumber());
        return null;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

}
