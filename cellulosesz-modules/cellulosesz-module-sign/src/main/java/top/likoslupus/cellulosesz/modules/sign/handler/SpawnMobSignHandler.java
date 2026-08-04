package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.api.entity.SpawnMobRequest;
import top.likoslupus.cellulosesz.api.entity.SpawnMobResult;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.sign.SynchronousSignHandler;
import top.likoslupus.cellulosesz.api.text.MessageArguments;

import static java.util.Objects.requireNonNull;

public final class SpawnMobSignHandler implements SynchronousSignHandler {

    private final EntityPlatformService entities;

    public SpawnMobSignHandler(EntityPlatformService entities) {
        this.entities = requireNonNull(entities, "entities");
    }

    @Override
    public String id() {
        return "SpawnMob";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        if (!entities.validLivingEntity(context.line(1))) {
            return SignUseResult.failure("service.sign.spawnmob-format");
        }

        if (!context.line(2).isBlank()
                && SignHandlerSupport.count(context.line(2), 1, 64).isEmpty()
        ) {
            return SignUseResult.failure("service.sign.spawnmob-format");
        }

        return context.line(3).isBlank()
                ? SignUseResult.success("service.sign.valid")
                : SignUseResult.failure("service.sign.spawnmob-format");
    }

    @Override
    public SignUseResult useSynchronously(SignUseContext context) {
        var count = (int) SignHandlerSupport
                .count(context.line(2), 1, 64)
                .orElse(1);
        var result = entities.spawnMob(new SpawnMobRequest(
                context.line(1),
                count,
                context.player()
        ));
        var spawned = result.value()
                .map(SpawnMobResult::spawned)
                .orElse(0);

        return result.successful() && spawned.equals(count)
                ?
                SignUseResult.success(
                        "service.sign.spawnmob-success",
                        MessageArguments.builder()
                                .put("count", count)
                                .put("entity", context.line(1))
                                .build()
                )
                : SignUseResult.failure(
                        "service.sign.spawnmob-failed",
                        MessageArguments.builder()
                                .put("spawned", spawned)
                                .put("count", count)
                                .build()
                );
    }

}
