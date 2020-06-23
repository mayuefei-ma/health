package com.itheima.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.aliyuncs.exceptions.ClientException;
import com.itheima.constant.MessageConstant;
import com.itheima.constant.RedisMessageConstant;
import com.itheima.entity.Result;
import com.itheima.pojo.Order;
import com.itheima.service.OrderService;
import com.itheima.utils.SMSUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.JedisPool;

import java.util.Map;


/**
 * 体检预约处理
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private JedisPool jedisPool;
    @Reference
    private OrderService orderService;
    //在线体检预约
    @RequestMapping("/submit")
    public Result submit(@RequestBody Map map){
        //首先判断参数是否正确，telephone字段是否为null或者空字符串
        String telephone = (String) map.get("telephone");
        //验证码是否为空
        //从Redis中获取保存的验证码
        String validateCodeInRedis= jedisPool.getResource().get(telephone + RedisMessageConstant.SENDTYPE_ORDER);
        //telephone + RedisMessageConstant.SENDTYPE_ORDER
        String validateCode = (String) map.get("validateCode");

        //  2)  没有处理暴力破解
        //  每个手机号每次点击 变量+1 存储到redis

        String numstr = jedisPool.getResource().get(telephone + RedisMessageConstant.SENDTYPE_ORDER+"num");
        int num=0;
        if(numstr!=null&&numstr.length()>0){
            num = Integer.parseInt(numstr);
        }

        if(num>5){
            return new Result(false, "操作过于频繁");
        }

        num++;
        jedisPool.getResource().setex(telephone + RedisMessageConstant.SENDTYPE_ORDER+"num",10,num+"");

        //将用户输入的验证码和Redis中保存的验证码进行比对
        if(validateCodeInRedis!=null&&validateCode!=null&&validateCode.equals(validateCodeInRedis)){
            //如果比对成功，调用服务完成预约业务处理
            map.put("orderType", Order.ORDERTYPE_WEIXIN);//设置预约类型，分为微信预约、电话预约
            Result result=null;
            try {
                result=orderService.order(map);//通过Dubbo远程调用服务实现在线预约业务处理
            } catch (Exception e) {
                e.printStackTrace();
                return new Result(false, MessageConstant.VALIDATECODE_ERROR);
            }
            if (result.isFlag()){
                //预约成功,可以为用户发送短信
                try {
                    SMSUtils.sendShortMessage(SMSUtils.ORDER_NOTICE,telephone, (String) map.get("orderDate"));
                } catch (ClientException e) {
                    e.printStackTrace();
                }
            }
            return result;
        }else {
            //如果比对不成功，返回结果给页面
            return new Result(false, MessageConstant.VALIDATECODE_ERROR);
        }
    }
    /**
     * 根据id查询预约信息，包括套餐信息和会员信息
     * @param id
     * @return
     */
    @RequestMapping("/findById")
    public Result findById(Integer id) {
        try {
            Map map = orderService.findById(id);
            //查询预约信息成功
            return new Result(true, MessageConstant.QUERY_ORDER_SUCCESS, map);
        } catch (Exception e) {
            e.printStackTrace();
            //查询预约信息失败
            return new Result(false, MessageConstant.QUERY_ORDER_FAIL);
        }
    }
}
