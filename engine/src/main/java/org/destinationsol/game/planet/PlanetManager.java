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
package org.destinationsol.game.planet;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import org.destinationsol.common.SolColor;
import org.destinationsol.common.SolMath;
import org.destinationsol.game.DebugOptions;
import org.destinationsol.game.GameDrawer;
import org.destinationsol.game.SolCam;
import org.destinationsol.game.SolGame;
import org.destinationsol.game.SolObject;
import org.destinationsol.game.UpdateAwareSystem;
import org.destinationsol.game.context.Context;
import org.destinationsol.game.maze.Maze;
import org.destinationsol.game.ship.SolShip;
import org.destinationsol.game.ship.hulls.Hull;
import org.destinationsol.game.ship.hulls.HullConfig;
import org.destinationsol.world.generators.SolarSystemGenerator;

import java.util.ArrayList;
import java.util.List;

public class PlanetManager implements UpdateAwareSystem {
    private final ArrayList<SolarSystem> systems;
    private final ArrayList<Planet> planets;
    private final ArrayList<SystemBelt> belts;
    private final FlatPlaceFinder flatPlaceFinder;
    private final ArrayList<Maze> mazes;
    private final SunSingleton sunSingleton;
    private final PlanetCoreSingleton planetCoreSingleton;
    private Planet nearestPlanet;

    public PlanetManager() {
        systems = new ArrayList<>();
        mazes = new ArrayList<>();
        planets = new ArrayList<>();
        belts = new ArrayList<>();
        flatPlaceFinder = new FlatPlaceFinder();
        sunSingleton = new SunSingleton();
        planetCoreSingleton = new PlanetCoreSingleton();
    }

    @Override
    public void update(SolGame game, float timeStep) {
        Vector2 camPos = game.getCam().getPosition();
        for (Planet planet : planets) {
            planet.update(game, timeStep);
        }
        for (Maze maze : mazes) {
            maze.update(game);
        }

        nearestPlanet = getNearestPlanet(camPos);

        SolarSystem nearestSys = getNearestSystem(camPos);
        applyGrav(game, nearestSys);
    }

    public Planet getNearestPlanet(Vector2 position) {
        float minDst = Float.MAX_VALUE;
        Planet res = null;
        for (Planet planet : planets) {
            float dst = position.dst(planet.getPosition());
            if (dst < minDst) {
                minDst = dst;
                res = planet;
            }
        }
        return res;
    }

    private void applyGrav(SolGame game, SolarSystem nearestSys) {
        float npGh = nearestPlanet.getGroundHeight();
        float npFh = nearestPlanet.getFullHeight();
        float npMinH = nearestPlanet.getMinGroundHeight();
        Vector2 npPos = nearestPlanet.getPosition();
        Vector2 sysPos = nearestSys.getPosition();
        float npGravConst = nearestPlanet.getGravitationConstant();

        List<SolObject> objs = game.getObjectManager().getObjects();
        for (SolObject obj : objs) {
            if (!obj.receivesGravity()) {
                continue;
            }

            Vector2 objPos = obj.getPosition();
            float minDist;
            Vector2 srcPos;
            float gravConst;
            boolean onPlanet;
            float toNp = npPos.dst(objPos);
            float toSys = sysPos.dst(objPos);
            if (toNp < npFh) {
                if (recoverObj(obj, toNp, npMinH)) {
                    continue;
                }
                minDist = npGh;
                srcPos = npPos;
                gravConst = npGravConst;
                onPlanet = true;
            } else if (toSys < SolarSystemGenerator.SUN_RADIUS) {
                minDist = SunSingleton.SUN_HOT_RAD;
                srcPos = sysPos;
                gravConst = SunSingleton.GRAV_CONST;
                onPlanet = false;
            } else {
                continue;
            }

            Vector2 grav = SolMath.getVec(srcPos);
            grav.sub(objPos);
            float len = grav.len();
            grav.nor();
            if (len < minDist) {
                len = minDist;
            }
            float g = gravConst / len / len;
            grav.scl(g);
            obj.receiveForce(grav, game, true);
            SolMath.free(grav);
            if (!onPlanet) {
                sunSingleton.doDmg(game, obj, toSys);
            }
        }

    }

    private boolean recoverObj(SolObject obj, float toNp, float npMinH) {
        if (npMinH < toNp) {
            return false;
        }
        if (!(obj instanceof SolShip)) {
            return false;
        }
        SolShip ship = (SolShip) obj;
        Hull hull = ship.getHull();
        if (hull.config.getType() == HullConfig.Type.STATION) {
            return false;
        }
        float fh = nearestPlanet.getFullHeight();
        Vector2 npPos = nearestPlanet.getPosition();
        Vector2 toShip = SolMath.distVec(npPos, ship.getPosition());
        float len = toShip.len();
        if (len == 0) {
            toShip.set(0, fh);
        } else {
            toShip.scl(fh / len);
        }
        toShip.add(npPos);
        Body body = hull.getBody();
        body.setTransform(toShip, 0);
        body.setLinearVelocity(Vector2.Zero);
        SolMath.free(toShip);
        return true;
    }

    public Planet getNearestPlanet() {
        return nearestPlanet;
    }

    public void drawDebug(GameDrawer drawer, Context context) {
        if (DebugOptions.DRAW_PLANET_BORDERS) {
            SolCam cam = context.get(SolCam.class);
            float lineWidth = cam.getRealLineWidth();
            float viewHeight = cam.getViewHeight();
            for (Planet planet : planets) {
                Vector2 position = planet.getPosition();
                float angle = planet.getAngle();
                float fullHeight = planet.getFullHeight();
                Color color = planet == nearestPlanet ? SolColor.WHITE : SolColor.G;
                drawer.drawCircle(drawer.debugWhiteTexture, position, planet.getGroundHeight(), color, lineWidth, viewHeight);
                drawer.drawCircle(drawer.debugWhiteTexture, position, fullHeight, color, lineWidth, viewHeight);
                drawer.drawLine(drawer.debugWhiteTexture, position.x, position.y, angle, fullHeight, color, lineWidth);
            }

        }
    }

    public ArrayList<Planet> getPlanets() {
        return planets;
    }

    public ArrayList<SystemBelt> getBelts() {
        return belts;
    }

    public ArrayList<SolarSystem> getSystems() {
        return systems;
    }

    public Vector2 findFlatPlace(SolGame game, Planet planet, ConsumedAngles takenAngles,
                                 float objHalfWidth) {
        return flatPlaceFinder.find(game, planet, takenAngles, objHalfWidth);
    }

    public ArrayList<Maze> getMazes() {
        return mazes;
    }

    public SolarSystem getNearestSystem(Vector2 position) {
        float minDst = Float.MAX_VALUE;
        SolarSystem res = null;
        for (SolarSystem system : systems) {
            float dst = position.dst(system.getPosition());
            if (dst < minDst) {
                minDst = dst;
                res = system;
            }
        }
        return res;
    }

    public Maze getNearestMaze(Vector2 position) {
        float minDst = Float.MAX_VALUE;
        Maze res = null;
        for (Maze maze : mazes) {
            float dst = position.dst(maze.getPos());
            if (dst < minDst) {
                minDst = dst;
                res = maze;
            }
        }
        return res;
    }

    public void drawSunHack(SolGame game, GameDrawer drawer, Context context) {
        sunSingleton.draw(game, drawer, context);
    }

    public void drawPlanetCoreHack(SolGame game, GameDrawer drawer, Context context) {
        planetCoreSingleton.draw(game, drawer, context);
    }

}
