package com.pinyougou.cart.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.pinyougou.cart.service.CartService;
import com.pinyougou.pojogroup.Cart;

import entity.Result;
import util.CookieUtil;

@RestController
@RequestMapping("/cart")
public class CartController {
	@Reference(timeout=6000)
	private CartService cartService;
	
	@Autowired
	private  HttpServletRequest request;
	
	@Autowired
	private  HttpServletResponse response;
	
	
	/**
	 * 购物车列表
	 * @param request
	 * @return
	 */
	@RequestMapping("/findCartList")
	public List<Cart> findCartList(){
		//得到登陆人账号,判断当前是否有人登陆
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		System.out.println("当前登录人:"+username);
		String cartListString = CookieUtil.getCookieValue(request, "cartList","UTF-8");
		if(cartListString==null || cartListString.equals("")){
			cartListString="[]";
		}
		List<Cart> cartList_cookie = JSON.parseArray(cartListString, Cart.class);
		//如果未登录
		if(username.equals("anonymousUser")){
			//从cookie中提取购物车
			System.out.println("从Cookie中提取购物车");
			return cartList_cookie;

		}else{
			//如果已登录					
			List<Cart> cartList_redis =cartService.findCartListFromRedis(username);//从redis中提取	
			if(cartList_cookie.size()>0){//如果本地存在购物车
				//合并购物车
				List<Cart> cartList=cartService.mergeCartList(cartList_redis, cartList_cookie);	
				//清除本地cookie的数据
				util.CookieUtil.deleteCookie(request, response, "cartList");
				//将合并后的数据存入redis
				cartService.saveCartListToRedis(username, cartList); 
				System.out.println("执行了合并购物车的逻辑");
				return cartList;
			}			
			return cartList_redis;
		}			
	
	}
	
	/**
	 * 添加商品到购物车
	 * @param request
	 * @param response
	 * @param itemId
	 * @param num
	 * @return
	 */
	@RequestMapping("/addGoodsToCartList")
	//@CrossOrigin(origins="http://localhost:9105",allowCredentials="true")
	public Result addGoodsToCartList(Long itemId,Integer num){
		response.setHeader("Access-Control-Allow-Origin", "http://localhost:9105");//可以访问的域（当此方法不需要操作cookie）
		response.setHeader("Access-Control-Allow-Credentials", "true");//如果操作cookie，必须加上这句话

		//得到登陆人账号,判断当前是否有人登陆
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		System.out.println("当前登录人:"+username);
		
		try {			
			//从cookie中获取购物车列表
			List<Cart>cartList =findCartList();
			//调用服务方法操作购物车 覆盖cartList
			cartList = cartService.addGoodsToCartList(cartList, itemId, num);	
			if(username.equals("anonymousUser")){
				//将新的购物车存入cookie
				CookieUtil.setCookie(request, response, "cartList", JSON.toJSONString(cartList),3600*24,"UTF-8");
				System.out.println("向Cookie中存储购物车");
			}else{
				//如果已登录
				cartService.saveCartListToRedis(username, cartList);
			}
			return new Result(true, "添加成功");
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(false, "添加失败");
		}
	}	

}
