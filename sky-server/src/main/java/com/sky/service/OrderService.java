package com.sky.service;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {

    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 查询历史订单
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    OrderVO queryById(Long id);

    /**
     * 取消订单
     * @param id
     */
    void cancelOrder(Long id);

    /**
     * 再来一单
     * @param id
     */
    void repeatOrder(Long id);

    /**
     * 管理端查询历史订单
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult pageQueryAdmin(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 各个状态的订单数量统计
     * @return
     */
    OrderStatisticsVO showStatistics();

    /**
     * 接单
     * @param id
     */
    void confirmOrder(Long id);

    /**
     * 管理端拒单
     * @param id
     * @param rejectionReason
     */
    void rejectOrder(Long id, String rejectionReason);

    /**
     * 取消订单
     * @param id
     * @param cancelReason
     */
    void adminCancelOrder(Long id, String cancelReason);

    /**
     * 派送订单
     * @param id
     */
    void deliverOrder(Long id);

    /**
     * 完成订单
     * @param id
     */
    void completeOrder(Long id);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

}
