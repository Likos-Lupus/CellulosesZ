package top.likoslupus.cellulosesz.core.module

import top.likoslupus.cellulosesz.api.event.EventRegistry
import top.likoslupus.cellulosesz.api.module.ModuleKey
import top.likoslupus.cellulosesz.api.service.ServiceRegistry
import top.likoslupus.cellulosesz.core.command.CommandMiddlewareRegistry
import top.likoslupus.cellulosesz.core.config.ConfigRegistry
import top.likoslupus.cellulosesz.core.lifecycle.legacy.AsyncInitializable
import top.likoslupus.cellulosesz.core.logging.CellulosesZLogger
import top.likoslupus.cellulosesz.core.runtime.CellulosesRuntime
import top.likoslupus.cellulosesz.core.scheduler.Scheduler
import java.nio.file.Path

class DefaultModuleContext(
    val key: ModuleKey,
    private val dataDirectory: Path,
    services: ServiceRegistry,
    configs: ConfigRegistry,
    events: EventRegistry,
    scheduler: Scheduler,
    middlewares: CommandMiddlewareRegistry,
    private val logger: CellulosesZLogger,
    private val enabledPredicate: (ModuleKey) -> Boolean,
    val scope: DefaultModuleScope,
) : ModuleContext {

    constructor(
        key: ModuleKey,
        dataDirectory: Path,
        services: ServiceRegistry,
        configs: ConfigRegistry,
        events: EventRegistry,
        scheduler: Scheduler,
        middlewares: CommandMiddlewareRegistry,
        logger: CellulosesZLogger,
        enabledPredicate: (ModuleKey) -> Boolean,
        runtime: CellulosesRuntime,
    ) : this(
        key = key,
        dataDirectory = dataDirectory,
        services = services,
        configs = configs,
        events = events,
        scheduler = scheduler,
        middlewares = middlewares,
        logger = logger,
        enabledPredicate = enabledPredicate,
        scope = runtime.createModuleScope(key.value),
    )

    private val scopedServices = ModuleScopedServiceRegistry(
        key.value,
        services,
        scope
    )
    private val scopedConfigs = ModuleScopedConfigRegistry(
        key.value,
        configs,
        scope
    )
    private val scopedEvents = ModuleScopedEventRegistry(
        key.value,
        events,
        scope
    )
    private val scopedScheduler = ModuleScopedScheduler(
        key.value,
        scheduler,
        scope
    )
    private val scopedMiddlewares = ModuleScopedCommandMiddlewareRegistry(
        key.value,
        middlewares,
        scope
    )

    override fun moduleId(): String = key.value

    override fun dataDirectory(): Path = dataDirectory

    override fun scope(): DefaultModuleScope = scope

    override fun services(): ServiceRegistry = scopedServices

    override fun configs(): ConfigRegistry = scopedConfigs

    override fun events(): EventRegistry = scopedEvents

    override fun scheduler(): Scheduler = scopedScheduler

    override fun middlewares(): CommandMiddlewareRegistry = scopedMiddlewares

    override fun logger(): CellulosesZLogger = logger

    override fun moduleEnabled(moduleId: String): Boolean =
        enabledPredicate(ModuleKey(moduleId))

    fun isModuleActive(targetKey: ModuleKey): Boolean =
        enabledPredicate(targetKey)

    fun initializables(): List<AsyncInitializable> =
        scopedServices.initializables()

}
