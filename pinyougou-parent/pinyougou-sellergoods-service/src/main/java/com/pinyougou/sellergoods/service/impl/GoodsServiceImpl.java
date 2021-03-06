package com.pinyougou.sellergoods.service.impl;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.mapper.TbBrandMapper;
import com.pinyougou.mapper.TbGoodsDescMapper;
import com.pinyougou.mapper.TbGoodsMapper;
import com.pinyougou.mapper.TbItemCatMapper;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.mapper.TbSellerMapper;
import com.pinyougou.pojo.TbBrand;
import com.pinyougou.pojo.TbGoods;
import com.pinyougou.pojo.TbGoodsDesc;
import com.pinyougou.pojo.TbGoodsDescExample;
import com.pinyougou.pojo.TbGoodsExample;
import com.pinyougou.pojo.TbGoodsExample.Criteria;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbItemCat;
import com.pinyougou.pojo.TbItemExample;
import com.pinyougou.pojo.TbSeller;
import com.pinyougou.pojogroup.Goods;
import com.pinyougou.sellergoods.service.GoodsService;

import entity.PageResult;

/**
 * 服务实现层
 * @author Administrator
 *
 */
@Service
@Transactional
public class GoodsServiceImpl implements GoodsService {

	@Autowired
	private TbGoodsMapper goodsMapper;
	@Autowired
	private TbGoodsDescMapper goodsDescMapper;
	@Autowired
	private TbItemMapper itemMapper;
	
	@Autowired
	private TbBrandMapper brandMapper;
	
	@Autowired
	private TbItemCatMapper itemCatMapper;
	
	@Autowired
	private TbSellerMapper sellerMapper;

	
	/**
	 * 查询全部
	 */
	@Override
	public List<TbGoods> findAll() {
		return goodsMapper.selectByExample(null);
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);		
		Page<TbGoods> page=   (Page<TbGoods>) goodsMapper.selectByExample(null);
		return new PageResult(page.getTotal(), page.getResult());
	}
	
	

	/**
	 * 增加
	 */
	@Override
	public void add(Goods goods) {
		goods.getGoods().setAuditStatus("0");//状态：未审核
		//插入商品基本信息
		goodsMapper.insert(goods.getGoods());	
		//设置ID
		goods.getGoodsDesc().setGoodsId(goods.getGoods().getId());
		//插入商品扩展数据
		goodsDescMapper.insert(goods.getGoodsDesc());
		//插入sku商品数据
		saveItemList(goods);
		
	}
	
	//插入sku列表数据
	private void saveItemList(Goods goods){
		if("1".equals(goods.getGoods().getIsEnableSpec())){
			for(TbItem item : goods.getItemList()){
				//手动插入
				//构建标题 SPU名称+ 规格选项值
				String title = goods.getGoods().getGoodsName();//SPU名称
				Map<String,Object> map = JSON.parseObject(item.getSpec());
				for(String key : map.keySet()){
					title += " "+ map.get(key);
				}
				item.setTitle(title);
				setItemValues(item,goods);
				itemMapper.insert(item);
			}
		}else{
			//没有启用规格
			TbItem item=new TbItem();
			//标题
			item.setTitle(goods.getGoods().getGoodsName());
			//价格
			item.setPrice(goods.getGoods().getPrice());
			//状态
			item.setStatus("1");
			//是否默认	
			item.setIsDefault("1");	
			//库存数量
			item.setNum(99999);
			//规格
			item.setSpec("{}");			
			setItemValues(item,goods);					
			itemMapper.insert(item);

		}
	}
	
	private void setItemValues(TbItem item, Goods goods){
		//商品分类编号（3级）
		item.setCategoryid(goods.getGoods().getCategory3Id());
		//创建日期
		item.setCreateTime(new Date());
		//修改日期
		item.setUpdateTime(new Date());
		//商品SPU编号 ID
		item.setGoodsId(goods.getGoods().getId());
		//商家编号 ID
		item.setSellerId(goods.getGoods().getSellerId());
		//品牌名称
		TbBrand brand = brandMapper.selectByPrimaryKey(goods.getGoods().getBrandId());
		item.setBrand(brand.getName());
		//分类名称
		TbItemCat itemCat = itemCatMapper.selectByPrimaryKey(goods.getGoods().getCategory3Id());
		item.setCategory(itemCat.getName());		
		//商家名称(店铺)
		TbSeller seller = sellerMapper.selectByPrimaryKey(goods.getGoods().getSellerId());
		item.setSeller(seller.getNickName());		
		//图片地址（取spu的第一个图片）
		List<Map> imageList = JSON.parseArray(goods.getGoodsDesc().getItemImages(), Map.class) ;
		if(imageList.size()>0){
			item.setImage ( (String)imageList.get(0).get("url"));
		}		
	}
	
	/**
	 * 修改
	 */
	@Override
	public void update(Goods goods){
		if(goods.getGoods().getIsMarketable()=="1"){
			goods.getGoods().setIsMarketable("1");
		}else if(goods.getGoods().getIsMarketable()=="0"){
			goods.getGoods().setIsMarketable("0");
		}
		//更新商品表
		goodsMapper.updateByPrimaryKey(goods.getGoods());
		//更新商品扩展表
		goodsDescMapper.updateByPrimaryKey(goods.getGoodsDesc());
		//删除原有的sku列表数据		
		TbItemExample example = new TbItemExample();
		com.pinyougou.pojo.TbItemExample.Criteria criteria = example.createCriteria();
		criteria.andGoodsIdEqualTo(goods.getGoods().getId());	
		itemMapper.deleteByExample(example);
		//插入sku商品数据
		saveItemList(goods);
	}	
	
	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */
	@Override
	public Goods findOne(Long id){
		Goods goods=new Goods();
		//查询单表goods
		TbGoods tbGoods = goodsMapper.selectByPrimaryKey(id);
		//封装到组合实体类中
		goods.setGoods(tbGoods);
		//查询单表goods_desc
		TbGoodsDesc tbGoodsDesc = goodsDescMapper.selectByPrimaryKey(id);
		//封装到组合实体类中
		goods.setGoodsDesc(tbGoodsDesc);
		//查询单表item 读取SKU列表
		TbItemExample example=new TbItemExample();
		com.pinyougou.pojo.TbItemExample.Criteria criteria = example.createCriteria();
		criteria.andGoodsIdEqualTo(id);//查询条件：商品ID
		List<TbItem> itemList = itemMapper.selectByExample(example);		
		goods.setItemList(itemList);

		return goods;
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {
		for(Long id:ids){
			TbGoods goods = goodsMapper.selectByPrimaryKey(id);
			goods.setIsDelete("1");//表示逻辑删除
			goodsMapper.updateByPrimaryKey(goods);
		}		
	}
	
	
		@Override
	public PageResult findPage(TbGoods goods, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		
		TbGoodsExample example=new TbGoodsExample();
		Criteria criteria = example.createCriteria();
		criteria.andIsDeleteIsNull();//非逻辑删除状态
		
		if(goods!=null){			
			if(goods.getSellerId()!=null && goods.getSellerId().length()>0){
				//criteria.andSellerIdLike("%"+goods.getSellerId()+"%");
				criteria.andSellerIdEqualTo(goods.getSellerId());
			}
			if(goods.getGoodsName()!=null && goods.getGoodsName().length()>0){
				criteria.andGoodsNameLike("%"+goods.getGoodsName()+"%");
			}
			if(goods.getAuditStatus()!=null && goods.getAuditStatus().length()>0){
				criteria.andAuditStatusLike("%"+goods.getAuditStatus()+"%");
			}
			if(goods.getIsMarketable()!=null && goods.getIsMarketable().length()>0){
				criteria.andIsMarketableLike("%"+goods.getIsMarketable()+"%");
			}
			if(goods.getCaption()!=null && goods.getCaption().length()>0){
				criteria.andCaptionLike("%"+goods.getCaption()+"%");
			}
			if(goods.getSmallPic()!=null && goods.getSmallPic().length()>0){
				criteria.andSmallPicLike("%"+goods.getSmallPic()+"%");
			}
			if(goods.getIsEnableSpec()!=null && goods.getIsEnableSpec().length()>0){
				criteria.andIsEnableSpecLike("%"+goods.getIsEnableSpec()+"%");
			}
			if(goods.getIsDelete()!=null && goods.getIsDelete().length()>0){
				criteria.andIsDeleteLike("%"+goods.getIsDelete()+"%");
			}
	
		}
		
		Page<TbGoods> page= (Page<TbGoods>)goodsMapper.selectByExample(example);		
		return new PageResult(page.getTotal(), page.getResult());
	}

	@Override
	public void updateStatus(Long[] ids, String status) {
		for(Long id:ids){
			TbGoods goods = goodsMapper.selectByPrimaryKey(id);
			goods.setAuditStatus(status);
			goodsMapper.updateByPrimaryKey(goods);
		}
		
	}
	
	@Override
	public List<TbItem> findItemListByGoodsIdandStatus(Long[] goodsIds, String status) {
		TbItemExample example=new TbItemExample();
	    com.pinyougou.pojo.TbItemExample.Criteria criteria = example.createCriteria();
	    criteria.andStatusEqualTo(status);//已审核
		criteria.andGoodsIdIn(Arrays.asList(goodsIds));//指定条件 SPUID集合
		return itemMapper.selectByExample(example);
	}

	@Override
	public void onStatus(Long[] ids) {
		for(Long id:ids){
			TbGoods goods = goodsMapper.selectByPrimaryKey(id);
			goods.setIsMarketable("1");//1表示上架
			goodsMapper.updateByPrimaryKey(goods);
		}	
		
	}
	
	@Override
	public void offStatus(Long[] ids) {
		for(Long id:ids){
			TbGoods goods = goodsMapper.selectByPrimaryKey(id);
			goods.setIsMarketable("0");//0表示下架
			goodsMapper.updateByPrimaryKey(goods);
		}	
		
	}

}
