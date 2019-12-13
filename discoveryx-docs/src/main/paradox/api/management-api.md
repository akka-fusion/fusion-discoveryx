# 管理API

Fusion DiscoveryX管理控制台使用的API接口

## Config Manager 配置管理服务

- gRPC服务地址：`/fusion.discoveryx.server.grpc.ConfigManagerService`
- REST URL前缀：`/fusion/discoveryx/management/config`

REST URL路径由 **REST URL前缀** + 服务名组织，均使用 **POST** 方法的请求，JSON序例化格式。如查询配置列表接口访问地址为：`POST /fusion/discoveryx/management/config/ListConfig`。Protobuf与JSON格式转换请参阅： @ref[JSON 说明](json.md)。

TODO

## Naming Manager 名称管理服务

- gRPC服务地址：`/fusion.discoveryx.server.grpc.NamingManagerService`
- REST URL前缀：`/fusion/discoveryx/management/naming`

REST URL路径由 **REST URL前缀** + 服务名组织，均使用 **POST** 方法的请求，JSON序例化格式。如查询服务列表接口访问地址为：`POST /fusion/discoveryx/management/naming/ListService`。Protobuf与JSON格式转换请参阅： @ref[JSON 说明](json.md)。

### ListService 查询服务列表

**gRPC**

@@snip [gRPC](../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/naming.proto) { #ListService }

**请求**

@@snip [protocol](../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/naming.proto) { #ListService }

**响应**

@@snip [protocol](../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/naming.proto) { #NamingResponse }

`oneof`的`listed_service`字段将返回匹配查询的服务列表：

@@snip [protocol](../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/naming.proto) { #ListedService }
@@snip [protocol](../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/naming.proto) { #ServiceInfo }

`Instance`见 @ref[开放API#instance](open-api.md#instance)。

### GetService 查询单个服务

**gRPC**

@@snip [gRPC](../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/naming.proto) { #GetService }

**请求**

@@snip [protocol](../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/naming.proto) { #GetService }

**响应**

@@snip [protocol](../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/naming.proto) { #NamingResponse }

`oneof`的`listed_service`字段将返回匹配查询的服务列表：

@@snip [protocol](../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/naming.proto) { #ServiceInfo }

### RemoveInstance 删除实例

**gRPC**

@@snip [gRPC](../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/naming.proto) { #RemoveInstance }

**请求**

@@snip [protocol](../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #InstanceRemove }

**响应**

@@snip [protocol](../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/naming.proto) { #NamingResponse }

### ModifyInstance 编辑实例

**gRPC**

@@snip [gRPC](../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/naming.proto) { #ModifyInstance }

**请求**

@@snip [protocol](../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #InstanceModify }

**响应**

@@snip [protocol](../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/naming.proto) { #NamingResponse }

`oneof`的`instance`将返回编辑后的实例信息，`Instance`见 @ref[开放API#instance](open-api.md#instance)。
