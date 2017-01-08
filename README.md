# guice的定时任务注解插件

实现了一个类似spring的定时任务注解功能，在定时方法上添加 Scheduled 注解即可。

暂时只实现了corn的配置方式

##使用方法

1. 用 Guice 加载 `me.libin.guice.quartz.module.QuartzTaskModule` 模块
2. 配置 guice-quartz.properties 文件(配置文件可使用系统参数改变，需要在加载模块之前更改,  
如: `-Dme.libin.guice.quartz.properties=/home/etc/example.properties` ）
3. 配置文件内容如下:  
开启注解定时任务  
`me.libin.guice.quartz.isRun=yes`  
定时任务扫描包  多个包以","分隔  
`me.libin.guice.quartz.scanPackages=com.test.tasks,com.test2.test`
