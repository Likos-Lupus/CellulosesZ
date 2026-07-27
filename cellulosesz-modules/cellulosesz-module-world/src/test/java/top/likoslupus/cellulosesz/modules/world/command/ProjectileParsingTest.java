package top.likoslupus.cellulosesz.modules.world.command;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.entity.ProjectileType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProjectileParsingTest {

    @Test
    void everyDocumentedProjectileHasStablePermissionToken() {
        assertEquals(ProjectileType.FIREBALL, FireballCommand.parse("fireball").orElseThrow());
        assertEquals(ProjectileType.SMALL, FireballCommand.parse("small").orElseThrow());
        assertEquals(ProjectileType.LARGE, FireballCommand.parse("large").orElseThrow());
        assertEquals(ProjectileType.ARROW, FireballCommand.parse("arrow").orElseThrow());
        assertEquals(ProjectileType.SKULL, FireballCommand.parse("skull").orElseThrow());
        assertEquals(ProjectileType.EGG, FireballCommand.parse("egg").orElseThrow());
        assertEquals(ProjectileType.SNOWBALL, FireballCommand.parse("snowball").orElseThrow());
        assertEquals(ProjectileType.EXPERIENCE_BOTTLE, FireballCommand.parse("expbottle").orElseThrow());
        assertEquals(ProjectileType.DRAGON, FireballCommand.parse("dragon").orElseThrow());
        assertEquals(ProjectileType.SPLASH_POTION, FireballCommand.parse("splashpotion").orElseThrow());
        assertEquals(ProjectileType.LINGERING_POTION, FireballCommand.parse("lingeringpotion").orElseThrow());
        assertEquals(ProjectileType.TRIDENT, FireballCommand.parse("trident").orElseThrow());
        assertTrue(FireballCommand.parse("not-an-entity").isEmpty());

        assertEquals("expbottle", FireballCommand.token(ProjectileType.EXPERIENCE_BOTTLE));
        assertEquals("splashpotion", FireballCommand.token(ProjectileType.SPLASH_POTION));
        assertEquals("lingeringpotion", FireballCommand.token(ProjectileType.LINGERING_POTION));
    }

}
