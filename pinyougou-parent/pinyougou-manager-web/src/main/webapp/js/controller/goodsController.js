 //控制层 
app.controller('goodsController' ,function($scope,$controller,$location,itemCatService,goodsService){	
	
	$controller('baseController',{$scope:$scope});//继承
	
    //读取列表数据绑定到表单中  
	$scope.findAll=function(){
		goodsService.findAll().success(
			function(response){
				$scope.list=response;
			}			
		);
	}    

	
	//分页
	$scope.findPage=function(page,rows){			
		goodsService.findPage(page,rows).success(
			function(response){
				$scope.list=response.rows;	
				$scope.paginationConf.totalItems=response.total;//更新总记录数
			}			
		);
	}
	
	
    //初始化页面实体结构
    $scope.entity={ goodsDesc:{itemImages:[],specificationItems:[]} };
	
	 //读取一级分类列表
    $scope.selectItemCat1List=function(){
          itemCatService.findByParentId(0).success(
    		function(response){
    			 $scope.itemCat1List=response; 
    		 }
    );
    }
    $scope.findCategory1=function(){
    	alert($scope.entity.goods.category1Id);
    	itemCatService.findOne($scope.entity.goods.category1Id).success(
    			function(response){
    				$scope.itemCat1=response;
    			}
    )
    }
    //读取二级分类列表
    $scope.$watch('entity.goods.category1Id', function(newValue, oldValue) {          
    	//根据选择的值，查询二级分类
    	itemCatService.findByParentId(newValue).success(
    		function(response){
    			$scope.itemCat2List=response; 				
    		}
    	);    	
    }); 
    
    //读取三级分类
    $scope.$watch('entity.goods.category2Id', function(newValue, oldValue) {          
    	//根据选择的值，查询二级分类
    	itemCatService.findByParentId(newValue).success(
    		function(response){
    			$scope.itemCat3List=response; 				
    		}
    	);    	
     });

    //三级分类选择后  读取模板ID
    $scope.$watch('entity.goods.category3Id', function(newValue, oldValue) {    
	itemCatService.findOne(newValue).success(
		function(response){
			    $scope.entity.goods.typeTemplateId=response.typeId; //更新模板ID    
		  }
        );

    }); 
/*    //模板ID选择后  更新品牌列表 扩展属性 规格列表
    $scope.$watch('entity.goods.typeTemplateId', function(newValue, oldValue) {    
    	typeTemplateService.findOne(newValue).success(
    		function(response){
    			  $scope.typeTemplate=response;//获取类型模板
    			  $scope.typeTemplate.brandIds= JSON.parse( $scope.typeTemplate.brandIds);//品牌列表
    			  if($location.search()['id']==null){
    				  $scope.entity.goodsDesc.customAttributeItems=JSON.parse( $scope.typeTemplate.customAttributeItems);//扩展属性
    			  }
    		}
         );  */ 
    	

	//查询实体 
	$scope.findOne=function(){
		var id = $location.search()['id'];//获取参数值
		if(id==null){
			return ;
		}

		goodsService.findOne(id).success(
			function(response){
				$scope.entity= response;	
				//向富文本编辑器添加商品介绍
				editor.html($scope.entity.goodsDesc.introduction);
				//显示图片列表
				$scope.entity.goodsDesc.itemImages = JSON.parse($scope.entity.goodsDesc.itemImages);
				//显示扩展属性
				$scope.entity.goodsDesc.customAttributeItems = JSON.parse($scope.entity.goodsDesc.customAttributeItems);	
				//显示规格属性		
				$scope.entity.goodsDesc.specificationItems=JSON.parse($scope.entity.goodsDesc.specificationItems);			
				//SKU列表规格对象转换				
				for( var i=0;i<$scope.entity.itemList.length;i++ ){
					$scope.entity.itemList[i].spec = JSON.parse( $scope.entity.itemList[i].spec);		
				}			

			}
		);	

    };
    
/*    $scope.findSpecList=function(){
    	typeTemplateService.findSpecList($scope.entity.goods.typeTemplateId).success(
				function(response){
					$scope.specList=response;
				}
		); 
    };*/
	//保存 
	$scope.save=function(){				
		var serviceObject;//服务层对象  				
		if($scope.entity.id!=null){//如果有ID
			serviceObject=goodsService.update( $scope.entity ); //修改  
		}else{
			serviceObject=goodsService.add( $scope.entity  );//增加 
		}				
		serviceObject.success(
			function(response){
				if(response.success){
					//重新查询 
		        	$scope.reloadList();//重新加载
				}else{
					alert(response.message);
				}
			}		
		);				
	}
	
	 
	//批量删除 
	$scope.dele=function(){			
		//获取选中的复选框			
		goodsService.dele( $scope.selectIds ).success(
			function(response){
				if(response.success){
					$scope.reloadList();//刷新列表
					$scope.selectIds = [];
				}						
			}		
		);				
	}
	
	$scope.searchEntity={};//定义搜索对象 
	
	//搜索
	$scope.search=function(page,rows){			
		goodsService.search(page,rows,$scope.searchEntity).success(
			function(response){
				$scope.list=response.rows;	
				$scope.paginationConf.totalItems=response.total;//更新总记录数
			}			
		);
	}
    
	// 显示状态
	$scope.status = ["未审核","审核通过","审核未通过","关闭"];
	
	$scope.itemCatList = [];
	// 显示分类:
	$scope.findItemCatList = function(){
		
		itemCatService.findAll().success(function(response){
			for(var i=0;i<response.length;i++){
				$scope.itemCatList[response[i].id] = response[i].name;
			}
		});
	}
	
	// 审核的方法:
	$scope.updateStatus = function(status){
		goodsService.updateStatus($scope.selectIds,status).success(function(response){
			if(response.success){
				$scope.reloadList();//刷新列表
				$scope.selectIds = [];
			}else{
				alert(response.message);
			}
		});
	}
	//根据规格名称和选项名称返回是否被勾选
	$scope.checkAttributeValue=function(specName,optionName){
		var items= $scope.entity.goodsDesc.specificationItems;
		var object= $scope.searchObjectByKey(items,'attributeName',specName);
		if(object==null){
			return false;
		}else{
			//如果能查询到规格选项
			if(object.attributeValue.indexOf(optionName)>=0){
				return true;
			}else{
				return false;
			}
		}			
	}
	


});
