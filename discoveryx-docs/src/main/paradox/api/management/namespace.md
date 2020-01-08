# NamespaceManagerService

- gRPC服务地址：`/fusion.discoveryx.grpc.NamingService`
- REST URL前缀：`/fusion/discoveryx/console/namespace`

REST URL路径由 **REST URL前缀** + 服务名组织，均使用 **POST** 方法的请求，JSON序例化格式。如查询实例接口访问地址为：`POST /fusion/discoveryx/console/management/ListNamespace`。Protobuf与JSON格式转换请参阅： @ref[JSON 说明](../json.md)。

## ListNamespace

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/server.proto) { #ListNamespace }

**请求**

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/namespace.proto) { #ListNamespace }

**响应**

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/namespace.proto) { #ManagementResponse }

`oneof`字段`listed`将返回匹配的namespace列表：

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/namespace.proto) { #ListedNamespace }

#### Namespace

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/namespace.proto) { #Namespace }

## CreateNamespace

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/server.proto) { #CreateNamespace }

**请求**

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/namespace.proto) { #CreateNamespace }

**响应**

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/namespace.proto) { #ManagementResponse }

`oneof`字段`namespace`将返回创建的namespace：

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/namespace.proto) { #Namespace }

## ModifyNamespace

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/server.proto) { #ModifyNamespace }

**请求**

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/namespace.proto) { #ModifyNamespace }

**响应**

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/namespace.proto) { #ManagementResponse }

`oneof`字段`namespace`将返回修改后的namespace：

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/namespace.proto) { #Namespace }

## RemoveNamespace

删除命名空间将导致此命名空间下所有配置（Config）和服务（Naming）不可用， **慎重**！

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/server.proto) { #RemoveNamespace }

**请求**

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/namespace.proto) { #RemoveNamespace }

**响应**

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/namespace.proto) { #ManagementResponse }
