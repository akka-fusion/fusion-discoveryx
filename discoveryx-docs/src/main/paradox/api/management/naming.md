# NamingManagerService

- gRPC服务地址：`/fusion.discoveryx.server.grpc.NamingManagerService`
- REST URL前缀：`/fusion/discoveryx/console/naming`

REST URL路径由 **REST URL前缀** + 服务名组织，均使用 **POST** 方法的请求，JSON序例化格式。如查询服务列表接口访问地址为：`POST /fusion/discoveryx/console/naming/ListService`。Protobuf与JSON格式转换请参阅： @ref[JSON 说明](../json.md)。

## ListService

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/server.proto) { #ListService }

**请求**

@@snip [protocol](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/naming.proto) { #ListService }

**响应**

@@snip [protocol](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/naming.proto) { #NamingResponse }

`oneof`的`listed_service`字段将返回匹配查询的服务列表：

@@snip [protocol](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/naming.proto) { #ListedService }
@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #ServiceInfo }

`Instance`见 @ref[开放API#instance](../open/naming.md#instance)。

## GetService

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/server.proto) { #GetService }

**请求**

@@snip [protocol](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/naming.proto) { #GetService }

**响应**

@@snip [protocol](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/naming.proto) { #NamingResponse }

`oneof`的`listed_service`字段将返回匹配查询的服务列表：

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #ServiceInfo }

## CreateService

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/server.proto) { #CreateService }

**请求**

@@snip [protocol](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/naming.proto) { #CreateService }

**响应**

@@snip [protocol](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/naming.proto) { #NamingResponse }

`oneof`的`listed_service`字段将返回匹配查询的服务列表：

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #ServiceInfo }

## ModifyService

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/server.proto) { #ModifyService }

**请求**

@@snip [protocol](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/naming.proto) { #ModifyService }

**响应**

@@snip [protocol](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/naming.proto) { #NamingResponse }

`oneof`的`listed_service`字段将返回匹配查询的服务列表：

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #ServiceInfo }

## RemoveService

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/server.proto) { #RemoveService }

**请求**

@@snip [protocol](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/naming.proto) { #RemoveService }

**响应**

@@snip [protocol](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/naming.proto) { #NamingResponse }

## RemoveInstance

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/server.proto) { #RemoveInstance }

**请求**

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #InstanceRemove }

**响应**

删除实例没有`oneof`字段返回。

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #NamingReply }

## ModifyInstance

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/server.proto) { #ModifyInstance }

**请求**

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #InstanceModify }

**响应**

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #NamingReply }

`oneof`的`instance`字段将返回修改后的实例信息。
