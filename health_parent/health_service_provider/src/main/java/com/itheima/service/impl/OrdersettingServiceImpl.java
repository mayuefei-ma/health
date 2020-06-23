package com.itheima.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.itheima.dao.OrdersettingDao;
import com.itheima.pojo.OrderSetting;
import com.itheima.service.OrdersettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service(interfaceClass =OrdersettingService.class )
@Transactional
public class OrdersettingServiceImpl implements OrdersettingService {
    @Autowired
    private OrdersettingDao ordersettingDao;

    //批量导入预约设置数据
    @Override
    public void add(List<OrderSetting> list) {
        if(list!=null &&list.size()>0){

            for (OrderSetting orderSetting : list) {
                //判断当前日期是否已经进行了预约设置
                long countByOrderDate=ordersettingDao.findCountByOrderDate(orderSetting.getOrderDate());
                if(countByOrderDate>0){
                    //已经进行了预约设置，执行更新操作
                    ordersettingDao.editNumberByOrderDate(orderSetting);
                }else {
                    //没有进行预约设置，执行插入操作
                    ordersettingDao.add(orderSetting);
                }
            }
        }
    }

    @Override
    public List<Map> getOrderSettingByMonth(String date) {//格式：yyyy-M
        String begin=date + "-1";//2019-6-1
        String end=date + "-31";//2019-6-31
        Map<String,String> map= new HashMap<>();
        map.put("begin",begin);
        map.put("end",end);
        //调用DAO，根据日期范围查询预约设置数据
        List<OrderSetting> list=ordersettingDao.getOrderSettingByMonth(map);
        List<Map> result=new ArrayList<>();
        if(list!=null &&list.size()>0){
            for (OrderSetting orderSetting : list) {
                Map<String,Object> m=new HashMap<>();
                m.put("date",orderSetting.getOrderDate().getDate());//获取日期数字（几号）
                m.put("number",orderSetting.getNumber());
                m.put("reservations",orderSetting.getReservations());
                result.add(m);
            }
        }
        return result;
    }

    //根据日期设置对应的预约设置数据
    @Override
    public void editNumberByDate(OrderSetting orderSetting) {
        Date orderDate = orderSetting.getOrderDate();
        //根据日期查询是否已经进行了预约设置
        long count = ordersettingDao.findCountByOrderDate(orderDate);
        if(count>0){
            //当前日期已经进行了预约设置，需要执行更新操作

            ordersettingDao.editNumberByOrderDate(orderSetting);
        }else {
            //当前日期没有就那些预约设置，需要执行插入操作
            ordersettingDao.add(orderSetting);
        }
    }
}
