package top.likoslupus.cellulosesz.modules.sign;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.sign.SignService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.warp.WarpService;
import top.likoslupus.cellulosesz.modules.sign.handler.BuySignHandler;
import top.likoslupus.cellulosesz.modules.sign.handler.KitSignHandler;
import top.likoslupus.cellulosesz.modules.sign.handler.SellSignHandler;
import top.likoslupus.cellulosesz.modules.sign.handler.WarpSignHandler;
import top.likoslupus.cellulosesz.modules.sign.service.DefaultSignService;

@CellulosesModule(
        id = "sign",
        name = "Sign",
        description = "Interactive Warp, Buy, Sell, and Kit sign handlers.",
        phase = ModulePhase.FEATURE,
        requires = {"permission", "economy", "item", "teleport", "warp", "kit"}
)
public final class SignModule implements CellulosesZModule {

    private @Nullable SignConfig config;

    @Override
    public void registerConfigs(ModuleContext context) {
        config = context.configs().register(
                "module.sign",
                SignConfig.class,
                "modules/sign.yml",
                SignConfig::new
        );
    }

    @Override
    public void registerServices(ModuleContext context) {
        var permissions = context.services().require(PermissionService.class);
        var items = context.services().require(ItemService.class);
        var economy = context.services().require(EconomyService.class);
        var warps = context.services().require(WarpService.class);
        var teleports = context.services().require(TeleportService.class);
        var kits = context.services().require(KitService.class);

        var signs = new DefaultSignService(config, permissions);
        signs.register(new WarpSignHandler(warps, teleports, permissions));
        signs.register(new BuySignHandler(items, economy));
        signs.register(new SellSignHandler(items, economy));
        signs.register(new KitSignHandler(kits, permissions));
        context.services().register(SignService.class, signs);
        context.services().register(DefaultSignService.class, signs);
    }

}
