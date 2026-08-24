package top.likoslupus.cellulosesz.core.command;

import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;

import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface CommandContinuation {

    CompletionStage<CommandOutcome> proceed();

}
