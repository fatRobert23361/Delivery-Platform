package com.sky.controller.admin;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Slf4j
@Api(tags = "用户订单相关接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 搜索订单
     * @param ordersPageQueryDTO
     * @return
     */
    @ApiOperation("搜索订单")
    @GetMapping("/conditionSearch")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("搜索订单: {}", ordersPageQueryDTO);
        PageResult pageResult = orderService.pageQueryAdmin(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    @GetMapping("/statistics")
    @ApiOperation("各个状态的订单数量")
    public Result<OrderStatisticsVO> orderStatistics() {
        log.info("各个状态的订单数量统计");
        OrderStatisticsVO orderStatisticsVO= orderService.showStatistics();
        return Result.success(orderStatisticsVO);
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @ApiOperation("查询订单详情")
    @GetMapping("/details/{id}")
    public Result<OrderVO> findOrderByOrderId(@PathVariable Long id) {
        log.info("查询订单详情: {}", id);
        OrderVO orderVO = orderService.queryById(id);
        return Result.success(orderVO);
    }

    /**
     * 接单
     * @param id
     * @return
     */
    @ApiOperation("接单")
    @PutMapping("/confirm")
    public Result confirmOrder(@RequestBody Long id) {
        log.info("商家接单: {}",id);
        orderService.confirmOrder(id);
        return Result.success();
    }

    /**
     * 管理段拒单
     * @param id
     * @return
     */
    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result rejectOrder(@RequestBody Long id, String rejectionReason) {
        log.info("管理端拒单: {},{}",id,rejectionReason);
        orderService.rejectOrder(id,rejectionReason);
        return Result.success();
    }

    /**
     * 取消订单
     * @param id
     * @param cancelReason
     * @return
     */
    @ApiOperation("取消订单")
    @PutMapping("/cancel")
    public Result cancelOrder(@RequestBody Long id, String cancelReason) {
        log.info("取消订单: {},{}",id,cancelReason);
        orderService.adminCancelOrder(id,cancelReason);
        return Result.success();
    }

    /**
     * 派送订单
     * @param id
     * @return
     */
    @ApiOperation("派送订单")
    @PutMapping("/delivery/{id}")
    public Result deliverOrder(@PathVariable Long id) {
        log.info("派送订单: {}", id);
        orderService.deliverOrder(id);
        return Result.success();
    }

    /**
     * 完成订单
     * @param id
     * @return
     */
    @ApiOperation("完成订单")
    @PutMapping("/complete/{id}")
    public Result completeOrder(@PathVariable Long id){
        log.info("完成订单: {}", id);
        orderService.completeOrder(id);
        return Result.success();
    }
}
