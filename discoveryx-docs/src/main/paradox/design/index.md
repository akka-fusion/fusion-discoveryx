# 设计

Fusion DiscoveryX 完全基于 Akka 开放，使用 Actor 模型来处理各类异步任务。通过 Akka Persistence 来实现事件存储和状态保持，通过 gRPC 提供客户端接口协议。

@@toc { depth=3 }

@@@ index

- [concept](concept.md)
- [architecture](architecture.md)
- [config](config.md)
- [naming](naming.md)
- [technology](technology.md)

@@@
