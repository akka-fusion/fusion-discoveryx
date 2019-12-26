# Config配置管理

- gRPC服务地址：`/fusion.discoveryx.server.grpc.ConfigManagerService`
- REST URL前缀：`/fusion/discoveryx/console/config`

REST URL路径由 **REST URL前缀** + 服务名组织，均使用 **POST** 方法的请求，JSON序例化格式。如查询配置列表接口访问地址为：`POST /fusion/discoveryx/management/config/ListConfig`。Protobuf与JSON格式转换请参阅： @ref[JSON 说明](../json.md)。

## ListConfig

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/server.proto) { #ListConfig }

**请求**

@@snip [protocol](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/config.proto) { #ListConfig }

**响应**

@@snip [protocol](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/config.proto) { #ConfigResponse }

`oneof`字段`listed`有效：

@@snip [protocol](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #ConfigQueried }

## GetConfig

见 @ref[GetConfig](../open/config.md#getconfig) 。

## PublishConfig

见 @ref[PublishConfig](../open/config.md#publishconfig) 。

## RemoveConfig

见 @ref[RemoveConfig](../open/config.md#removeconfig) 。
