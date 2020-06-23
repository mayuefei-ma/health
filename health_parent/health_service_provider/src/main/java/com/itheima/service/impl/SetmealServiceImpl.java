package com.itheima.service.impl;




import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.constant.RedisConstant;
import com.itheima.dao.CheckGroupDao;
import com.itheima.dao.CheckItemDao;
import com.itheima.dao.SetmealDao;
import com.itheima.entity.PageResult;
import com.itheima.entity.QueryPageBean;
import com.itheima.pojo.CheckGroup;
import com.itheima.pojo.CheckItem;
import com.itheima.pojo.Setmeal;
import com.itheima.service.SetmealService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import redis.clients.jedis.JedisPool;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service(interfaceClass = SetmealService.class)
@Transactional
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealDao setmealDao;

    @Autowired
    private JedisPool jedisPool;
    @Autowired
    private CheckGroupDao checkGroupDao;
    @Autowired
    private CheckItemDao checkItemDao;

    @Autowired
    private FreeMarkerConfigurer freeMarkerConfigurer;
    @Value("${out_put_path}")//从属性文件读取输出目录的路径
    private String outputpath;

    //新增套餐,同时关联检查组
    @Override
    public void add(Setmeal setmeal, Integer[] checkgroupIds) {
        setmealDao.add(setmeal);
        Integer setmealId = setmeal.getId();//获取套餐ID
        this.setCheckGroupAndSetmeal(setmealId,checkgroupIds);

        //将图片名称保存到Redis
        String fileName = setmeal.getImg();
        if(fileName!=null&&fileName.length()>0){
            jedisPool.getResource().sadd(RedisConstant.SETMEAL_PIC_DB_RESOURCES,fileName);
        }

        //当添加套餐后需要重新生成静态页面（套餐列表页面、套餐详情页面）
        generateMobileStaticHtml();

        genById(setmealId);
    }

    //生成当前方法所需的静态页面
    public void generateMobileStaticHtml(){
        //在生成静态页面之前需要查询数据
        List<Setmeal> list = setmealDao.findAll();
        //需要生成套餐列表静态页面
        generteMobileSetmealListHtml(list);
        //需要生成套餐详情静态页面
        //generateMobileSetmealDetailHtml(list);
    }

    //生成套餐列表静态页面
    public void generteMobileSetmealListHtml(List<Setmeal> list){
        Map map=new HashMap();
        //为模板提供数据,用于生成静态页面
        map.put("setmealList",list);
        generteHtml("mobile_setmeal.ftl","m_setmeal.html",map);
    }

    //生成套餐详情静态页面（可能有多个）
    public void generateMobileSetmealDetailHtml(List<Setmeal> list){

        for (Setmeal setmeal : list) {
            Map map = new HashMap();
            map.put("setmeal",findById(setmeal.getId()));
            generteHtml("mobile_setmeal_detail.ftl","setmeal_detail_" + setmeal.getId() + ".html",map);
        }
    }
    //生成套餐详情静态页面（单个个）
    public void genById(Integer setmealId){
        Map map = new HashMap();
        map.put("setmeal",setmealDao.findById(setmealId));
        generteHtml("mobile_setmeal_detail.ftl","setmeal_detail_" + setmealId + ".html",map);
    }




    //通用的方法，用于生成静态页面
    public void generteHtml(String templateName,String htmlPageName,Map map){
        Configuration configuration = freeMarkerConfigurer.getConfiguration();//获得配置对象
        Writer out = null;
        try {
            Template template = configuration.getTemplate(templateName);
            //构造输出流
            // 中文乱码
            //out = new BufferedWriter (new OutputStreamWriter (new FileOutputStream (outPutPath + "/" + htmlPageName),"UTF-8"));            //构造输出流
            out = new FileWriter(new File(outputpath + "/" + htmlPageName));
            //输出文件
            template.process(map,out);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }









    @Override
    public PageResult pageQuery(QueryPageBean queryPageBean) {
        Integer currentPage = queryPageBean.getCurrentPage();
        Integer pageSize = queryPageBean.getPageSize();
        String queryString = queryPageBean.getQueryString();
        PageHelper.startPage(currentPage,pageSize);
        Page<Setmeal> page=setmealDao.findByCondition(queryString);
        return new PageResult(page.getTotal(),page.getResult());
    }

    @Override
    public void delete(Integer id) {
        //根据检查组id删除中间表数据（清理原有关联关系）
        setmealDao.deleteAssociation(id);
        setmealDao.delete(id);
    }


    //查询套餐列表数据
    @Override
    public List<Setmeal> findAll() {

        return setmealDao.findAll();
    }

    //根据id查询套餐详情(套餐基本信息,套餐对应的检查组信息,检查组对应的检查项信息)
    @Override
    public Setmeal findById(int id) {
        //根据id 关联查询t_setmeal表的基本信息
        Setmeal setmeal = setmealDao.findById(id);
        List<CheckGroup> checkGroups = checkGroupDao.findCheckGroupBysetMealId(id);
        setmeal.setCheckGroups(checkGroups);
        /*循环遍历*/
        for (CheckGroup checkGroup : checkGroups) {
            /*根据检查组id 查询关联的检查项信息*/
            List<CheckItem> checkItems = checkItemDao.findCheckItemByGroupId(checkGroup.getId());
            checkGroup.setCheckItems(checkItems);
        }
        return setmealDao.findById(id);
    }

    //查询套餐预约占比数据
    @Override
    public List<Map<String, Object>> findSetmealcount() {
        return setmealDao.findSetmealCount();
    }


    //工具
    public void setCheckGroupAndSetmeal(Integer setmealId,Integer[] checkgroupIds){
        if(checkgroupIds != null && checkgroupIds.length > 0){
            for (Integer checkgroupId : checkgroupIds) {
                Map<String,Integer> map = new HashMap<>();
                map.put("setmealId",setmealId);
                map.put("checkgroupId",checkgroupId);
                setmealDao.setCheckGroupAndSetmeal(map);
            }
        }
    }
}
