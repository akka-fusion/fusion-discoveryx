# JSON 说明

## Protobuf、JSON转换说明

REST API使用JSON作为序例化，由Protobuf数据自动序例化为JSON（或反之）。

1. Protobuf里使用下划线定义的字段在序例化为JSON时将自动转换成驼峰格式；
2. 当某个字段未设置时，将使用Protobuf里定义的默认值。也就是说客户在提交请求时不需要每个字段都设置，同时，服务端返回结果时未设置的字段将使用默认值返回；
3. `oneof`类型字段在序例化为JSON时将不包含外层的包裹，将直接返回设置的字段。

## 示例

**protobuf**

@@snip [protocol](../../../../../discoveryx-server/src/main/protobuf/fusion/discoveryx/server/protocol/naming.proto) { #NamingResponse }

**json**

```json
{
    "status":200,
    "message":"",
    "serviceInfo":{
        "namespace":"namespace",
        "serviceName":"fusion-discoveryx",
        "groupName":"default",
        "instances":[
            {
                "instanceId":"127.0.0.1:8000",
                "ip":"127.0.0.1",
                "port":8000,
                "weight":0,
                "healthy":true,
                "enabled":false,
                "metadata":{

                }
            }
        ]
    }
}
```