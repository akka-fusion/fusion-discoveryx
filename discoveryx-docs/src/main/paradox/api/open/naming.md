# Naming（服务发现）

- gRPC服务地址：`/fusion.discoveryx.grpc.NamingService`
- REST URL前缀：`/fusion/discoveryx/v1/naming`

REST URL路径由 **REST URL前缀** + 服务名组织，均使用 **POST** 方法的请求，JSON序例化格式。如查询实例接口访问地址为：`POST /fusion/discoveryx/v1/naming/QueryInstance`。Protobuf与JSON格式转换请参阅： @ref[JSON 说明](../json.md)。

### RegisterInstance

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/grpc/discoveryx.proto) { #RegisterInstance }

**请求**

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #InstanceRegister }

**响应**

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #NamingReply }

#### Instance
`oneof`的`instance`字段将返回已注册实例信息，如下：

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #Instance }

### RemoveInstance

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/grpc/discoveryx.proto) { #RemoveInstance }

**请求**

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #InstanceRemove }

**响应**

删除实例没有`oneof`字段返回。

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #NamingReply }

### QueryInstance

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/grpc/discoveryx.proto) { #QueryInstance }

**请求**

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #InstanceQuery }

**响应**

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #NamingReply }

查询成功`oneof`通过`queried`字段将查询匹配到的实例返回：

### ModifyInstance

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/grpc/discoveryx.proto) { #ModifyInstance }

**请求**

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #InstanceModify }

**响应**

@@snip [model](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/model/discoveryx.proto) { #NamingReply }

`oneof`的`instance`字段将返回修改后的实例信息。

### Heartbeat

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-common/src/main/protobuf/fusion/discoveryx/grpc/discoveryx.proto) { #Heartbeat }

`Heartbeat`接口第一次调用时需要通过`metadata`（HTTP Header）传递以下内容：

- `x-discoveryx-namespace`：命名空间 
- `x-discoveryx-service-name`：服务名
- `x-discoveryx-ip`：服务监听IP地址
- `x-discoveryx-port`：服务监听网络端口
- `x-discoveryx-instance-id`：实例ID
