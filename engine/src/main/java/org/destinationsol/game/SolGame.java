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
package org.destinationsol.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Timer;
import org.destinationsol.CommonDrawer;
import org.destinationsol.Const;
import org.destinationsol.GameOptions;
import org.destinationsol.SolApplication;
import org.destinationsol.assets.Assets;
import org.destinationsol.assets.sound.OggSoundManager;
import org.destinationsol.assets.sound.SpecialSounds;
import org.destinationsol.common.DebugCol;
import org.destinationsol.common.SolException;
import org.destinationsol.common.SolMath;
import org.destinationsol.common.SolRandom;
import org.destinationsol.entitysystem.EntitySystemManager;
import org.destinationsol.entitysystem.SerialisationManager;
import org.destinationsol.files.HullConfigManager;
import org.destinationsol.game.asteroid.AsteroidBuilder;
import org.destinationsol.game.attributes.RegisterUpdateSystem;
import org.destinationsol.game.chunk.ChunkManager;
import org.destinationsol.game.context.Context;
import org.destinationsol.game.drawables.DrawableDebugger;
import org.destinationsol.game.drawables.DrawableManager;
import org.destinationsol.game.farBg.FarBackgroundManagerOld;
import org.destinationsol.game.item.ItemManager;
import org.destinationsol.game.item.LootBuilder;
import org.destinationsol.game.item.MercItem;
import org.destinationsol.game.item.SolItem;
import org.destinationsol.game.maze.Maze;
import org.destinationsol.game.maze.MazeConfigManager;
import org.destinationsol.game.particle.EffectTypes;
import org.destinationsol.game.particle.PartMan;
import org.destinationsol.game.particle.SpecialEffects;
import org.destinationsol.game.planet.BeltConfigManager;
import org.destinationsol.game.planet.Planet;
import org.destinationsol.game.planet.PlanetConfigManager;
import org.destinationsol.game.planet.PlanetManager;
import org.destinationsol.game.planet.SolarSystem;
import org.destinationsol.game.planet.SolarSystemConfigManager;
import org.destinationsol.game.planet.SunSingleton;
import org.destinationsol.game.planet.SystemBelt;
import org.destinationsol.game.screens.GameScreens;
import org.destinationsol.game.ship.ShipAbility;
import org.destinationsol.game.ship.ShipBuilder;
import org.destinationsol.game.ship.SloMo;
import org.destinationsol.game.ship.hulls.HullConfig;
import org.destinationsol.mercenary.MercenaryUtils;
import org.destinationsol.modules.ModuleManager;
import org.destinationsol.ui.DebugCollector;
import org.destinationsol.ui.TutorialManager;
import org.destinationsol.ui.UiDrawer;
import org.destinationsol.ui.Waypoint;
import org.destinationsol.ui.nui.screens.MainGameScreen;
import org.destinationsol.util.InjectionHelper;
import org.destinationsol.world.GalaxyBuilder;
import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.gestalt.entitysystem.entity.EntityRef;
import org.terasology.nui.asset.UIElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class SolGame {
    private static final String NUI_MAIN_GAME_SCREEN_DESKTOP_URI = "engine:mainGameScreen_desktop";
    private static final String NUI_MAIN_GAME_SCREEN_MOBILE_URI = "engine:mainGameScreen_mobile";

    private final GameScreens gameScreens;
    private final SolCam solCamReference;
    private final ObjectManager objectManager;
    private final boolean isTutorial;
    private final SolApplication solApplication;
    private final PlanetManager planetManager;
    private final ChunkManager chunkManager;
    private final PartMan partMan;
    private final AsteroidBuilder asteroidBuilder;
    private final LootBuilder lootBuilder;
    private final ShipBuilder shipBuilder;
    private final HullConfigManager hullConfigManager;
    private final GridDrawer gridDrawer;
    private final FarBackgroundManagerOld farBackgroundManagerOld;
    private final FactionManager factionManager;
    private final MapDrawer mapDrawer;
    private final RubbleBuilder rubbleBuilder;
    private final ItemManager itemManager;
    private final StarPort.Builder starPortBuilder;
    private final OggSoundManager soundManager;
    private final DrawableDebugger drawableDebugger;
    private final SpecialEffects specialEffects;
    private final BeaconHandler beaconHandler;
    private final MountDetectDrawer mountDetectDrawer;
    private final TutorialManager tutorialManager;
    private final GalaxyFiller galaxyFiller;
    private final SolContactListener contactListener;
    private final GalaxyBuilder galaxyBuilder;
    private final SolarSystemConfigManager solarSystemConfigManager;
    private final PlanetConfigManager planetConfigManager;
    private final MazeConfigManager mazeConfigManager;
    private final BeltConfigManager beltConfigManager;
    private final MainGameScreen mainGameScreen;
    private Hero hero;
    private float timeStep;
    private float time;
    private boolean paused;
    private float timeFactor;
    private RespawnState respawnState;
    private SortedMap<Integer, List<UpdateAwareSystem>> onPausedUpdateSystems;
    private SortedMap<Integer, List<UpdateAwareSystem>> updateSystems;
    private Context context;

    private EntitySystemManager entitySystemManager;

    public SolGame(String shipName, boolean isTutorial, boolean isNewGame, CommonDrawer commonDrawer, Context context,
                   WorldConfig worldConfig) {
        // TODO: make this non-static
        FactionInfo.init();

        this.context = context;

        this.isTutorial = isTutorial;
        solApplication = context.get(SolApplication.class);

        //TODO this no longer needs to be instantiated in SolGame
        GameColors gameColors = new GameColors();
        context.put(GameColors.class, gameColors);

        //TODO this no longer needs to be instantiated in SolGame
        soundManager = solApplication.getSoundManager();
        SpecialSounds specialSounds = new SpecialSounds(soundManager);
        context.put(SpecialSounds.class, specialSounds);

        //TODO these no longer need to be instantiated in SolGame
        GameDrawer drawer = new GameDrawer(commonDrawer);
        context.put(GameDrawer.class, drawer);
        DrawableManager drawableManager = new DrawableManager(drawer);
        context.put(DrawableManager.class, drawableManager);

        //TODO refactor the usages of this to be independent from SolGame, so that this can be deleted
        // The only methods still using this are those that require changing an interface signature (UpdateAwareSystem, SolObject, and Drawable)
        SolCam camera = context.get(SolCam.class);
        solCamReference = camera;

        gameScreens = new GameScreens(solApplication, context);

        boolean isMobile = solApplication.isMobile();
        if (!isMobile) {
            mainGameScreen = (MainGameScreen) solApplication.getNuiManager().createScreen(NUI_MAIN_GAME_SCREEN_DESKTOP_URI);
        } else {
            mainGameScreen = (MainGameScreen) solApplication.getNuiManager().createScreen(NUI_MAIN_GAME_SCREEN_MOBILE_URI);
        }

        if (isTutorial) {
            tutorialManager = new TutorialManager(gameScreens, mainGameScreen, isMobile, solApplication.getOptions(), this);
            context.put(TutorialManager.class, tutorialManager);
        } else {
            tutorialManager = null;
        }
        farBackgroundManagerOld = new FarBackgroundManagerOld();
        shipBuilder = new ShipBuilder();
        EffectTypes effectTypes = new EffectTypes();
        specialEffects = new SpecialEffects(effectTypes, gameColors);
        itemManager = new ItemManager(soundManager, effectTypes, gameColors);
        AbilityCommonConfigs abilityCommonConfigs = new AbilityCommonConfigs(effectTypes, gameColors, soundManager);
        hullConfigManager = new HullConfigManager(itemManager, abilityCommonConfigs);
        planetManager = new PlanetManager();

        contactListener = new SolContactListener(this);
        factionManager = new FactionManager();
        objectManager = new ObjectManager(contactListener, factionManager, context);
        context.put(World.class, objectManager.getWorld());
        gridDrawer = new GridDrawer();
        chunkManager = new ChunkManager();
        partMan = new PartMan();
        asteroidBuilder = new AsteroidBuilder();
        lootBuilder = new LootBuilder();
        mapDrawer = new MapDrawer();
        rubbleBuilder = new RubbleBuilder();
        context.put(RubbleBuilder.class, rubbleBuilder);
        galaxyFiller = new GalaxyFiller(hullConfigManager);
        starPortBuilder = new StarPort.Builder();
        drawableDebugger = new DrawableDebugger();
        mountDetectDrawer = new MountDetectDrawer();
        beaconHandler = new BeaconHandler();

        solarSystemConfigManager = new SolarSystemConfigManager(hullConfigManager, itemManager);
        context.put(SolarSystemConfigManager.class, solarSystemConfigManager);
        planetConfigManager = new PlanetConfigManager(hullConfigManager, gameColors, itemManager);
        planetConfigManager.loadDefaultPlanetConfigs();
        context.put(PlanetConfigManager.class, planetConfigManager);
        mazeConfigManager = new MazeConfigManager(hullConfigManager, itemManager);
        mazeConfigManager.loadDefaultMazeConfigs();
        context.put(MazeConfigManager.class, mazeConfigManager);
        beltConfigManager = new BeltConfigManager(hullConfigManager, itemManager);
        beltConfigManager.loadDefaultBeltConfigs();
        context.put(BeltConfigManager.class, beltConfigManager);
        galaxyBuilder = new GalaxyBuilder(context, worldConfig.getNumberOfSystems());
        context.put(GalaxyBuilder.class, galaxyBuilder);

        timeFactor = 1;
    }

    public void createUpdateSystems(Context context) {
        ModuleManager moduleManager = context.get(ModuleManager.class);

        // the ordering of update aware systems is very important, switching them up can cause bugs!
        updateSystems = new TreeMap<Integer, List<UpdateAwareSystem>>();
        List<UpdateAwareSystem> defaultSystems = new ArrayList<UpdateAwareSystem>();
        defaultSystems.addAll(Arrays.asList(planetManager, context.get(SolCam.class), chunkManager, mountDetectDrawer, objectManager, mapDrawer, soundManager, beaconHandler, drawableDebugger));
        if (tutorialManager != null) {
            defaultSystems.add(tutorialManager);
        }
        updateSystems.put(0, defaultSystems);

        List<UpdateAwareSystem> defaultPausedSystems = new ArrayList<UpdateAwareSystem>();
        defaultPausedSystems.addAll(Arrays.asList(mapDrawer, context.get(SolCam.class), drawableDebugger));

        onPausedUpdateSystems = new TreeMap<Integer, List<UpdateAwareSystem>>();
        onPausedUpdateSystems.put(0, defaultPausedSystems);

        try {
            for (Class<?> updateSystemClass : moduleManager.getEnvironment().getSubtypesOf(UpdateAwareSystem.class)) {
                if (!updateSystemClass.isAnnotationPresent(RegisterUpdateSystem.class)) {
                    continue;
                }
                RegisterUpdateSystem registerAnnotation = updateSystemClass.getDeclaredAnnotation(RegisterUpdateSystem.class);
                UpdateAwareSystem system = (UpdateAwareSystem) updateSystemClass.newInstance();
                InjectionHelper.inject(system, context);
                if (!registerAnnotation.paused()) {
                    if (!updateSystems.containsKey(registerAnnotation.priority())) {
                        ArrayList<UpdateAwareSystem> systems = new ArrayList<UpdateAwareSystem>();
                        systems.add(system);
                        updateSystems.put(registerAnnotation.priority(), systems);
                    } else {
                        updateSystems.get(registerAnnotation.priority()).add(system);
                    }
                } else {
                    if (!onPausedUpdateSystems.containsKey(registerAnnotation.priority())) {
                        ArrayList<UpdateAwareSystem> systems = new ArrayList<UpdateAwareSystem>();
                        systems.add(system);
                        onPausedUpdateSystems.put(registerAnnotation.priority(), systems);
                    } else {
                        onPausedUpdateSystems.get(registerAnnotation.priority()).add(system);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startGame(String shipName, boolean isNewGame, WorldConfig worldConfig, EntitySystemManager entitySystemManager) {
        this.entitySystemManager = entitySystemManager;

        respawnState = new RespawnState();
        SolRandom.setSeed(worldConfig.getSeed());

        //World Generation will be initiated from here
        galaxyBuilder.buildWithRandomSolarSystemGenerators();

        //Add all the Planets in the game to the PlanetManager TODO: Add mazes, belts, etc. once the are implemented
        addObjectsToPlanetManager();

        createGame(shipName, isNewGame);

        if (!isNewGame) {
            createAndSpawnMercenariesFromSave();
        }
        SolMath.checkVectorsTaken(null);
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                if (!hero.isTranscendent()) {
                    saveShip();
//                    Console.getInstance().println("Game saved");
                }
            }
        }, 0, 30);
        gameScreens.consoleScreen.init(this);
        solApplication.getNuiManager().pushScreen(mainGameScreen);
        if (isTutorial) {
            tutorialManager.start();
        }
    }

    private void addObjectsToPlanetManager() {
        planetManager.getSystems().addAll(galaxyBuilder.getBuiltSolarSystems());
        for (SolarSystem system : planetManager.getSystems()) {
            for (Planet planet : system.getPlanets()) {
                planetManager.getPlanets().add(planet);
            }
            for (Maze maze : system.getMazes()) {
                planetManager.getMazes().add(maze);
            }
            for (SystemBelt belt : system.getBelts()) {
                planetManager.getBelts().add(belt);
            }
        }
    }

    public Context getContext() {
        return context;
    }

    private void createGame(String shipName, boolean shouldSpawnOnGalaxySpawnPosition) {
        /*
         * shipName will be null on respawn and continue, meaning the old ship will be loaded.
         * If shipName is not null then a new ship has to be created.
         */
        boolean isNewShip = shipName != null;
        ShipConfig shipConfig = readShipFromConfigOrLoadFromSaveIfNull(shipName, isNewShip);
        if (!respawnState.isPlayerRespawned()) {
            galaxyFiller.fill(this, hullConfigManager, itemManager, shipConfig.hull.getInternalName().split(":")[0]);
        }
        hero = new PlayerCreator().createPlayer(shipConfig,
                shouldSpawnOnGalaxySpawnPosition,
                respawnState,
                this,
                solApplication.getOptions().controlType == GameOptions.ControlType.MOUSE,
                isNewShip);
        hero.initialise(this);
    }

    private ShipConfig readShipFromConfigOrLoadFromSaveIfNull(String shipName, boolean isNewShip) {
        if (isNewShip) {
            return ShipConfig.load(hullConfigManager, shipName, itemManager);
        } else {
            return SaveManager.readShip(hullConfigManager, itemManager);
        }
    }

    private void createAndSpawnMercenariesFromSave() {
        List<MercItem> mercenaryItems = new MercenarySaveLoader().loadMercenariesFromSave(hullConfigManager, itemManager);
        for (MercItem mercenaryItem : mercenaryItems) {
            MercenaryUtils.createMerc(this, hero, mercenaryItem);
        }
    }

    public void onGameEnd(Context context) {
        // If the hero tries to exit while dead, respawn them first, then save
        if (hero.isDead()) {
            respawn();
        }
        if (!isTutorial) {
            //The ship should have been saved when it entered the star-port
            if (!hero.isTranscendent()) {
                saveShip();
            }
            SaveManager.saveWorld(getPlanetManager().getSystems().size());

            try {
                context.get(SerialisationManager.class).serialise();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // TODO: Remove this when context is reset after each game
            context.get(EntitySystemManager.class).getEntityManager().allEntities().forEach(EntityRef::delete);
        } else {
            context.remove(TutorialManager.class, tutorialManager);
        }
        FactionInfo.clearValues();
        objectManager.dispose();
        solApplication.getNuiManager().clearScreens();
    }

    private void saveShip() {
        if (hero.isTranscendent()) {
            throw new SolException("The hero cannot be saved when in a transcendent state.");
        }

        HullConfig hull;
        float money;
        List<SolItem> items;

        if (hero.isAlive()) {
            hull = hero.isTranscendent() ? hero.getTranscendentHero().getShip().getHullConfig() : hero.getHull().config;
            money = hero.getMoney();
            items = new ArrayList<>();
            for (List<SolItem> group : hero.getItemContainer()) {
                for (SolItem i : group) {
                    items.add(0, i);
                }
            }
        } else {
            hull = respawnState.getRespawnHull();
            money = respawnState.getRespawnMoney();
            items = respawnState.getRespawnItems();
        }

        SaveManager.writeShips(hull, money, items, hero, hullConfigManager);
    }

    public GameScreens getScreens() {
        return gameScreens;
    }

    public void update() {
        if (paused) {
            onPausedUpdateSystems.keySet().forEach(key -> onPausedUpdateSystems.get(key).forEach(system -> system.update(this, timeStep)));
        } else {
            updateTime();
            updateSystems.keySet().forEach(key ->
                    updateSystems.get(key).forEach(
                            system -> system.update(this, timeStep)));
        }
    }

    private void updateTime() {
        scaleTimeStep();
        time += timeStep;
    }

    private void scaleTimeStep() {
        timeFactor = DebugOptions.GAME_SPEED_MULTIPLIER;
        if (hero.isAlive() && hero.isNonTranscendent()) {
            ShipAbility ability = hero.getAbility();
            if (ability instanceof SloMo) {
                float factor = ((SloMo) ability).getFactor();
                timeFactor *= factor;
            }
        }
        timeStep = Const.REAL_TIME_STEP * timeFactor;
    }

    public void drawDebug(GameDrawer drawer, Context context) {
        if (DebugOptions.GRID_SZ > 0) {
            gridDrawer.draw(drawer, this, DebugOptions.GRID_SZ, drawer.debugWhiteTexture, context);
        }
        planetManager.drawDebug(drawer, context);
        objectManager.drawDebug(drawer);
        if (DebugOptions.ZOOM_OVERRIDE != 0) {
            context.get(SolCam.class).drawDebug(drawer);
        }
        drawDebugPoint(drawer, DebugOptions.DEBUG_POINT, DebugCol.POINT);
        drawDebugPoint(drawer, DebugOptions.DEBUG_POINT2, DebugCol.POINT2);
        drawDebugPoint(drawer, DebugOptions.DEBUG_POINT3, DebugCol.POINT3);
    }

    private void drawDebugPoint(GameDrawer drawer, Vector2 dp, Color col) {
        if (dp.x != 0 || dp.y != 0) {
            float sz = context.get(SolCam.class).getRealLineWidth() * 5;
            drawer.draw(drawer.debugWhiteTexture, sz, sz, sz / 2, sz / 2, dp.x, dp.y, 0, col);
        }
    }

    public float getTimeStep() {
        return timeStep;
    }

    //TODO refactor the usages of this to be independent from SolGame, so that this can be deleted
    // The only methods still using this are those that require changing an interface signature (UpdateAwareSystem, SolObject, and Drawable)
    public SolCam getCam() {
        return solCamReference;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    public PlanetManager getPlanetManager() {
        return planetManager;
    }

    public PartMan getPartMan() {
        return partMan;
    }

    public AsteroidBuilder getAsteroidBuilder() {
        return asteroidBuilder;
    }

    public LootBuilder getLootBuilder() {
        return lootBuilder;
    }

    public Hero getHero() {
        return hero;
    }

    public ShipBuilder getShipBuilder() {
        return shipBuilder;
    }

    public SolContactListener getContactListener() {
        return contactListener;
    }

    public ItemManager getItemMan() {
        return itemManager;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        DebugCollector.warn(this.paused ? "game paused" : "game resumed");
    }

    public void respawn() {
        respawnState.setPlayerRespawned(true);
        if (hero.isAlive()) {
            setRespawnState();
            objectManager.removeObjDelayed(hero.getShip());
        }
        createGame(null, true);
    }

    public FactionManager getFactionMan() {
        return factionManager;
    }

    public boolean isPlaceEmpty(Vector2 position, boolean considerPlanets) {
        if (considerPlanets) {
            Planet np = planetManager.getNearestPlanet(position);
            boolean inPlanet = np.getPosition().dst(position) < np.getFullHeight();

            if (inPlanet) {
                return false;
            }
        }

        SolarSystem ns = planetManager.getNearestSystem(position);
        if (ns.getPosition().dst(position) < SunSingleton.SUN_HOT_RAD) {
            return false;
        }

        List<SolObject> objs = objectManager.getObjects();
        for (SolObject o : objs) {
            if (!o.hasBody()) {
                continue;
            }

            if (position.dst(o.getPosition()) < objectManager.getRadius(o)) {
                return false;
            }
        }

        for (FarObjData fod : objectManager.getFarObjs()) {
            FarObject o = fod.fo;

            if (!o.hasBody()) {
                continue;
            }

            if (position.dst(o.getPosition()) < o.getRadius()) {
                return false;
            }
        }

        return true;
    }

    public MapDrawer getMapDrawer() {
        return mapDrawer;
    }

    public RubbleBuilder getRubbleBuilder() {
        return rubbleBuilder;
    }

    public FarBackgroundManagerOld getFarBackgroundgManagerOld() {
        return farBackgroundManagerOld;
    }

    public GalaxyFiller getGalaxyFiller() {
        return galaxyFiller;
    }

    public StarPort.Builder getStarPortBuilder() {
        return starPortBuilder;
    }

    public GridDrawer getGridDrawer() {
        return gridDrawer;
    }

    public OggSoundManager getSoundManager() {
        return soundManager;
    }

    public float getTime() {
        return time;
    }

    public void drawDebugUi(UiDrawer uiDrawer) {
        drawableDebugger.draw(uiDrawer);
    }

    public SpecialEffects getSpecialEffects() {
        return specialEffects;
    }

    public float getTimeFactor() {
        return timeFactor;
    }

    public BeaconHandler getBeaconHandler() {
        return beaconHandler;
    }

    public MountDetectDrawer getMountDetectDrawer() {
        return mountDetectDrawer;
    }

    public TutorialManager getTutMan() {
        return tutorialManager;
    }

    public EntitySystemManager getEntitySystemManager() {
        return entitySystemManager;
    }

    public void setRespawnState() {
        respawnState.setRespawnMoney(.75f * hero.getMoney());
        hero.setMoney(respawnState.getRespawnMoney()); // to update the display while the camera waits for respawn if the player died
        respawnState.setRespawnHull(hero.isNonTranscendent() ? hero.getHull().getHullConfig() : hero.getTranscendentHero().getShip().getHullConfig());
        respawnState.getRespawnItems().clear();
        respawnState.getRespawnWaypoints().clear();
        respawnState.setPlayerRespawned(true);
        for (List<SolItem> group : hero.getItemContainer()) {
            for (SolItem item : group) {
                boolean equipped = hero.isTranscendent() || hero.maybeUnequip(this, item, false);
                if (equipped || SolRandom.test(.75f)) {
                    respawnState.getRespawnItems().add(item);
                }
            }
        }
        for (Waypoint waypoint : hero.getWaypoints()) {
            respawnState.getRespawnWaypoints().add(waypoint);
        }
    }

    public boolean isTutorial() {
        return isTutorial;
    }

    public SolApplication getSolApplication() {
        return solApplication;
    }

    public HullConfigManager getHullConfigManager() {
        return hullConfigManager;
    }

    public GalaxyBuilder getGalaxyBuilder() {
        return galaxyBuilder;
    }

}
