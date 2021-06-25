1. <center>
     <h2>
       震荡机枪池白皮书
     </h2>
   </center>

   ## 0、导论

   **机枪池是什么？它能做什么？它是怎么工作的？**

   

   ```yaml
   		机枪池是一类通过不断的切换、来达到将筹码始终投入到理论收益最大的投资策略的一类应用程序、它对于提高用户收益是显而易见的、不同的机枪池通过其各自的推荐算法从而始终计算出理论收益最大的那支投资标。那么、震荡机枪池又是什么？
   		震荡机枪池即是推荐当前行情下震荡状态最强的那只投资标。推荐出来了、然后呢？那么便可以通过网格策略不断套利、且理论收益将会是较高的、下面进入正题。
   		--聚合器又是什么？
   ```

   ## 1、网格策略

   ### 1.1、套利过程

   ```yaml
   传统的网格策略即是在震荡行情中、不断的执行低买高卖的行为过程。一图以蔽之
   ```

   ![image-20210618221132318](https://gitee.com/yyy5325/smartpool/raw/master/imgs/%E7%BD%91%E6%A0%BC%E5%A5%97%E5%88%A9%E5%9B%BE.png)

   ### 1.2、收益分析

   在量化策略优劣分析中、网格策略仅适合于在震荡行情下进行套利操作、在单边上涨行情时、你甚至跑不过基准收益。既然仅适合于震荡行情、那么我们还是有必要使用当前处于震荡行情的投资标进行网格套利、且越震荡我越快乐。这似乎是一个很实用的结论。

   但好像你还是不知道怎么选。下面我们先看下列这些例子

   **如果当下行情有两支投资标的走势如下图、那么你会如何选择？**

   #### 例1

   ![image-20210618223451788](https://gitee.com/yyy5325/smartpool/raw/master/imgs/sin2x-sinx.png)

   很明显、左图的震荡状态更佳一些、因为两条函数的振幅虽然相同、但是左图的周期更短、波动更强、所以更适合做网格！

   #### 例2

   ![image-20210618223836798](https://gitee.com/yyy5325/smartpool/raw/master/imgs/sin2x-2sinx.png)

   你开始有些犹豫了、左图虽然周期更小、但是振幅也较小、右图虽然周期大、但是振幅也大、那么如何选择呢？

   下面我们将两条曲线在一个坐标上显示

   ![image-20210618225055141](https://gitee.com/yyy5325/smartpool/raw/master/imgs/%E5%90%88.png)

   我们发现、这两条曲线有公共周期 **[0,6.28]** ! 且在该周期内、他们对y轴扫过的距离是相同的**(1)**、那么我们可以认为他们的震荡状态是相同的、使用网格机器人对两支投资标套利将是相等的理论收益。

   我们似乎可以得出结论 : **网格策略的理论收益正相关于一定周期内、k线对价格轴的扫过距离。扫过的距离越大、波动越强、震荡状态越优、理论收益越大。**

   这是肯定的、但是、市场上、不会有投资标的走势完美契合三角函数、也不会有两支投资标的走势具有公共周期且周期内震荡状态相等！那么我们如何对走势多样的实盘中进行投资标的震荡分析呢。

   #(1)

   ```html
   已知函数y=sin(2x)、振幅m为1、函数周期t为3.14、公共周期at为6.28、在函数周期t内对y轴扫过的距离为 4 * m = 4、在公共周期at内对y轴扫过的距离为 8
   已知函数y=2sin(x)、振幅m为2、函数周期t为6.28、公共周期at为6.28、在函数周期t和公共周期at内对y轴扫过的距离为 4 * m = 8 
   ```

   ## 2、震荡机枪池

   ### 2.1、分析-y轴映射法

   前文提到、网格策略的理论收益正相关于  **投资标的震荡状态**—> **投资标在指定周期内扫过价格轴的距离**

   那么如何计算其扫过的距离呢?  

   看下图的转换过程:

   ![image-20210620230924400](https://gitee.com/yyy5325/smartpool/raw/master/imgs/%E4%B8%80%E6%AC%A1%E8%A1%8C%E6%83%85%E5%AF%B9y%E6%98%A0%E5%B0%84.png)

   左边是正常的k线数据、四根小时级别k线依次代表：8点到9点、价格从2涨至6刀；9点到10点、价格从6到跌倒4刀；10点到11点、价格从4涨到8；11点到现在、价格从8跌到了0。

   然后将四根蜡烛向价格轴映射、就形成了简单的一次映射堆叠图谱。到了这里我们似乎还不能得到任何有效信息、那么我们先将右边图标简单的颠倒一下、看看像什么。

   ![image-20210620233254090](https://gitee.com/yyy5325/smartpool/raw/master/imgs/%E4%BA%8C%E6%AC%A1%E6%AD%A3%E6%80%81%E6%9B%B2%E7%BA%BF%E5%BD%A2%E6%88%90.png)

   像是即将消去的俄罗斯方块、只不过x轴变成了价格轴、而右边呢、好像是一条均线、但我更建议你用正态分布的眼光去分析它。

   在过去四个小时到走势中、从价格2开始上涨、并两次接触最高点8、之后跌至0、其中在4和6之间来回震荡。

   上面我们说到网格策略的理论收益正相关于投资标在指定周期内扫过价格轴的距离、那么在该行情下、我们可以肯定、该币种在价格4到6区间内、扫过的次数是最频繁的、扫描距离也会是单位价格内最长的、占到了总扫描距离的44.4%、在2到8区间扫过的距离占比为88.8%。

   那么我们是否可以根据正态分布的特征、并确定一个下限、然后将扫描距离占比超过一个比率的且位于曲线中间的一段价格区间给计算出来、并认定该投资标在过去一段时间内主要在该区间内震荡。

   举个栗子:

   ```yaml
   下限设为20%、则投资标在震荡区间内扫过的距离占比必须大于等于80%
   那么上述行情则得到、该投资标的震荡区间为[2,8]、因为其在该区间内扫过的距离占比为88.8%、大于阙值80%。
   ```

   ### 2.2、实现

   

   

   

   

   https://gitee.com/gitee-stars/)
