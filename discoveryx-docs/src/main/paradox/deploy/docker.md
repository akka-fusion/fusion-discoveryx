# Docker

## 单节点

Fusion DiscoveryX 提供了 Docker 构建脚本：

```
cd fusion-discoveryx/
sbt "project discoveryx-server" dist
scripts/dockers/fusion-discoveryx
docker build -t fusion-discoveryx .
```

## 集群

TODO
