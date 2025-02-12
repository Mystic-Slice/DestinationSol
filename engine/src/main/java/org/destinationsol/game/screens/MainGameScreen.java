/*
 * Copyright 2018 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.destinationsol.game.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import org.destinationsol.Const;
import org.destinationsol.GameOptions;
import org.destinationsol.SolApplication;
import org.destinationsol.assets.Assets;
import org.destinationsol.common.SolColor;
import org.destinationsol.common.SolMath;
import org.destinationsol.game.DebugOptions;
import org.destinationsol.game.FactionManager;
import org.destinationsol.game.HardnessCalc;
import org.destinationsol.game.Hero;
import org.destinationsol.game.SolCam;
import org.destinationsol.game.SolGame;
import org.destinationsol.game.SolObject;
import org.destinationsol.game.context.Context;
import org.destinationsol.game.item.Gun;
import org.destinationsol.game.item.ItemManager;
import org.destinationsol.game.item.Shield;
import org.destinationsol.game.item.SolItem;
import org.destinationsol.game.planet.Planet;
import org.destinationsol.game.ship.ShipAbility;
import org.destinationsol.game.ship.SolShip;
import org.destinationsol.ui.DisplayDimensions;
import org.destinationsol.ui.FontSize;
import org.destinationsol.ui.SolInputManager;
import org.destinationsol.ui.SolUiBaseScreen;
import org.destinationsol.ui.SolUiControl;
import org.destinationsol.ui.SolUiScreen;
import org.destinationsol.ui.UiDrawer;
import org.destinationsol.ui.nui.screens.ConsoleScreen;
import org.destinationsol.ui.nui.screens.UIShipControlsScreen;
import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.nui.asset.UIElement;

import java.util.ArrayList;
import java.util.List;

public class MainGameScreen extends SolUiBaseScreen {
    // TODO: Rename!
    private static final float ICON_SZ = .03f;
    private static final float BAR_SZ = ICON_SZ * 5;
    private static final int MAX_ICON_COUNT = 3;
    static final float CELL_SZ = .17f;
    private static final float H_PAD = .005f;
    private static final float V_PAD = H_PAD;
    static final float HELPER_ROW_1 = 1 - 3f * CELL_SZ;
    private static final float HELPER_ROW_2 = HELPER_ROW_1 - .5f * CELL_SZ;
    private static final float HELPER_ROW_3 = HELPER_ROW_2 - .5f * CELL_SZ;

    private final ShipUiControl shipControl;
    private final SolUiControl freeCamControl;
    private final SolUiControl pauseControl;
    private final CameraKeyboardControl cameraControl;

    private final ZoneNameAnnouncer zoneNameAnnouncer;
    private final BorderDrawer borderDrawer;

    private final TextureAtlas.AtlasRegion lifeTexture;
    private final TextureAtlas.AtlasRegion infinityTexture;
    private final TextureAtlas.AtlasRegion waitTexture;
    private final TextureAtlas.AtlasRegion compassTexture;

    private final Color myCompassTint;
    private final TextPlace myLifeTp;
    private final TextPlace myRepairsExcessTp;
    private final TextPlace myShieldLifeTp;
    private final TextPlace myG1AmmoTp;
    private final TextPlace myG1AmmoExcessTp;
    private final TextPlace myG2AmmoTp;
    private final TextPlace myG2AmmoExcessTp;
    private final TextPlace myChargesExcessTp;
    private final TextPlace myMoneyExcessTp;
    private final SolApplication solApplication;
    private final Context context;

    private List<SolUiScreen> gameOverlayScreens = new ArrayList<>();
    private List<WarnDrawer> warnDrawers = new ArrayList<>();

    MainGameScreen(RightPaneLayout rightPaneLayout, Context context) {
        DisplayDimensions displayDimensions = SolApplication.displayDimensions;

        this.context = context;
        solApplication = context.get(SolApplication.class);
        GameOptions gameOptions = solApplication.getOptions();

        switch (gameOptions.controlType) {
            case KEYBOARD:
                UIShipControlsScreen shipControlsScreen =
                        (UIShipControlsScreen) solApplication.getNuiManager().createScreen("engine:uiShipControlsScreen");
                shipControl = shipControlsScreen;
                break;
            case MOUSE:
                shipControl = new ShipMouseControl();
                break;
            case CONTROLLER:
                shipControl = new ShipControllerControl(solApplication);
                break;
            case MIXED:
            default:
                shipControl = new ShipMixedControl(solApplication, controls);
                break;
        }

        boolean mobile = solApplication.isMobile();
        float lastCol = displayDimensions.getRatio() - MainGameScreen.CELL_SZ;
        // No button, since on mobile, it should be ideally controlled straightly by dragging.
        freeCamControl = new SolUiControl(null, false, gameOptions.getKeyFreeCameraMovement());
        controls.add(freeCamControl);
        pauseControl = new SolUiControl(null, true, gameOptions.getKeyPause());
        controls.add(pauseControl);
        cameraControl = new CameraKeyboardControl(gameOptions, controls);

        // possible warning messages in order of importance, so earlier one will be drawn on the center
        warnDrawers.add(new SunWarnDrawer());
        warnDrawers.add(new DmgWarnDrawer());
        warnDrawers.add(new CollisionWarnDrawer());
        warnDrawers.add(new NoShieldWarn());
        warnDrawers.add(new NoArmorWarn());
        warnDrawers.add(new EnemyWarn());

        zoneNameAnnouncer = new ZoneNameAnnouncer();
        borderDrawer = new BorderDrawer();

        lifeTexture = Assets.getAtlasRegion("engine:iconLife");
        infinityTexture = Assets.getAtlasRegion("engine:iconInfinity");
        waitTexture = Assets.getAtlasRegion("engine:iconWait");

        compassTexture = Assets.getAtlasRegion("engine:uiCompass");
        myCompassTint = SolColor.col(1, 0);

        myLifeTp = new TextPlace(SolColor.W50);
        myRepairsExcessTp = new TextPlace(SolColor.WHITE);
        myShieldLifeTp = new TextPlace(SolColor.W50);
        myG1AmmoTp = new TextPlace(SolColor.W50);
        myG1AmmoExcessTp = new TextPlace(SolColor.WHITE);
        myG2AmmoTp = new TextPlace(SolColor.W50);
        myG2AmmoExcessTp = new TextPlace(SolColor.WHITE);
        myChargesExcessTp = new TextPlace(SolColor.WHITE);
        myMoneyExcessTp = new TextPlace(SolColor.WHITE);
    }

    @Override
    public void onAdd(SolApplication solApplication) {
        super.onAdd(solApplication);
        if (solApplication.getOptions().controlType == GameOptions.ControlType.KEYBOARD) {
            UIShipControlsScreen uiControls = (UIShipControlsScreen) shipControl;
            if (!solApplication.getNuiManager().hasScreen(uiControls)) {
                solApplication.getNuiManager().pushScreen(uiControls);
            }
        }
    }

    public static Rectangle btn(float x, float y, boolean halfHeight) {
        float gap = .01f;
        float cellH = CELL_SZ;
        if (halfHeight) {
            cellH /= 2;
        }
        return new Rectangle(x + gap, y + gap, CELL_SZ - gap * 2, cellH - gap * 2);
    }

    private void maybeDrawHeight(UiDrawer drawer) {
        SolGame game = solApplication.getGame();
        Planet np = game.getPlanetManager().getNearestPlanet();
        SolCam cam = context.get(SolCam.class);
        Vector2 camPos = cam.getPosition();
        if (np != null && np.getPosition().dst(camPos) < np.getFullHeight()) {
            drawHeight(drawer, np, camPos, cam.getAngle());
        }
    }

    private void drawHeight(UiDrawer drawer, Planet np, Vector2 camPos, float camAngle) {
        float toPlanet = camPos.dst(np.getPosition());
        toPlanet -= np.getGroundHeight();
        if (Const.ATM_HEIGHT < toPlanet) {
            return;
        }
        float perc = toPlanet / Const.ATM_HEIGHT;
        float sz = .08f;
        float maxY = 1 - sz / 2;
        float y = 1 - perc;
        myCompassTint.a = SolMath.clamp(1.5f * y);
        if (maxY < y) {
            y = maxY;
        }
        float angle = np.getAngle() - camAngle;
        drawer.draw(compassTexture, sz, sz, sz / 2, sz / 2, sz / 2, y, angle, myCompassTint);
    }

    @Override
    public void updateCustom(SolApplication solApplication, SolInputManager.InputPointer[] inputPointers, boolean clickedOutside) {
        if (DebugOptions.PRINT_BALANCE) {
            solApplication.finishGame();
            return;
        }
        SolGame game = solApplication.getGame();
        SolInputManager inputMan = solApplication.getInputManager();
        GameScreens screens = game.getScreens();
        Hero hero = game.getHero();

        for (WarnDrawer warnDrawer : warnDrawers) {
            warnDrawer.update(game);
        }

        zoneNameAnnouncer.update(game, context);

        boolean controlsEnabled = inputMan.getTopScreen() == this;
        shipControl.update(solApplication, controlsEnabled);

        SolCam.DIRECT_CAM_CONTROL = freeCamControl.isOn();

        if (solApplication.getNuiManager().hasScreenOfType(ConsoleScreen.class)) {
            controls.forEach(x -> x.setEnabled(false));
        } else if (!inputMan.isScreenOn(screens.menuScreen)) {
            game.setPaused(false);
            controls.forEach(x -> x.setEnabled(true));
        }

        if (pauseControl.isJustOff()) {
            game.setPaused(!game.isPaused());
        }

        for (SolUiScreen screen : gameOverlayScreens) {
            screen.updateCustom(solApplication, inputPointers, clickedOutside);
        }
    }

    private boolean drawGunStat(UiDrawer uiDrawer, Hero hero, boolean secondary, float col0, float col1,
                                float col2, float y) {
        Gun g = hero.getHull().getGun(secondary);
        if (g == null) {
            return false;
        }
        TextureAtlas.AtlasRegion tex = g.config.icon;

        uiDrawer.draw(tex, ICON_SZ, ICON_SZ, 0, 0, col0, y, 0, SolColor.WHITE);
        float curr;
        float max;
        if (g.reloadAwait > 0) {
            max = g.config.reloadTime;
            curr = max - g.reloadAwait;
        } else {
            curr = g.ammo;
            max = g.config.clipConf.size;
        }
        TextPlace ammoTp = g.reloadAwait > 0 ? null : secondary ? myG2AmmoTp : myG1AmmoTp;
        drawBar(uiDrawer, col1, y, curr, max, ammoTp);
        if (g.reloadAwait > 0) {
            drawWait(uiDrawer, col1, y);
        }
        if (!g.config.clipConf.infinite) {
            int clipCount = hero.getItemContainer().count(g.config.clipConf.example);
            drawIcons(uiDrawer, col2, y, clipCount, g.config.clipConf.icon, secondary ? myG2AmmoExcessTp : myG1AmmoExcessTp);
        } else {
            uiDrawer.draw(infinityTexture, ICON_SZ, ICON_SZ, 0, 0, col2, y, 0, SolColor.WHITE);
        }
        return true;
    }

    private void drawWait(UiDrawer uiDrawer, float x, float y) {
        uiDrawer.draw(waitTexture, ICON_SZ, ICON_SZ, ICON_SZ / 2, ICON_SZ / 2, x + BAR_SZ / 2, y + ICON_SZ / 2, 0, SolColor.WHITE);
    }

    private void drawBar(UiDrawer uiDrawer, float x, float y, float curr, float max, TextPlace tp) {
        float perc = curr / max;
        uiDrawer.draw(uiDrawer.whiteTexture, BAR_SZ, ICON_SZ, 0, 0, x, y, 0, SolColor.UI_DARK);
        uiDrawer.draw(uiDrawer.whiteTexture, BAR_SZ * perc, ICON_SZ, 0, 0, x, y, 0, SolColor.UI_LIGHT);
        if (tp != null && max > 1 && curr > 0) {
            tp.text = (int) curr + "/" + (int) max;
            tp.position.set(x + BAR_SZ / 2, y + ICON_SZ / 2);
        }
    }

    private void drawIcons(UiDrawer uiDrawer, float x, float y, int count, TextureAtlas.AtlasRegion tex,
                           TextPlace textPlace) {
        int excess = count - MAX_ICON_COUNT;
        int iconCount = excess > 0 ? MAX_ICON_COUNT : count;
        for (int i = 0; i < iconCount; i++) {
            uiDrawer.draw(tex, ICON_SZ, ICON_SZ, 0, 0, x, y, 0, SolColor.WHITE);
            x += ICON_SZ + H_PAD;
        }
        if (excess > 0) {
            updateTextPlace(x, y, "+" + excess, textPlace);
        }
    }

    private void updateTextPlace(float x, float y, String text, TextPlace textPlace) {
        textPlace.text = text;
        textPlace.position.set(x + ICON_SZ / 2, y + ICON_SZ / 2);
    }

    @Override
    public void drawImages(UiDrawer uiDrawer, SolApplication solApplication) {
        myLifeTp.text = null;
        myRepairsExcessTp.text = null;
        myShieldLifeTp.text = null;
        myG1AmmoTp.text = null;
        myG1AmmoExcessTp.text = null;
        myG2AmmoTp.text = null;
        myG2AmmoExcessTp.text = null;
        myChargesExcessTp.text = null;
        myMoneyExcessTp.text = null;

        maybeDrawHeight(uiDrawer);
        borderDrawer.draw(uiDrawer, solApplication, context);

        SolGame game = solApplication.getGame();
        Hero hero = game.getHero();
        if (hero.isNonTranscendent()) {
            float row = BorderDrawer.PLANET_PROXIMITY_INDICATOR_SIZE + V_PAD;
            float col0 = BorderDrawer.PLANET_PROXIMITY_INDICATOR_SIZE + H_PAD;
            float col1 = col0 + ICON_SZ + H_PAD;
            float col2 = col1 + BAR_SZ + H_PAD;

            Shield shield = hero.getShield();
            if (shield != null) {
                uiDrawer.draw(shield.getIcon(game), ICON_SZ, ICON_SZ, 0, 0, col0, row, 0, SolColor.WHITE);
                drawBar(uiDrawer, col1, row, MathUtils.floor(shield.getLife()), shield.getMaxLife(), myShieldLifeTp);
                row += ICON_SZ + V_PAD;
            }

            uiDrawer.draw(lifeTexture, ICON_SZ, ICON_SZ, 0, 0, col0, row, 0, SolColor.WHITE);
            drawBar(uiDrawer, col1, row, MathUtils.floor(hero.getLife()), hero.getHull().config.getMaxLife(), myLifeTp);
            int repairKitCount = hero.getItemContainer().count(game.getItemMan().getRepairExample());
            ItemManager itemManager = game.getItemMan();
            drawIcons(uiDrawer, col2, row, repairKitCount, itemManager.repairIcon, myRepairsExcessTp);

            row += ICON_SZ + V_PAD;
            boolean consumed = drawGunStat(uiDrawer, hero, false, col0, col1, col2, row);
            if (consumed) {
                row += ICON_SZ + V_PAD;
            }
            consumed = drawGunStat(uiDrawer, hero, true, col0, col1, col2, row);
            if (consumed) {
                row += ICON_SZ + V_PAD;
            }

            ShipAbility ability = hero.getAbility();
            SolItem abilityChargeEx = ability == null ? null : ability.getConfig().getChargeExample();
            if (abilityChargeEx != null) {
                int abilityChargeCount = hero.getItemContainer().count(abilityChargeEx);
                TextureAtlas.AtlasRegion icon = abilityChargeEx.getIcon(game);
                uiDrawer.draw(icon, ICON_SZ, ICON_SZ, 0, 0, col0, row, 0, SolColor.WHITE);
                float chargePercentage = 1 - SolMath.clamp(hero.getAbilityAwait() / ability.getConfig().getRechargeTime());
                drawBar(uiDrawer, col1, row, chargePercentage, 1, null);
                if (chargePercentage < 1) {
                    drawWait(uiDrawer, col1, row);
                }
                drawIcons(uiDrawer, col2, row, abilityChargeCount, icon, myChargesExcessTp);
                row += ICON_SZ + V_PAD;
            }
            uiDrawer.draw(game.getItemMan().moneyIcon, ICON_SZ, ICON_SZ, 0, 0, col0, row, 0, SolColor.WHITE);
            myMoneyExcessTp.text = Integer.toString(Math.round(hero.getMoney()));
            myMoneyExcessTp.position.set(col1, row + ICON_SZ / 2);
            //updateTextPlace(col1, row, (int) hero.getMoney() + "", myMoneyExcessTp);
        }

        int drawPlace = 0;
        for (WarnDrawer wd : warnDrawers) {
            if (wd.drawPercentage > 0) {
                wd.draw(uiDrawer, drawPlace++);
            }
        }

        for (SolUiScreen screen : gameOverlayScreens) {
            screen.drawImages(uiDrawer, solApplication);
        }
    }

    @Override
    public void drawText(UiDrawer uiDrawer, SolApplication solApplication) {
        myLifeTp.draw(uiDrawer);
        myRepairsExcessTp.draw(uiDrawer);
        myShieldLifeTp.draw(uiDrawer);
        myG1AmmoTp.draw(uiDrawer);
        myG1AmmoExcessTp.draw(uiDrawer);
        myG2AmmoTp.draw(uiDrawer);
        myG2AmmoExcessTp.draw(uiDrawer);
        myChargesExcessTp.draw(uiDrawer);
        myMoneyExcessTp.draw(uiDrawer, UiDrawer.TextAlignment.LEFT);

        int drawPlace = 0;
        for (WarnDrawer warnDrawer : warnDrawers) {
            if (warnDrawer.drawPercentage > 0) {
                warnDrawer.drawText(uiDrawer, drawPlace++);
            }
        }

        zoneNameAnnouncer.drawText(uiDrawer);

        for (SolUiScreen screen : gameOverlayScreens) {
            screen.drawText(uiDrawer, solApplication);
        }
    }

    @Override
    public void blurCustom(SolApplication solApplication) {
        shipControl.blur();

        for (SolUiScreen screen : gameOverlayScreens) {
            screen.blurCustom(solApplication);
        }
    }

    public ShipUiControl getShipControl() {
        return shipControl;
    }

    public boolean isCameraUp() {
        return cameraControl.isUp();
    }

    public boolean isCameraDown() {
        return cameraControl.isDown();
    }

    public boolean isCameraLeft() {
        return cameraControl.isLeft();
    }

    public boolean isCameraRight() {
        return cameraControl.isRight();
    }

    public void addOverlayScreen(SolUiScreen screen) {
        gameOverlayScreens.add(screen);
        screen.onAdd(solApplication);
        controls.addAll(screen.getControls());
    }

    public void removeOverlayScreen(SolUiScreen screen) {
        gameOverlayScreens.remove(screen);
        controls.removeAll(screen.getControls());
    }

    public boolean hasOverlay(SolUiScreen screen) {
        return gameOverlayScreens.contains(screen);
    }

    public void addWarnDrawer(WarnDrawer drawer) {
        if (!warnDrawers.contains(drawer)) {
            warnDrawers.add(drawer);
        }
    }

    public void removeWarnDrawer(WarnDrawer drawer) {
        warnDrawers.remove(drawer);
    }

    public boolean hasWarnDrawer(WarnDrawer drawer) {
        return warnDrawers.contains(drawer);
    }

    public static class TextPlace {
        public final Color color;
        public String text;
        public Vector2 position = new Vector2();

        TextPlace(Color col) {
            color = new Color(col);
        }

        public void draw(UiDrawer uiDrawer) {
            uiDrawer.drawString(text, position.x, position.y, FontSize.HUD, true, color);
        }

        public void draw(UiDrawer uiDrawer, UiDrawer.TextAlignment align) {
            uiDrawer.drawString(text, position.x, position.y, FontSize.HUD, align, true, color);
        }
    }

    private static class NoShieldWarn extends WarnDrawer {
        NoShieldWarn() {
            super("No Shield");
        }

        protected boolean shouldWarn(SolGame game) {
            Hero hero = game.getHero();
            return hero.isNonTranscendent() && hero.getShield() == null;
        }
    }

    private static class NoArmorWarn extends WarnDrawer {
        NoArmorWarn() {
            super("No Armor");
        }

        protected boolean shouldWarn(SolGame game) {
            Hero hero = game.getHero();
            return hero.isNonTranscendent() && hero.getArmor() == null;
        }
    }

    private static class EnemyWarn extends WarnDrawer {
        EnemyWarn() {
            super("Dangerous\nEnemy");
        }

        protected boolean shouldWarn(SolGame game) {
            Hero hero = game.getHero();
            if (hero.isTranscendent()) {
                return false;
            }

            float heroCap = HardnessCalc.getShipDmgCap(hero.getShip());
            List<SolObject> objs = game.getObjectManager().getObjects();
            FactionManager fm = game.getFactionMan();
            SolCam cam = game.getContext().get(SolCam.class);
            float viewDist = cam.getViewDistance();
            float dps = 0;

            for (SolObject o : objs) {
                if (!(o instanceof SolShip)) {
                    continue;
                }

                SolShip ship = (SolShip) o;

                if (viewDist < ship.getPosition().dst(hero.getPosition())) {
                    continue;
                }

                if (!fm.areEnemies(hero.getShip(), ship)) {
                    continue;
                }

                dps += HardnessCalc.getShipDps(ship);

                if (HardnessCalc.isDangerous(heroCap, dps)) {
                    return true;
                }
            }

            return false;
        }
    }
}

