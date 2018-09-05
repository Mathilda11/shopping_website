//控制层 
app.controller('seckillGoodsController' ,function($scope,$location,$interval,seckillGoodsService){	
	//读取列表数据绑定到表单中  
	$scope.findList=function(){
		seckillGoodsService.findList().success(
			function(response){
				$scope.list=response;
			}			
		);
	}       
	
	//查询商品实体 
	$scope.findOne=function(){	
		seckillGoodsService.findOne($location.search()['id']).success(
			function(response){
				$scope.entity= response;
				//倒计时开始
				//获取结束时间到当前时间的秒数  getTime()返回距 1970 年 1 月 1 日之间的毫秒数
				allsecond =Math.floor( (  new Date($scope.entity.endTime).getTime()- (new Date().getTime())) /1000); //总秒数
				time= $interval(function(){ 
				if(allsecond>0){ 
					allsecond =allsecond-1;
					$scope.timeString=convertTimeString(allsecond);//转换时间字符串
				  }else{
					  $interval.cancel(time); 		
					  alert("秒杀服务已结束");
				  }
				},1000);		

			}
		);				
	}
	
	//转换秒为   天小时分钟秒格式  XXX天 10:22:33
	convertTimeString=function(allsecond){
		var days= Math.floor( allsecond/(60*60*24));//天数
		if(days<10){
			days="0"+days;
		}
		var hours= Math.floor( (allsecond-days*60*60*24)/(60*60) );//小数数
		if(hours<10){
			hours="0"+hours;
		}
		var minutes= Math.floor(  (allsecond -days*60*60*24 - hours*60*60)/60    );//分钟数
		if(minutes<10){
			minutes="0"+minutes;
		}
		var seconds= allsecond -days*60*60*24 - hours*60*60 -minutes*60; //秒数
		if(seconds<10){
			seconds="0"+seconds;
		}
		var timeString="";
		if(days>0){
			timeString=days+"天 ";
		}
		return timeString+hours+":"+minutes+":"+seconds;
	}
	
	//提交订单
	$scope.submitOrder=function(){
		seckillGoodsService.submitOrder($scope.entity.id).success(
			function(response){
				if(response.success){
					alert("抢购成功，请在5分钟内完成支付");
					location.href="pay.html";
				}else{
					alert(response.message);
				}
			}
		);		
	}


});	
