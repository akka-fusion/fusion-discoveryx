#!/bin/sh

# 若无法执行 运行命令 chmod +x deploy.sh

# 当前时间
date=`date +%F`
# 编译后资源所在的文件名
dist_name="dist"
# 压缩后的文件名
file_name="dist-${date}.tar.gz"
# 服务器用户名
user="hl"
# 服务器地址
host="dn5"
# 服务器上资源所在的路劲
pwd="/home/app/frontend/recommender-app/"

# 删除之前的文件
rm -rf ${dist_name}
rm -rf *.tar.gz

# 构建项目
npm run build

# 压缩打包
tar -zcvf ${file_name} ${dist_name}

# 上传到服务器
scp ${file_name} ${user}@${host}:${pwd}

# 登录到目标服务器并发布
ssh ${user}@${host} "cd ${pwd};tar -zxvf ${file_name} ${dist_name}"

echo "发布成功"