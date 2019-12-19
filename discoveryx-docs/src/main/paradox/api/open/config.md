# Config（配置管理）

- gRPC服务地址：`/fusion.discoveryx.grpc.ConfigService`
- REST URL前缀：`/fusion/discoveryx/v1/config`

REST URL路径由 **REST URL前缀** + 服务名组织，均使用 **POST** 方法的请求，JSON序例化格式。如查询配置接口访问地址为：`POST /fusion/discoveryx/v1/config/QueryConfig`。Protobuf与JSON格式转换请参阅： @ref[JSON 说明](../json.md)。

### GetConfig

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/grpc/discoveryx.proto) { #GetConfig }

**请求**

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #ConfigGet }

**响应**

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #ConfigReply }

`oneof`的`queried`字段将返回已注册实例信息，如下：

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #ConfigQueried }

### PublishConfig

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/grpc/discoveryx.proto) { #PublishConfig }

**请求**

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #ConfigItem }

**响应**

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #ConfigReply }

### RemoveConfig

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/grpc/discoveryx.proto) { #RemoveConfig }

**请求**

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #ConfigRemove }

**响应**

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #ConfigReply }

### ListenerConfig

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/grpc/discoveryx.proto) { #ListenerConfig }

**请求**

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #ConfigChangeListen }

**响应**

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #ConfigChanged }
@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #ChangeType }
