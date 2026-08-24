package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.common.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.common.entity.SpawnMobRequest;
import top.likoslupus.cellulosesz.modules.sign.domain.SignUseContext;
import top.likoslupus.cellulosesz.modules.sign.domain.SignUseResult;

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
        var spawnValue = result.value();
        var spawned = spawnValue == null
                ? 0
                : spawnValue.spawned();

        return result.successful() && spawned == count
                ?
                SignUseResult.success(
                        "service.sign.spawnmob-success",
                        MessageArguments.builder()
                                .add(count)
                                .add(context.line(1))
                                .build()
                )
                : SignUseResult.failure(
                        "service.sign.spawnmob-failed",
                        MessageArguments.builder()
                                .add(spawned)
                                .add(count)
                                .build()
                );
    }

}
