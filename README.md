<center>
  <h2>
    震荡分析池
  </h2>
</center>


**Nodejs版本实现  ==> [smartpool](https://github.com/foreverchens/smartpool-nodejs)** 


### 0、一句话介绍

> 这是一个针对网格策略而设计的选币策略程序、可将币对的历史k线走势量化为一组震荡指标。

### 1、适合人群

> 熟悉网格策略盈利逻辑、但缺乏选币策略的人。

### 2、实现思路

> 类似于正态分布曲线的形成过程、程序将每根k线量化为了一组连续的点、然后正态分布~
>
> 最后可得到某币对在指定周期内的震荡区间和震荡得分等信息

### 3、需要更多

> 更多介绍可通过git查看本文档历史版本~

### 4、结果示例

> ```java
> 03:04:45.523 [main] INFO icu.smartpool.job.SmartPoolJob - ～～周期24H-振幅-[8,10)-前5排名～～
> {"amplitude":9.71,"highP":"18.9907","lowP":"17.3094","score":2752,"symbol":"DCRUSDT"}
> {"amplitude":9.03,"highP":"7.5943","lowP":"6.9649","score":2639,"symbol":"PROMUSDT"}
> {"amplitude":8.38,"highP":"1.2552","lowP":"1.1580","score":1625,"symbol":"NTRNUSDT"}
> {"amplitude":8.39,"highP":"1.4604","lowP":"1.3474","score":1598,"symbol":"SUIUSDT"}
> {"amplitude":9.72,"highP":"36.1905","lowP":"32.9817","score":1358,"symbol":"AVAXUSDT"}
> 03:04:45.567 [main] INFO icu.smartpool.job.SmartPoolJob - ～～周期24H-振幅-[6,8)-前5排名～～
> {"amplitude":7.22,"highP":"0.4395","lowP":"0.4099","score":4564,"symbol":"ALTUSDT"}
> {"amplitude":6.5,"highP":"1.2857","lowP":"1.2071","score":3189,"symbol":"AIUSDT"}
> {"amplitude":6.47,"highP":"0.0000","lowP":"0.0000","score":2164,"symbol":"BONKUSDT"}
> {"amplitude":6.84,"highP":"0.0005","lowP":"0.0004","score":2125,"symbol":"1000SATSUSDT"}
> {"amplitude":7.96,"highP":"0.9097","lowP":"0.8427","score":2107,"symbol":"XAIUSDT"}
> 03:04:45.567 [main] INFO icu.smartpool.job.SmartPoolJob - ～～周期24H-振幅-[4,6)-前5排名～～
> {"amplitude":4.5,"highP":"0.5929","lowP":"0.5674","score":3690,"symbol":"NFPUSDT"}
> {"amplitude":4.09,"highP":"2.0931","lowP":"2.0108","score":3629,"symbol":"RADUSDT"}
> {"amplitude":4.65,"highP":"5.4351","lowP":"5.1933","score":3164,"symbol":"UMAUSDT"}
> {"amplitude":4.7,"highP":"0.2825","lowP":"0.2698","score":3051,"symbol":"DUSKUSDT"}
> {"amplitude":4.76,"highP":"0.1059","lowP":"0.1011","score":2938,"symbol":"OMUSDT"}
> 03:04:45.567 [main] INFO icu.smartpool.job.SmartPoolJob - ～～周期24H-振幅-[2,4)-前5排名～～
> {"amplitude":2.88,"highP":"0.0000","lowP":"0.0000","score":15400,"symbol":"PEPEUSDT"}
> {"amplitude":3.58,"highP":"0.0000","lowP":"0.0000","score":9024,"symbol":"BTTCUSDT"}
> {"amplitude":2.42,"highP":"17.4303","lowP":"17.0182","score":5031,"symbol":"TIAUSDT"}
> {"amplitude":2.25,"highP":"16.2850","lowP":"15.9264","score":4869,"symbol":"CREAMUSDT"}
> {"amplitude":2.07,"highP":"0.0001","lowP":"0.0000","score":4410,"symbol":"LUNCUSDT"}
> ```
>
> **字段描述**
>
> ```yaml
> symbol: ~
> lowP: 震荡区间下限
> highP: 震荡区间上限
> amplitude: 区间振幅
> score: 震荡得分、程序实现为一种点位密度、正比于总点数、反比于区间振幅
> ```
>
> 我设置的参数将币对分为五组、区间振幅[2,4)、[4,6)、[6,8)、[8,10)和其他
>
> 理论上、同一区间内、score越高、越适合进行网格套利
>
> 最后、牛市行情谨慎操作网格