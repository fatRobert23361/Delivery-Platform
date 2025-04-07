package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    void insertBatch(List<OrderDetail> res);

    /**
     * 根据订单id查询订单细节
     * @param id
     * @return
     */
    @Select("select * from sky_take_out.order_detail where order_id = #{id}")
    List<OrderDetail> getByOrderId(Long id);
}
