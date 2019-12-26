# 用户管理

- gRPC服务地址：`/fusion.discoveryx.grpc.UserService`
- REST URL前缀：`/fusion/discoveryx/console/user`

REST URL路径由 **REST URL前缀** + 服务名组织，均使用 **POST** 方法的请求，JSON序例化格式。如查询实例接口访问地址为：`POST /fusion/discoveryx/console/sign/Login`。Protobuf与JSON格式转换请参阅： @ref[JSON 说明](../json.md)。

## Login

- REST URI：`POST /fusion/discoveryx/console/sign/Login`

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/server.proto) { #Login }

**请求**

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/user.proto) { #Login }

**响应**

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/user.proto) { #UserResponse }

`oneof`字段`logined`将返回登录结果和Session Token：

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/user.proto) { #Logined }

## Logout

- REST URI：`POST /fusion/discoveryx/console/sign/Logout`

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/server.proto) { #Logout }

**请求**

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/user.proto) { #Logout }

**响应**

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/user.proto) { #UserResponse }

## Current Session User

通过Session获取当前登录用户，REST API用户不需要传body（将从cookie及http header头中获取）。

- **REST URI**：`POST|GET /fusion/discoveryx/console/sign/` or `POST|GET /fusion/discoveryx/console/sign/CurrentSessionUser`

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/server.proto) { #CurrentSessionUser }

**请求**

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/user.proto) { #CurrentSessionUser }

**响应**

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/user.proto) { #UserResponse }

`oneof`字段`user`将返回已当前登录Session的用户

#### User

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/user.proto) { #User }

## ListUser

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/server.proto) { #ListUser }

**请求**

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/user.proto) { #ListUser }

**响应**

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/user.proto) { #UserResponse }

`oneof`字段`listed`将返回匹配的用户

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/user.proto) { #ListedUser }

## CreateUser

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/server.proto) { #CreateUser }

**请求**

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/user.proto) { #CreateUser }

**响应**

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/user.proto) { #UserResponse }

`oneof`字段`user`将返回已创建的用户

## ModifyUser

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/server.proto) { #ModifyUser }

**请求**

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/user.proto) { #ModifyUser }

**响应**

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/user.proto) { #UserResponse }

`oneof`字段`user`将返回已修改后的用户

## RemoveUser

**gRPC**

@@snip [gRPC](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/grpc/server.proto) { #RemoveUser }

**请求**

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/user.proto) { #RemoveUser }

**响应**

@@snip [model](../../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/user.proto) { #UserResponse }

`oneof`字段`user`将返回已删除的用户
