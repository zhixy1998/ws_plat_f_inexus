## ws_plat_inexus

工作站后台的业务平台，iNexus产品代码仓库，存放业务相关的java、python等代码

@SysLog注解，用于用户端操作日志记录，必须填入action,module两个参数，入参和回参记录默认是ture

@LogPoint注解，用于记录开发者日志，无需传入任何参数，默认会记录文件地址以及方法名称。加密使用aes对称加密，具体加密流程看com/persee/workstation/uv_inexus/config/logging/SymmetricEncryptor.java。
同时配备了参考解密工具decryption/decrypt_logs_gui.py。如果需要记录接口运行过程中的参数（如报文），请使用LogPointContextHolder.put()。注意，使用该方法时需要确保前端调用接口是否
已使用@LogPoint注解，否则无法记录入参。同一个工具类里，调用方使用了@LogPoint注解的才会生效。

# RocketMQ启动指令
>start mqnamesrv.cmd -n 127.0.0.1
> 
>start mqbroker.cmd -n 127.0.0.1:9876 autoCreateTopicEnable=true