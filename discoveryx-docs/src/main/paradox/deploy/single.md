# 单机部署

*请先阅读 @ref[软件包](package.md) 。*

## 使用

单机部署默认使用嵌入式数据库 **H2**。Fusion DiscoveryX 将在第一次启动服务时在用户主目录下创建 H2 数据库目录并初始化数据表和用户，默认用户：

- SN: `discoveryx`
- PW: `discoveryx`

@@@vars
```
unzip discoveryx-server-$version$.zip
cd discoveryx-server-$version$
./bin/discoveryx-server
```
@@@

## H2 数据库配置

默认 H2 数据库表将创建在 `$HOME` 目录，如：

```
yangjing@yangbajing:~$ tree fusion-discoveryx/
fusion-discoveryx/
└── db.mv.db

0 directories, 1 file
``` 
