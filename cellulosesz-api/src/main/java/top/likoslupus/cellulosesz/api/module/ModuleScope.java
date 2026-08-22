package top.likoslupus.cellulosesz.api.module;

import top.likoslupus.cellulosesz.api.service.Registration;

public interface ModuleScope {

    String owner();

    <R extends Registration> R own(R registration);

    boolean closing();

}
