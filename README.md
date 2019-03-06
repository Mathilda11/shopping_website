## 购物商城2.0
#### ⌛项目描述
本项目是一个综合性的 B2B2C 平台，实现了电商网站的部分功能。  
整个项目系统主要分为网站前台、运营商后台、商家管理后台三个子系统。网站采用商家入驻的模式，商家入驻平台提交申请，运营商平台进行资质审核，审核通过后，商家拥有独立的管理后台录入商品信息。商品经过平台审核后即可发布。用户可在网站前台进行商品浏览、商品搜索、加入购物车、提交订单和秒杀商品等功能。  

本项目采用SOA架构，前端使用AngularJS、Bootstrap，后端使用SSM、Dubbo搭建分布式应用。

#### 🌐开发环境
Windows 10，CentOS 6.5，Eclipse 4.5.2，Tomcat 7

#### 👜数据库
MySQL 5.5

#### 💌第三方工具
阿里云•云通信

#### ✍具体实现
①实现运营商后台的品牌管理的列表功能。  

②使用SpringSecurity完成运营商、商家系统的登陆与安全控制功能，CAS与SpringSecurity集成实现用户中心的单点登录。  

③实现商家后台的商品分类列表管理功能，借助富文本编辑器实现商品录入功能和商品管理功能。  

④实现运营商后台的广告类型管理与广告管理功能，并使用Redis缓存广告数据。  

⑤使用Solr实现商品搜索功能，包括关键字查询、条件筛选，运用Freemarker技术来实现商品详情页的静态化，并通过ActiveMQ中间件实现运营商后台的审核服务与搜索服务、网页生成服务的零耦合。  

⑥实现用户注册，并使用云通信构建短信发送服务以实现注册判断短信验证码。  

⑦结合Cookie和Session实现存储用户的购物车。  

⑧实现加入购物车、提交订单功能。  

⑨在秒杀活动页显示参与秒杀的商品，用户在秒杀商品详情页点击立即抢购实现秒杀下单，同时使用令牌桶算法进行限流，并使用SpringTask框架实现秒杀商品列表的定时更新和删除。

#### 💎难点
- 购物车  
当用户在未登录的情况下，将此购物车存入Cookies, 在用户登陆的情况下，将购物车数据存入Redis。如果用户登陆时，若Cookies中存在购物车，需要将Cookies的购物车合并到Redis中存储，Cookies中的购物车清空。

- 秒杀活动  
秒杀技术实现核心思想是运用缓存减少数据库瞬间的访问压力。读取商品详细信息时运用缓存，当用户点击抢购时减少缓存中的库存数量，当库存数为0时或活动期结束时，同步到数据库。产生的秒杀预订单也不会立刻写到数据库中，而是先写到缓存，当用户付款成功后再写入数据库。  

   * 秒杀商品更新  
每分钟执行查询秒杀商品表，将符合条件的记录并且缓存中不存在的秒杀商品存入缓存。有的就不更新，否则库存被覆盖。
秒杀商品列表是发生变化的，需要定时任务，定时地从数据库中取出符合条件的商品放入到缓存中。   

   * 秒杀商品清除
每秒种在缓存的秒杀商品列表中查询过期的商品，发现过期同步到数据库，并在缓存中移除该秒杀商品。
使用Spring线程池ThreadPoolTaskExecutor，加快移除速度，尽快释放占用的缓存。  

   * 秒杀商品抢购
商品详细页点击立即抢购实现秒杀下单，下单时扣减库存。当库存为0或不在活动期范围内无法秒杀。  
秒杀这个场景来说，最终抢到商品的人数是固定的，所以100人和10000人发起请求的结果都是一样的算法，并发度越高，无效请求越多。
所以使用了限流：令牌桶算法。  
令牌桶算法是按照恒定的速率向桶中放入令牌，每当请求经过时则消耗一个或多个令牌。当桶中的令牌为0时，请求则会被阻塞。
RateLimiter是Guava提供的基于令牌桶算法的实现类，可以非常简单的完成限流，并且根据系统的实际情况来调整生成令牌的速率，
实际业务在每次响应请求之前都从桶中获取令牌，只有取到令牌的请求才会被成功响应，获取的方式有两种：阻塞等待令牌或者非阻塞等待令牌。  
本次采用的是非阻塞等待令牌。
1秒钟产生10枚令牌，判断能否在1秒内得到令牌，如果不能则立即返回false，不会阻塞程序。  

😆更多秒杀系统设计的思路/方法：