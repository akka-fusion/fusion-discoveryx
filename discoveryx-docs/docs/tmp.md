ConfigManagerServiceImpl:

```scala
  readJournal
    .eventsByTag(ConfigEntity.TypeKey.name, Offset.noOffset)
    .filter(event => event.event.isInstanceOf[ChangedConfigEvent])
    .mapConcat { event =>
      event.persistenceId.split("\\" + PersistenceId.DefaultSeparator) match {
        case Array(_, ConfigEntity.ConfigKey(configKey)) if event.event.isInstanceOf[ChangedConfigEvent] =>
          (configKey, event.event.asInstanceOf[ChangedConfigEvent]) :: Nil
        case _ => Nil
      }
    }
    .groupedWithin(1024, 5.seconds)
    .runForeach { list =>
      for ((namespace, items) <- list.groupBy(_._1.namespace)) {
        configManager ! ShardingEnvelope(namespace, InitialConfigKeyEvents(items.map {
          case (key, event) => ConfigKeyEvent(key, event)
        }))
      }
    }
```

ConfigManager:

```scala
      case InitialConfigKeyEvents(keyEvents) =>
        var tmpKeys = Vector.empty[ConfigKey]
        for (keyEvent <- keyEvents) {
          if (!dataIds.contains(keyEvent.key)) {
            tmpKeys :+= keyEvent.key
          }
          configEntity ! ShardingEnvelope(ConfigEntity.ConfigKey.makeEntityId(keyEvent.key), keyEvent.event.con)
        }
        dataIds ++= tmpKeys
        Behaviors.same

      .receiveSignal {
        case (_, Terminated(ref)) =>
          configRefs = configRefs.filterNot(_ == ref)
          Behaviors.same
      }
```
